package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.api.TokenManager
import com.example.data.model.ServiceLog
import com.example.data.model.Vehicle
import com.example.data.model.UserProfile
import com.example.data.repository.GarageRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

enum class GarageTab {
    DASHBOARD, HISTORY, ADD, PROFILE
}

data class MaintenanceConfig(
    val category: String,
    val subtitle: String,
    val intervalKm: Double,
    val intervalDays: Int
)


class GarageViewModel(private val repository: GarageRepository, private val context: android.content.Context) : ViewModel() {

    private val prefs = context.getSharedPreferences("garage_pulse_prefs", android.content.Context.MODE_PRIVATE)

    // --- Auth states ---
    private val _isLoggedIn = MutableStateFlow(prefs.getBoolean("is_logged_in", false))
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _isRegistered = MutableStateFlow(prefs.getBoolean("is_registered", false))
    val isRegistered: StateFlow<Boolean> = _isRegistered.asStateFlow()

    private val _showSplash = MutableStateFlow(true)
    val showSplash: StateFlow<Boolean> = _showSplash.asStateFlow()

    // --- Auth loading/error states for UI feedback ---
    private val _authLoading = MutableStateFlow(false)
    val authLoading: StateFlow<Boolean> = _authLoading.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    fun clearAuthError() {
        _authError.value = null
    }

    // --- Tab state ---
    private val _currentTab = MutableStateFlow(GarageTab.DASHBOARD)
    val currentTab: StateFlow<GarageTab> = _currentTab.asStateFlow()

    // --- Maintenance Config State ---
    private val _categoryConfig = MutableStateFlow<Map<String, Triple<String, Double, Int>>>(emptyMap())
    val categoryConfig: StateFlow<Map<String, Triple<String, Double, Int>>> = _categoryConfig.asStateFlow()


    fun dismissSplash() {
        _showSplash.value = false
        _currentTab.value = GarageTab.DASHBOARD
    }

