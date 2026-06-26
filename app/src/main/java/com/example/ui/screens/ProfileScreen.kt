package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.compose.AsyncImagePainter
import coil.compose.AsyncImage
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.isGranted
import androidx.compose.ui.layout.ContentScale
import com.example.data.model.Vehicle
import com.example.ui.viewmodel.GarageTab
import com.example.ui.viewmodel.GarageViewModel
import com.example.ui.components.PremiumBadgeIcon

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun ProfileScreen(
    viewModel: GarageViewModel,
    modifier: Modifier = Modifier
) {
    val userProfile by viewModel.userProfile.collectAsState()
    val allVehicles by viewModel.allVehicles.collectAsState()
    val allLogs by viewModel.allServiceLogs.collectAsState()
    val isDarkTheme by viewModel.isDarkTheme.collectAsState()
    val categoryConfig by viewModel.categoryConfig.collectAsState()

    // Dialog state controllers
    var showVehiclesDialog by remember { mutableStateOf(false) }
    var showAddVehicleDialog by remember { mutableStateOf(false) }
    var showMaintenanceConfigDialog by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<String?>(null) }
    var tempSubtitle by remember { mutableStateOf("") }
    var tempKm by remember { mutableStateOf("") }
    var tempDays by remember { mutableStateOf("") }
    var notificationStatus by remember { mutableStateOf("Activadas") }

    // Dialog form state matching splash/onboarding
    var newVehicleType by remember { mutableStateOf("Car") } // "Car" or "Motorcycle"
    var selectedBrand by remember { mutableStateOf("") }
    var selectedModel by remember { mutableStateOf("") }
    var customVehicleBrand by remember { mutableStateOf("") }
    var customVehicleModel by remember { mutableStateOf("") }
    var newVehiclePlate by remember { mutableStateOf("") }
    var initialOdometerText by remember { mutableStateOf("15000") }
    var vehicleYearText by remember { mutableStateOf("2026") }
    var newVehiclePhotoUri by remember { mutableStateOf("") }

    var brandError by remember { mutableStateOf(false) }
    var modelError by remember { mutableStateOf(false) }
    var plateError by remember { mutableStateOf(false) }
    var odometerError by remember { mutableStateOf(false) }

    val context = androidx.compose.ui.platform.LocalContext.current
    var showPhotoSourceDialog by remember { mutableStateOf(false) }

    val galleryLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        uri?.let {
            viewModel.updateAvatarUrl(it.toString())
        }
    }

    val cameraLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.TakePicturePreview()
    ) { bitmap: android.graphics.Bitmap? ->
        bitmap?.let {
            val file = java.io.File(context.cacheDir, "avatar_${System.currentTimeMillis()}.jpg")
            val out = java.io.FileOutputStream(file)
            it.compress(android.graphics.Bitmap.CompressFormat.JPEG, 100, out)
            out.flush()
            out.close()
            viewModel.updateAvatarUrl(android.net.Uri.fromFile(file).toString())
        }
    }

    // Vehicles list dialog
    if (showVehiclesDialog) {
        AlertDialog(
            onDismissRequest = { showVehiclesDialog = false },
            title = { Text("Mis Vehículos") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    allVehicles.forEach { vehicle ->
                        val isActive = vehicle.isActive
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                                    else Color.Transparent
                                )
                                .clickable {
                                    viewModel.selectVehicle(vehicle.id)
                                    showVehiclesDialog = false
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = if (vehicle.type == "Motorcycle") Icons.Default.TwoWheeler else Icons.Default.DirectionsCar,
                                    contentDescription = null,
                                    tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(
                                            color = (if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary).copy(alpha = 0.1f),
                                            shape = CircleShape
                                        )
                                        .padding(8.dp)
                                )

                                Column {
                                    Text(
                                        text = vehicle.name,
                                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                        color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Placa: ${vehicle.licensePlate} • ${if (vehicle.type == "Motorcycle") "MOTO" else "AUTO"}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                            if (isActive) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Activo",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showVehiclesDialog = false }) {
                    Text("Cerrar")
                }
            }
        )
    }

    // Photo Source Dialog
    if (showPhotoSourceDialog) {
        AlertDialog(
            onDismissRequest = { showPhotoSourceDialog = false },
            title = {
                Text(
                    text = "Foto de Perfil",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Tomar Foto
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showPhotoSourceDialog = false
                                cameraLauncher.launch(null)
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Cámara",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            text = "Tomar Foto",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }

                    // Seleccionar de Galería
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showPhotoSourceDialog = false
                                galleryLauncher.launch("image/*")
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = "Galería",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            text = "Seleccionar de la Galería",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPhotoSourceDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Add Vehicle Dialog modal (Splash-like high-fidelity style!)
    if (showAddVehicleDialog) {
        val carModels = remember {
            mapOf(
                "Toyota" to listOf("Hilux", "Corolla", "Yaris"),
                "Chery" to listOf("Arauca", "Tiggo 2", "Arauca 1.3"),
                "BMW" to listOf("Serie 3", "X5", "M3"),
                "Mercedes" to listOf("Clase C", "GLA 200", "A200"),
                "Porsche" to listOf("911 Carrera", "Macan S", "Cayenne"),
                "Renault" to listOf("Sandero", "Duster", "Kwid")
            )
        }

        val motoModels = remember {
            mapOf(
                "Vespa" to listOf("GTS 300", "Primavera 150", "Sprint 150"),
                "Yamaha" to listOf("R6", "MT-07", "Fazer 250"),
                "Honda" to listOf("CB500", "Africa Twin", "PCX 160"),
                "Suzuki" to listOf("V-Strom 650", "Gixxer 150", "GN 125"),
                "Ducati" to listOf("Monster", "Multistrada", "Panigale V4")
            )
        }

        AlertDialog(
            onDismissRequest = { 
                showAddVehicleDialog = false 
                // Reset errors when dismissing
                brandError = false
                modelError = false
                plateError = false
                odometerError = false
            },
            title = {
                Text(
                    text = "Añadir Nuevo Vehículo",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 460.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. SELECT TYPE: Automóvil vs Motocicleta
                    Text(
                        text = "Tipo de Vehículo",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Automóvil Card
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { 
                                    newVehicleType = "Car"
                                    selectedBrand = ""
                                    selectedModel = ""
                                    customVehicleBrand = ""
                                    customVehicleModel = ""
                                    brandError = false
                                    modelError = false
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = if (newVehicleType == "Car") MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            ),
                            border = BorderStroke(
                                width = 1.5.dp,
                                color = if (newVehicleType == "Car") MaterialTheme.colorScheme.primary else Color.Transparent
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DirectionsCar,
                                    contentDescription = null,
                                    tint = if (newVehicleType == "Car") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(28.dp)
                                )
                                Text(
                                    text = "Automóvil",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (newVehicleType == "Car") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Motocicleta Card
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { 
                                    newVehicleType = "Motorcycle"
                                    selectedBrand = ""
                                    selectedModel = ""
                                    customVehicleBrand = ""
                                    customVehicleModel = ""
                                    brandError = false
                                    modelError = false
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = if (newVehicleType == "Motorcycle") MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            ),
                            border = BorderStroke(
                                width = 1.5.dp,
                                color = if (newVehicleType == "Motorcycle") MaterialTheme.colorScheme.primary else Color.Transparent
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.TwoWheeler,
                                    contentDescription = null,
                                    tint = if (newVehicleType == "Motorcycle") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(28.dp)
                                )
                                Text(
                                    text = "Motocicleta",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (newVehicleType == "Motorcycle") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    // 2. SELECT BRAND: Scrollable row of Brand Logos
                    Text(
                        text = "Selecciona la Marca",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    val brands = if (newVehicleType == "Car") {
                        listOf("Toyota", "Chery", "BMW", "Mercedes", "Porsche", "Renault", "Otro")
                    } else {
                        listOf("Vespa", "Yamaha", "Honda", "Suzuki", "Ducati", "Otro")
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        brands.forEach { brand ->
                            val isSelected = selectedBrand == brand
                            Card(
                                modifier = Modifier
                                    .width(86.dp)
                                    .clickable {
                                        selectedBrand = brand
                                        if (brand != "Otro") {
                                            customVehicleBrand = brand
                                            brandError = false
                                        } else {
                                            customVehicleBrand = ""
                                        }
                                        selectedModel = ""
                                        customVehicleModel = ""
                                    },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                ),
                                border = BorderStroke(
                                    width = 1.5.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 10.dp, horizontal = 4.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    if (brand == "Otro") {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    } else {
                                        BrandLogo(brandName = brand, modifier = Modifier.size(40.dp))
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = brand,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }

                    if (selectedBrand == "Otro") {
                        OutlinedTextField(
                            value = customVehicleBrand,
                            onValueChange = {
                                customVehicleBrand = it
                                brandError = false
                            },
                            label = { Text("Nombre de la Marca") },
                            leadingIcon = { Icon(Icons.Default.Build, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                            isError = brandError,
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("register_vehicle_brand"),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    if (brandError) {
                        Text(
                            text = "Por favor, selecciona o ingresa la marca.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // 3. SELECT MODEL
                    if (selectedBrand.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Selecciona el Modelo",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )

                        val models = if (selectedBrand == "Otro") {
                            listOf("Otro")
                        } else {
                            (if (newVehicleType == "Car") carModels[selectedBrand] else motoModels[selectedBrand])?.plus("Otro") ?: listOf("Otro")
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            models.forEach { model ->
                                val isSelected = selectedModel == model
                                val modelImage = when {
                                    selectedBrand == "Toyota" && model == "Hilux" -> com.example.R.drawable.img_toyota_hilux_2_8_1781704989578
                                    selectedBrand == "Vespa" && model == "GTS 300" -> com.example.R.drawable.img_vespa_gts_300_1781704971852
                                    selectedBrand == "Chery" && model == "Arauca" -> "https://lh3.googleusercontent.com/aida-public/AB6AXuDqhohr5P2dCxFvCtHhd8LekMaLiFz-QggHJfhsrrcVqfFKuZuDDlSbDBlaT3H1rSTzcsCZw--vWptlNLmvJi6IVrQgEsk1tb6vB8aXeCphAqFozzE6J5S3Ez7B-PBMalJpYSdrPKfUdQX8-rmvmtsu9C1yAo3ZvoSH_EcLZBQYUp8_IH0qADqtNCdOUyIeXA9XwU-t5M_YrkMKrGbmae3u7hdQBSjhbvDWI0bgcvy8ZlXWGyM7pkkB4dZ1W5Y2MDYKh58xeg6N_fE"
                                    newVehicleType == "Motorcycle" -> com.example.R.drawable.img_default_moto_1781706682868
                                    else -> com.example.R.drawable.img_default_car_1781706668505
                                }

                                Card(
                                    modifier = Modifier
                                        .width(116.dp)
                                        .clickable {
                                            selectedModel = model
                                            if (model != "Otro") {
                                                customVehicleModel = model
                                                modelError = false
                                                
                                                // Pre-assign beautiful unsplash photos on matching model selection
                                                newVehiclePhotoUri = when {
                                                    selectedBrand == "Toyota" && model == "Hilux" -> "https://images.unsplash.com/photo-1533473359331-0135ef1b58bf?auto=format&fit=crop&w=600&q=80"
                                                    selectedBrand == "Vespa" && model == "GTS 300" -> "https://images.unsplash.com/photo-1568772585407-9361f9bf3a87?auto=format&fit=crop&w=600&q=80"
                                                    selectedBrand == "Chery" && model == "Arauca" -> "https://images.unsplash.com/photo-1549399542-7e3f8b79c341?auto=format&fit=crop&w=600&q=80"
                                                    newVehicleType == "Motorcycle" -> "https://images.unsplash.com/photo-1485965120184-e220f721d03e?auto=format&fit=crop&w=600&q=80"
                                                    else -> "https://images.unsplash.com/photo-1503376780353-7e6692767b70?auto=format&fit=crop&w=600&q=80"
                                                }
                                            } else {
                                                customVehicleModel = ""
                                            }
                                        },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                    ),
                                    border = BorderStroke(
                                        width = 1.5.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        if (model == "Otro") {
                                            Box(
                                                modifier = Modifier
                                                    .size(96.dp, 62.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Edit,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            }
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .size(96.dp, 62.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                SubcomposeAsyncImage(
                                                    model = modelImage,
                                                    contentDescription = model,
                                                    contentScale = ContentScale.Fit,
                                                    modifier = Modifier.fillMaxSize()
                                                ) {
                                                    val state = painter.state
                                                    if (state is AsyncImagePainter.State.Loading) {
                                                        CircularProgressIndicator(
                                                            color = MaterialTheme.colorScheme.primary,
                                                            strokeWidth = 2.dp,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    } else if (state is AsyncImagePainter.State.Error) {
                                                        Icon(
                                                            imageVector = if (newVehicleType == "Motorcycle") Icons.Default.TwoWheeler else Icons.Default.DirectionsCar,
                                                            contentDescription = null,
                                                            tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                                            modifier = Modifier.size(24.dp)
                                                        )
                                                    } else {
                                                        SubcomposeAsyncImageContent()
                                                    }
                                                }
                                            }
                                        }
                                        Text(
                                            text = model,
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(horizontal = 2.dp)
                                        )
                                    }
                                }
                            }
                        }

                        if (selectedModel == "Otro") {
                            OutlinedTextField(
                                value = customVehicleModel,
                                onValueChange = {
                                    customVehicleModel = it
                                    modelError = false
                                },
                                label = { Text("Nombre del Modelo") },
                                leadingIcon = { Icon(Icons.Default.Garage, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                isError = modelError,
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("register_vehicle_model"),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

                        if (modelError) {
                            Text(
                                text = "Por favor, selecciona o ingresa el modelo.",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    // 4. LICENSE PLATE & INITIAL MILEAGE / ODOMETER & YEAR & PHOTO URL
                    Text(
                        text = "Especificaciones Técnicas",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )

                    OutlinedTextField(
                        value = newVehiclePlate,
                        onValueChange = {
                            newVehiclePlate = it
                            plateError = false
                        },
                        label = { Text("Placa del Vehículo (ej. ABC-123)") },
                        leadingIcon = { Icon(Icons.Default.Pin, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        isError = plateError,
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("new_vehicle_plate_input"),
                        shape = RoundedCornerShape(12.dp)
                    )
                    if (plateError) {
                        Text(
                            text = "Por favor, ingresa la placa o patente del vehículo.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    OutlinedTextField(
                        value = initialOdometerText,
                        onValueChange = {
                            initialOdometerText = it
                            odometerError = false
                        },
                        label = { Text("Kilometraje Inicial (km)") },
                        leadingIcon = { Icon(Icons.Default.Speed, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        isError = odometerError,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("new_vehicle_odometer_input"),
                        shape = RoundedCornerShape(12.dp)
                    )
                    if (odometerError) {
                        Text(
                            text = "Por favor, ingresa un kilometraje inicial válido.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    OutlinedTextField(
                        value = vehicleYearText,
                        onValueChange = {
                            vehicleYearText = it
                        },
                        label = { Text("Año del Vehículo (ej. 2024)") },
                        leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = newVehiclePhotoUri,
                        onValueChange = { newVehiclePhotoUri = it },
                        label = { Text("Foto (URL de Imagen - Opcional)") },
                        leadingIcon = { Icon(Icons.Default.Image, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        placeholder = { Text("https://ejemplo.com/foto.jpg") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("new_vehicle_photo_url"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Suggest presets Row
                    Text(
                        text = "Sugerencias de fotos realistas:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val presets = if (newVehicleType == "Car") {
                            listOf(
                                Pair("Deportivo Rojo", "https://images.unsplash.com/photo-1503376780353-7e6692767b70?auto=format&fit=crop&w=600&q=80"),
                                Pair("SUV Familiar", "https://images.unsplash.com/photo-1533473359331-0135ef1b58bf?auto=format&fit=crop&w=600&q=80"),
                                Pair("Compacto", "https://images.unsplash.com/photo-1549399542-7e3f8b79c341?auto=format&fit=crop&w=600&q=80")
                            )
                        } else {
                            listOf(
                                Pair("Alta Cilindrada", "https://images.unsplash.com/photo-1485965120184-e220f721d03e?auto=format&fit=crop&w=600&q=80"),
                                Pair("Scooter", "https://images.unsplash.com/photo-1568772585407-9361f9bf3a87?auto=format&fit=crop&w=600&q=80"),
                                Pair("Aventura", "https://images.unsplash.com/photo-1558981806-ec527fa84c39?auto=format&fit=crop&w=600&q=80")
                            )
                        }
                        for (p in presets) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f))
                                    .clickable { newVehiclePhotoUri = p.second }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = p.first,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val brand = if (selectedBrand == "Otro") customVehicleBrand.trim() else selectedBrand.trim()
                        val model = if (selectedModel == "Otro") customVehicleModel.trim() else selectedModel.trim()
                        val plate = newVehiclePlate.trim()
                        val odometerVal = initialOdometerText.toDoubleOrNull()
                        val yearVal = vehicleYearText.toIntOrNull() ?: java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)

                        var hasError = false
                        if (brand.isBlank()) {
                            brandError = true
                            hasError = true
                        }
                        if (model.isBlank()) {
                            modelError = true
                            hasError = true
                        }
                        if (plate.isBlank()) {
                            plateError = true
                            hasError = true
                        }
                        if (odometerVal == null || odometerVal < 0.0) {
                            odometerError = true
                            hasError = true
                        }

                        if (!hasError && odometerVal != null) {
                            val finalVehicleName = "$brand $model".trim()
                            viewModel.addVehicle(
                                name = finalVehicleName,
                                brand = brand,
                                model = model,
                                year = yearVal,
                                licensePlate = plate.uppercase(),
                                type = newVehicleType,
                                photoUri = newVehiclePhotoUri.ifBlank { null },
                                odometer = odometerVal
                            )

                            // Reset states after successful save
                            selectedBrand = ""
                            selectedModel = ""
                            customVehicleBrand = ""
                            customVehicleModel = ""
                            newVehiclePlate = ""
                            initialOdometerText = "15000"
                            newVehiclePhotoUri = ""
                            brandError = false
                            modelError = false
                            plateError = false
                            odometerError = false
                            showAddVehicleDialog = false
                        }
                    },
                    modifier = Modifier.testTag("confirm_add_vehicle_button")
                ) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        selectedBrand = ""
                        selectedModel = ""
                        customVehicleBrand = ""
                        customVehicleModel = ""
                        newVehiclePlate = ""
                        initialOdometerText = "15000"
                        newVehiclePhotoUri = ""
                        brandError = false
                        modelError = false
                        plateError = false
                        odometerError = false
                        showAddVehicleDialog = false
                    }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Maintenance Config Dialog
    if (showMaintenanceConfigDialog) {
        AlertDialog(
            onDismissRequest = { showMaintenanceConfigDialog = false },
            title = {
                Text(
                    text = "Ajustes de Mantenimiento",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 460.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Personaliza los detalles e intervalos para cada tipo de servicio.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    categoryConfig.forEach { (catName, config) ->
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable {
                                editingCategory = catName
                                tempSubtitle = config.first
                                tempKm = config.second.toInt().toString()
                                tempDays = config.third.toString()
                            },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(catName, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Detalle: ${config.first}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                                Text("Intervalo: ${config.second.toInt()} km / ${config.third} días", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showMaintenanceConfigDialog = false }) {
                    Text("Cerrar")
                }
            }
        )
    }

    if (editingCategory != null) {
        AlertDialog(
            onDismissRequest = { editingCategory = null },
            title = { Text("Editar $editingCategory") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = tempSubtitle,
                        onValueChange = { tempSubtitle = it },
                        label = { Text("Detalle (Ej. SINTÉTICO 5W-30)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = tempKm,
                        onValueChange = { tempKm = it },
                        label = { Text("Intervalo (Kilómetros)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = tempDays,
                        onValueChange = { tempDays = it },
                        label = { Text("Intervalo (Días)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val kmVal = tempKm.toDoubleOrNull() ?: 10000.0
                    val daysVal = tempDays.toIntOrNull() ?: 180
                    viewModel.saveCategoryConfigItem(editingCategory!!, tempSubtitle, kmVal, daysVal)
                    editingCategory = null
                }) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingCategory = null }) { Text("Cancelar") }
            }
        )
    }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val isScrolled by remember { derivedStateOf { scrollBehavior.state.overlappedFraction > 0.01f } }
    val headerColor by animateColorAsState(
        targetValue = if (isScrolled) MaterialTheme.colorScheme.surfaceContainerLowest else Color.Transparent,
        label = "headerColor"
    )
    val contentColor by animateColorAsState(
        targetValue = if (isScrolled) MaterialTheme.colorScheme.primary else Color.White,
        label = "contentColor"
    )

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            Column(modifier = Modifier.background(headerColor)) {
                Spacer(modifier = Modifier.statusBarsPadding().height(16.dp))
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text(
                                text = "Perfil",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = contentColor
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = { viewModel.selectTab(GarageTab.DASHBOARD) }
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Atrás",
                                tint = contentColor
                            )
                        }
                    },
                    scrollBehavior = scrollBehavior,
                    windowInsets = WindowInsets(0, 0, 0, 0),
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent
                    )
                )
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // User Portrait & Header Information
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box {
                    // Profile image wrapper circular
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                            .clickable { showPhotoSourceDialog = true },
                        contentAlignment = Alignment.Center
                    ) {
                        if (userProfile.avatarUrl.isNotBlank()) {
                            AsyncImage(
                                model = userProfile.avatarUrl,
                                contentDescription = "Profile Picture",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                modifier = Modifier.size(54.dp)
                            )
                        }
                    }

                    // Edit Pencil overlay icon
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .align(Alignment.BottomEnd)
                            .clickable { showPhotoSourceDialog = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Editar",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Text(
                    text = userProfile.name,
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = userProfile.email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline
                )

                // Bento grid row statistics
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDarkTheme) MaterialTheme.colorScheme.surfaceContainer else Color.White
                        ),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = if (isDarkTheme) 0.dp else 2.dp
                        ),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = if (isDarkTheme) {
                                Brush.linearGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.12f)
                                    )
                                )
                            } else {
                                SolidColor(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f))
                            }
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = allVehicles.size.toString(),
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "VEHÍCULOS",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDarkTheme) MaterialTheme.colorScheme.surfaceContainer else Color.White
                        ),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = if (isDarkTheme) 0.dp else 2.dp
                        ),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = if (isDarkTheme) {
                                Brush.linearGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.12f)
                                    )
                                )
                            } else {
                                SolidColor(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f))
                            }
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = allLogs.size.toString(),
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "SERVICIOS",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // LIST SECTION LISTS
            // SECTION: FLOTA
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "FLOTA",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp)
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDarkTheme) MaterialTheme.colorScheme.surfaceContainer else Color.White
                    ),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = if (isDarkTheme) 0.dp else 2.dp
                    ),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = if (isDarkTheme) {
                            Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.12f)
                                )
                            )
                        } else {
                            SolidColor(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f))
                        }
                    )
                ) {
                    Column {
                        // Clickable list Row: "Mis Vehículos"
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showVehiclesDialog = true }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DirectionsCar,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    "Mis Vehículos",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outlineVariant
                            )
                        }

                        Divider(
                            modifier = Modifier.padding(start = 52.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )

                        // Clickable list Row: "Añadir Vehículo"
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showAddVehicleDialog = true }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    "Añadir vehículo",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                PremiumBadgeIcon(size = 14.dp)
                            }
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outlineVariant
                            )
                        }
                    }
                }
            }

            // SECTION: AJUSTES
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "AJUSTES",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp)
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDarkTheme) MaterialTheme.colorScheme.surfaceContainer else Color.White
                    ),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = if (isDarkTheme) 0.dp else 2.dp
                    ),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = if (isDarkTheme) {
                            Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.12f)
                                )
                            )
                        } else {
                            SolidColor(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f))
                        }
                    )
                ) {
                    Column {
                        // Clickable list Row: "Notificaciones"
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    notificationStatus = if (notificationStatus == "Activadas") "Desactivadas" else "Activadas"
                                }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            Text(
                                "Notificaciones",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )

                            Text(
                                notificationStatus,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.padding(end = 4.dp)
                            )

                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outlineVariant
                            )
                        }

                        Divider(
                            modifier = Modifier.padding(start = 52.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )

                        // Row: "Seguridad"
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { /* detail popup */ }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    "Seguridad",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outlineVariant
                            )
                        }

                        Divider(
                            modifier = Modifier.padding(start = 52.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )

                        // Row: "Mantenimiento"
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showMaintenanceConfigDialog = true }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Build,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    "Mantenimiento",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outlineVariant
                            )
                        }

                        Divider(
                            modifier = Modifier.padding(start = 52.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )

                        // Row: "Idioma"
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { /* language switcher dialog */ }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            Text(
                                "Idioma",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )

                            Text(
                                "Español",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.padding(end = 4.dp)
                            )

                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outlineVariant
                            )
                        }
                    }
                }
            }

            // SECTION: PREFERENCIAS (Unidades: KM MI)
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "PREFERENCIAS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp)
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDarkTheme) MaterialTheme.colorScheme.surfaceContainer else Color.White
                    ),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = if (isDarkTheme) 0.dp else 2.dp
                    ),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = if (isDarkTheme) {
                            Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.12f)
                                )
                            )
                        } else {
                            SolidColor(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f))
                        }
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Straighten,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "Unidades",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Sliding style segment toggle: KM | MI
                        val isKmActive = userProfile.useKm
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                                .padding(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        if (isKmActive) MaterialTheme.colorScheme.surfaceContainerLowest
                                        else Color.Transparent
                                    )
                                    .clickable { viewModel.toggleUnit(true) }
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                                    .testTag("unit_km_button"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "KM",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isKmActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        if (!isKmActive) MaterialTheme.colorScheme.surfaceContainerLowest
                                        else Color.Transparent
                                    )
                                    .clickable { viewModel.toggleUnit(false) }
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                                    .testTag("unit_mi_button"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "MI",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (!isKmActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }

                    // GPS Tracking Segment (Premium)
                    val locationPermissionsState = rememberMultiplePermissionsState(
                        permissions = listOf(
                            android.Manifest.permission.ACCESS_COARSE_LOCATION,
                            android.Manifest.permission.ACCESS_FINE_LOCATION
                        )
                    )
                    
                    val bgLocationPermissionState = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                        com.google.accompanist.permissions.rememberPermissionState(android.Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                    } else null
                    
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.GpsFixed,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        "Rastreo",
                                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    PremiumBadgeIcon(size = 14.dp)
                                }
                                Text(
                                    "Usa GPS en segundo plano",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        
                        Switch(
                            checked = locationPermissionsState.allPermissionsGranted && userProfile.isPremium,
                            onCheckedChange = { isChecked ->
                                if (isChecked) {
                                    if (locationPermissionsState.allPermissionsGranted) {
                                        if (bgLocationPermissionState != null && !bgLocationPermissionState.status.isGranted) {
                                            bgLocationPermissionState.launchPermissionRequest()
                                        } else {
                                            viewModel.togglePremiumTelemetry(true)
                                        }
                                    } else {
                                        locationPermissionsState.launchMultiplePermissionRequest()
                                    }
                                } else {
                                    viewModel.togglePremiumTelemetry(false)
                                }
                            }
                        )
                    }

                    androidx.compose.runtime.LaunchedEffect(locationPermissionsState.allPermissionsGranted, bgLocationPermissionState?.status?.isGranted) {
                        // Si acaban de otorgar los permisos y todavía no es premium en DB (pero querían activarlo)
                        // Para simplificar, si dan permiso, asumimos que querían activar el premium telemetry.
                        if (locationPermissionsState.allPermissionsGranted && !userProfile.isPremium) {
                            if (bgLocationPermissionState == null || bgLocationPermissionState.status.isGranted) {
                                viewModel.togglePremiumTelemetry(true)
                            }
                        }
                    }

                    // Log de Rastreo
                    val activeVehicleForLog = allVehicles.find { it.isActive }
                    if (activeVehicleForLog != null && locationPermissionsState.allPermissionsGranted) {
                        val context = androidx.compose.ui.platform.LocalContext.current
                        val gpsPrefs = remember(activeVehicleForLog.id) { context.getSharedPreferences("GaragePulsePrefs", android.content.Context.MODE_PRIVATE) }
                        var gpsDistanceKm by remember(activeVehicleForLog.id) {
                            mutableStateOf(gpsPrefs.getFloat("gps_distance_vehicle_${activeVehicleForLog.id}", 0f).toDouble())
                        }
                        val todayStr = remember { java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date()) }
                        var gpsDistanceToday by remember(activeVehicleForLog.id) {
                            mutableStateOf(gpsPrefs.getFloat("gps_distance_today_${activeVehicleForLog.id}_$todayStr", 0f).toDouble())
                        }
                        androidx.compose.runtime.LaunchedEffect(activeVehicleForLog.odometer, activeVehicleForLog.lastUpdatedDate) {
                            gpsDistanceKm = gpsPrefs.getFloat("gps_distance_vehicle_${activeVehicleForLog.id}", 0f).toDouble()
                            gpsDistanceToday = gpsPrefs.getFloat("gps_distance_today_${activeVehicleForLog.id}_$todayStr", 0f).toDouble()
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                "Log de Rastreo GPS",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.Black.copy(alpha = 0.8f))
                                    .padding(12.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    val locStr = activeVehicleForLog.lastKnownLocation ?: "Sin ubicación registrada"
                                    val dateStr = activeVehicleForLog.lastUpdatedDate?.let {
                                        java.text.SimpleDateFormat("dd/MM/yy HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(it))
                                    } ?: "N/A"
                                    val useKm = userProfile?.useKm ?: true
                                    val displayOdo = if (useKm) activeVehicleForLog.odometer else activeVehicleForLog.odometer * 0.621371
                                    val odoUnit = if (useKm) "km" else "mi"
                                    val displayGpsToday = if (useKm) gpsDistanceToday else gpsDistanceToday * 0.621371
                                    val displayGpsTotal = if (useKm) gpsDistanceKm else gpsDistanceKm * 0.621371
                                    val gpsUnit = if (useKm) "km" else "mi"

                                    Text("Ubicación: $locStr", color = Color.Green, style = MaterialTheme.typography.bodySmall, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                                    Text(String.format(java.util.Locale.US, "Odómetro: %.2f %s", displayOdo, odoUnit), color = Color.Green, style = MaterialTheme.typography.bodySmall, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                                    Text(String.format(java.util.Locale.US, "Rastreo Hoy: %.2f %s", displayGpsToday, gpsUnit), color = Color.Green, style = MaterialTheme.typography.bodySmall, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                                    Text(String.format(java.util.Locale.US, "Rastreo Total: %.2f %s", displayGpsTotal, gpsUnit), color = Color.Green, style = MaterialTheme.typography.bodySmall, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                                    Text("Última act.: $dateStr", color = Color.Green, style = MaterialTheme.typography.bodySmall, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                                }
                            }
                        }
                    }

                    // Divider and Theme Toggle Segments
                    val isDark = isDarkTheme
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = if (isDark) Icons.Default.DarkMode else Icons.Default.LightMode,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "Tema",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                                .padding(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        if (!isDark) MaterialTheme.colorScheme.surfaceContainerLowest
                                        else Color.Transparent
                                    )
                                    .clickable { if (isDark) viewModel.toggleTheme() }
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                                    .testTag("theme_light_button"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "CLARO",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (!isDark) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        if (isDark) MaterialTheme.colorScheme.surfaceContainerLowest
                                        else Color.Transparent
                                    )
                                    .clickable { if (!isDark) viewModel.toggleTheme() }
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                                    .testTag("theme_dark_button"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "OSCURO",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                }
            }


            // CORRESPONDING CERRAR SESIÓN RED BUTTON
            Button(
                onClick = {
                    viewModel.logoutUser()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("logout_button"),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE53935),
                    contentColor = Color.White
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Logout,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "Cerrar sesión",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // TEMPORARY ONBOARDING shortCUT
            OutlinedButton(
                onClick = {
                    viewModel.resetToAuth()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("temp_onboarding_shortcut"),
                shape = RoundedCornerShape(26.dp),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFE75C31)),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFFE75C31)
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Build,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        "Probar Onboarding & Registro",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            // APP VERSION TAG
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "GaragePulse v2.4.0 (Build 1082)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.padding(bottom = 24.dp), // Normal pad
                    fontSize = 11.sp
                )
            }
        }
    }
}
