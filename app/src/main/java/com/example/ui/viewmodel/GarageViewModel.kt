package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
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

class GarageViewModel(private val repository: GarageRepository, private val context: android.content.Context) : ViewModel() {

    private val prefs = context.getSharedPreferences("garage_pulse_prefs", android.content.Context.MODE_PRIVATE)

    // --- Auth states ---
    private val _isLoggedIn = MutableStateFlow(prefs.getBoolean("is_logged_in", false))
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _isRegistered = MutableStateFlow(prefs.getBoolean("is_registered", false))
    val isRegistered: StateFlow<Boolean> = _isRegistered.asStateFlow()

    private val _showSplash = MutableStateFlow(true)
    val showSplash: StateFlow<Boolean> = _showSplash.asStateFlow()

    // --- Tab state ---
    private val _currentTab = MutableStateFlow(GarageTab.DASHBOARD)
    val currentTab: StateFlow<GarageTab> = _currentTab.asStateFlow()

    fun dismissSplash() {
        _showSplash.value = false
        _currentTab.value = GarageTab.DASHBOARD
    }

    fun registerUser(name: String, email: String, password: String, vehicleBrand: String, vehicleModel: String, initialOdometer: Double, licensePlate: String, vehicleType: String = "Car") {
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

        viewModelScope.launch {
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
    }

    fun getRegisteredEmail(): String {
        return prefs.getString("registered_email", "") ?: ""
    }

    fun getRegisteredPassword(): String {
        return prefs.getString("registered_password", "") ?: ""
    }

    fun loginUser(email: String, password: String): Boolean {
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
            return true
        }
        return false
    }

    fun logoutUser() {
        prefs.edit().putBoolean("is_logged_in", false).apply()
        _isLoggedIn.value = false
    }

    fun resetToAuth() {
        prefs.edit()
            .putBoolean("is_logged_in", false)
            .putBoolean("is_registered", false)
            .apply()
        _isLoggedIn.value = false
        _isRegistered.value = false
        _showSplash.value = true
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
    val formDate = MutableStateFlow(System.currentTimeMillis())
    val formCost = MutableStateFlow("")
    val formMileage = MutableStateFlow("")
    val formNotes = MutableStateFlow("")

    // --- Theme Toggle State ---
    private val _isDarkTheme = MutableStateFlow(true)
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    fun toggleTheme() {
        _isDarkTheme.value = !_isDarkTheme.value
    }

    init {
        // Pre-populate DB if it doesn't have any vehicles yet
        viewModelScope.launch {
            repository.prepopulateIfEmpty()
        }
    }

    // --- FORM ACTIONS ---
    fun setFormCategory(category: String) {
        formCategory.value = category
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

    fun resetForm(currentActiveVehicle: Vehicle?) {
        formCategory.value = "Cambio de Aceite"
        formDate.value = System.currentTimeMillis()
        formCost.value = ""
        // Pre-populate form mileage with current active vehicle odometer if available
        formMileage.value = currentActiveVehicle?.odometer?.toInt()?.toString() ?: ""
        formNotes.value = ""
    }

    fun saveServiceLog(onSuccess: () -> Unit) {
        val vehicle = activeVehicle.value ?: return
        val costVal = formCost.value.toDoubleOrNull() ?: 0.0
        val mileageVal = formMileage.value.toDoubleOrNull() ?: vehicle.odometer

        val log = ServiceLog(
            vehicleId = vehicle.id,
            category = formCategory.value,
            title = formCategory.value,
            description = formNotes.value.ifBlank { "Mantenimiento general" },
            cost = costVal,
            mileage = mileageVal,
            date = formDate.value,
            type = if (formCategory.value in listOf("Frenos", "Motor")) "REPARACIONES" else "PREVENTIVO"
        )

        viewModelScope.launch {
            // Save log
            repository.insertServiceLog(log)
            // Update vehicle odometer to the registered mileage if it is newer
            if (mileageVal > vehicle.odometer) {
                repository.updateVehicle(vehicle.copy(odometer = mileageVal))
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