    fun registerUser(name: String, email: String, password: String, vehicleBrand: String, vehicleModel: String, initialOdometer: Double, licensePlate: String, vehicleType: String = "Car") {
        _authLoading.value = true
        _authError.value = null

        viewModelScope.launch {
            // 1. Try to register via the REST API
            val apiResult = repository.registerUserApi(
                name = name,
                email = email,
                password = password,
                vehicleBrand = vehicleBrand,
                vehicleModel = vehicleModel,
                initialOdometer = initialOdometer,
                licensePlate = licensePlate
            )

            if (apiResult != null) {
                // API registration succeeded — save local state
                prefs.edit()
                    .putString("registered_name", name)
                    .putString("registered_email", email)
                    .putBoolean("is_registered", true)
                    .putBoolean("is_logged_in", true)
                    .apply()

                _isRegistered.value = true
                _isLoggedIn.value = true
                _currentTab.value = GarageTab.DASHBOARD

                // Save user profile locally
                val userEntity = apiResult.user.toEntity()
                repository.saveUserProfile(userEntity)

                // Clear existing local data and insert the vehicle from API
                repository.clearAllVehicles()
                repository.clearAllServiceLogs()

                val vehicleEntity = apiResult.vehicle.toEntity()
                repository.insertVehicle(vehicleEntity)

            } else {
                // API registration failed — fall back to offline-only mode
                _authError.value = "No se pudo conectar al servidor. Registrando localmente..."

                prefs.edit()
                    .putString("registered_name", name)
                    .putString("registered_email", email)
                    .putString("registered_password", password)
                    .putBoolean("is_registered", true)
                    .putBoolean("is_logged_in", true)
                    .apply()

                _isRegistered.value = true
                _isLoggedIn.value = true
                _currentTab.value = GarageTab.DASHBOARD

                // Update profile with registered name and email
                val currentProfile = repository.userProfile.firstOrNull() ?: UserProfile()
                repository.saveUserProfile(
                    currentProfile.copy(
                        name = name,
                        email = email
                    )
                )

                // Clear any other vehicles and service logs so the registered vehicle is the only one, and history is clean
                repository.clearAllVehicles()
                repository.clearAllServiceLogs()

                // Insert custom vehicle registered in onboarding/splash
                val customVehicle = com.example.data.model.Vehicle(
                    name = "$vehicleBrand $vehicleModel".trim(),
                    brand = vehicleBrand,
                    model = vehicleModel,
                    year = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR),
                    licensePlate = if (licensePlate.isNotBlank()) licensePlate.uppercase().trim() else "NU-EVO",
                    status = "Optimal",
                    odometer = initialOdometer,
                    isActive = true,
                    type = vehicleType
                )

                repository.insertVehicle(customVehicle)
            }

            _authLoading.value = false
        }
    }

    fun getRegisteredEmail(): String {
        return prefs.getString("registered_email", "") ?: ""
    }

    fun getRegisteredPassword(): String {
        return prefs.getString("registered_password", "") ?: ""
    }

    fun loginUser(email: String, password: String): Boolean {
        _authLoading.value = true
        _authError.value = null

        viewModelScope.launch {
            // 1. Try to login via the REST API
            val apiResult = repository.loginUserApi(email, password)

            if (apiResult != null) {
                // API login succeeded
                prefs.edit().putBoolean("is_logged_in", true).apply()
                _isLoggedIn.value = true
                _currentTab.value = GarageTab.DASHBOARD

                // Sync vehicles and services from server
                repository.syncLocalWithRemote()

                _authLoading.value = false
                return@launch
            }

            // 2. API failed — fall back to local credentials check
            val regEmail = prefs.getString("registered_email", "") ?: ""
            val regPassword = prefs.getString("registered_password", "") ?: ""
            val matchesReal = regEmail.trim().equals(email.trim(), ignoreCase = true) && regPassword == password
            val matchesMock = email.trim().equals("prueba@garagepulse.com", ignoreCase = true) && password == "1234"

            if (matchesReal || matchesMock) {
                if (matchesMock && regEmail.trim().isEmpty()) {
                    registerUser(
                        name = "Usuario Prueba",
                        email = "prueba@garagepulse.com",
                        password = "1234",
                        vehicleBrand = "Toyota",
                        vehicleModel = "Hilux",
                        initialOdometer = 24500.0,
                        licensePlate = "M0CK-123",
                        vehicleType = "Car"
                    )
                }
                prefs.edit().putBoolean("is_logged_in", true).apply()
                _isLoggedIn.value = true
                _currentTab.value = GarageTab.DASHBOARD
            } else {
                _authError.value = "Credenciales inválidas"
            }

            _authLoading.value = false
        }

        // Return true optimistically — the actual result is handled via StateFlows
        return true
    }

    fun logoutUser() {
        prefs.edit().putBoolean("is_logged_in", false).apply()
        _isLoggedIn.value = false
        TokenManager.clearToken()
    }

    fun resetToAuth() {
        prefs.edit()
            .putBoolean("is_logged_in", false)
            .putBoolean("is_registered", false)
            .apply()
        _isLoggedIn.value = false
        _isRegistered.value = false
        _showSplash.value = true
        TokenManager.clearToken()
    }

    // --- Tab state ---
    // Moved to top

    fun selectTab(tab: GarageTab) {
        _currentTab.value = tab
    }

    fun navigateToRegisterService(category: String) {
        setFormCategory(category)
        selectTab(GarageTab.ADD)
    }

    // --- Database Data ---
    val allVehicles: StateFlow<List<Vehicle>> = repository.allVehicles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeVehicle: StateFlow<Vehicle?> = repository.activeVehicleFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allServiceLogs: StateFlow<List<ServiceLog>> = repository.allServiceLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userProfile: StateFlow<UserProfile> = repository.userProfile
        .map { it ?: UserProfile() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserProfile())

    // --- Add Service Form State ---
    val formCategory = MutableStateFlow("Cambio de Aceite")
    val formTitle = MutableStateFlow("")
    val formDate = MutableStateFlow(System.currentTimeMillis())
    val formCost = MutableStateFlow("")
    val formMileage = MutableStateFlow("")
    val formNotes = MutableStateFlow("")
    val formDetails = MutableStateFlow("")

    // --- Theme Toggle State ---
    private val _isDarkTheme = MutableStateFlow(true)
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    fun toggleTheme() {
        _isDarkTheme.value = !_isDarkTheme.value
    }

    init {
        // Initialize TokenManager
        TokenManager.init(context)

        // Pre-populate DB if it doesn't have any vehicles yet
        viewModelScope.launch {
            repository.prepopulateIfEmpty()

            // If user is logged in and has a token, attempt background sync
            if (_isLoggedIn.value && TokenManager.isAuthenticated()) {
                repository.syncLocalWithRemote()
            }
        }
        loadCategoryConfig()
    }

    // --- CONFIG ACTIONS ---
    private fun loadCategoryConfig() {
        val defaults = mapOf(
            "Cambio de Aceite" to Triple("SINTÉTICO 5W-30", 10000.0, 180),
            "Filtros" to Triple("FILTROS DE AIRE Y AC", 15000.0, 365),
            "Frenos" to Triple("PASTILLAS Y DISCOS", 30000.0, 730),
            "Neumáticos" to Triple("ROTACIÓN Y BALANCEO", 40000.0, 730),
            "Batería" to Triple("TEST DE CORRIENTE", 60000.0, 1095)
        )
        val currentConfig = mutableMapOf<String, Triple<String, Double, Int>>()
        for ((key, defaultVal) in defaults) {
            val subtitle = prefs.getString("config_sub_$key", defaultVal.first) ?: defaultVal.first
            val km = prefs.getFloat("config_km_$key", defaultVal.second.toFloat()).toDouble()
            val days = prefs.getInt("config_days_$key", defaultVal.third)
            currentConfig[key] = Triple(subtitle, km, days)
        }
        _categoryConfig.value = currentConfig
    }

    fun saveCategoryConfigItem(category: String, subtitle: String, intervalKm: Double, intervalDays: Int) {
        prefs.edit()
            .putString("config_sub_$category", subtitle)
            .putFloat("config_km_$category", intervalKm.toFloat())
            .putInt("config_days_$category", intervalDays)
            .apply()
        loadCategoryConfig() // Reload to update StateFlow
    }

    // --- FORM ACTIONS ---
    fun setFormCategory(category: String) {
        formCategory.value = category
        val defaultConfig = _categoryConfig.value[category]
        formTitle.value = defaultConfig?.first ?: category
    }

    fun setFormTitle(title: String) {
        formTitle.value = title
    }

    fun setFormDate(date: Long) {
        formDate.value = date
    }

    fun setFormCost(cost: String) {
        formCost.value = cost
    }

    fun setFormMileage(mileage: String) {
        formMileage.value = mileage
    }

    fun setFormNotes(notes: String) {
        formNotes.value = notes
    }

    fun setFormDetails(details: String) {
        formDetails.value = details
    }

    fun resetForm(currentActiveVehicle: Vehicle?) {
        formCategory.value = "Cambio de Aceite"
        formTitle.value = _categoryConfig.value["Cambio de Aceite"]?.first ?: "SINTÉTICO 5W-30"
        formDate.value = System.currentTimeMillis()
        formCost.value = ""
        // Pre-populate form mileage converting to MI if needed
        val profile = userProfile.value
        val odoKm = currentActiveVehicle?.odometer
        formMileage.value = if (odoKm != null) {
            val converted = if (profile.useKm) odoKm else odoKm * 0.621371
            converted.toInt().toString()
        } else ""
        formNotes.value = ""
        formDetails.value = ""
    }

    fun saveServiceLog(onSuccess: () -> Unit) {
        val vehicle = activeVehicle.value ?: return
        val profile = userProfile.value
        val costVal = formCost.value.toDoubleOrNull() ?: 0.0
        val inputMileage = formMileage.value.toDoubleOrNull()

        // Si el usuario escribió en millas, convertimos a KM para guardar
        val finalMileageKm = if (inputMileage != null) {
            if (profile.useKm) inputMileage else inputMileage / 0.621371
        } else {
            vehicle.odometer
        }

        val log = ServiceLog(
            vehicleId = vehicle.id,
            category = formCategory.value,
            title = formTitle.value.ifBlank { formCategory.value },
            description = formNotes.value.ifBlank { "Mantenimiento general" },
            cost = costVal,
            mileage = finalMileageKm,
            date = formDate.value,
            type = if (formCategory.value in listOf("Frenos", "Motor")) "REPARACIONES" else "PREVENTIVO",
            details = formDetails.value
        )

        viewModelScope.launch {
            // Save log
            repository.insertServiceLog(log)
            // Update vehicle odometer to the registered mileage if it is newer
            if (finalMileageKm > vehicle.odometer) {
                repository.updateVehicle(vehicle.copy(odometer = finalMileageKm))
            }
            // Reset form
            resetForm(vehicle)
            onSuccess()
        }
    }

    fun deleteServiceLog(log: ServiceLog) {
        viewModelScope.launch {
            repository.deleteServiceLog(log)
        }
    }

    // --- VEHICLE ACTIONS ---
    fun addVehicle(name: String, brand: String, model: String, year: Int, licensePlate: String, type: String = "Car", photoUri: String? = null, odometer: Double = 0.0) {
        viewModelScope.launch {
            val v = Vehicle(
                name = name,
                brand = brand,
                model = model,
                year = year,
                licensePlate = licensePlate,
                odometer = odometer,
                status = "Optimal",
                type = type,
                photoUri = if (photoUri?.isNotBlank() == true) photoUri else null
            )
            val newId = repository.insertVehicle(v)
            repository.selectActiveVehicle(newId.toInt())
        }
    }

    fun selectVehicle(vehicleId: Int) {
        viewModelScope.launch {
            repository.selectActiveVehicle(vehicleId)
        }
    }

    // --- USER PROFILE ACTIONS ---
    fun toggleUnit(useKm: Boolean) {
        viewModelScope.launch {
            val profile = userProfile.value
            repository.saveUserProfile(profile.copy(useKm = useKm))
        }
    }

    fun togglePremiumTelemetry(enabled: Boolean) {
        viewModelScope.launch {
            val profile = userProfile.value
            repository.saveUserProfile(profile.copy(isPremium = enabled))
            
            val intent = android.content.Intent(context, com.example.service.TelemetryService::class.java)
            if (enabled) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } else {
                context.stopService(intent)
            }
        }
    }

    // --- AI Server Integration ---
    private val aiApi = com.example.data.api.RetrofitClient.aiServerApi

    fun processAudioFile(fileUri: String, onSuccess: (String, Int) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            // Implementation pending: Upload multipart file to process-audio
            // onSuccess("Mantenimiento Sugerido", 50000)
        }
    }

    fun scanDashboardOdometer(imageUri: String, onSuccess: (Int) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            // Implementation pending: Upload multipart image to scan-odometer
            // onSuccess(45200)
        }
    }

    fun updateProfileName(newName: String) {
        viewModelScope.launch {
            val profile = userProfile.value
            repository.saveUserProfile(profile.copy(name = newName))
        }
    }

    fun updateAvatarUrl(newUrl: String) {
        viewModelScope.launch {
            val profile = userProfile.value
            repository.saveUserProfile(profile.copy(avatarUrl = newUrl))
        }
    }
}

class GarageViewModelFactory(private val repository: GarageRepository, private val context: android.content.Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GarageViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return GarageViewModel(repository, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
