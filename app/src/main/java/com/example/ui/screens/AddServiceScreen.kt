package com.example.ui.screens

import android.app.DatePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.GarageTab
import com.example.ui.viewmodel.GarageViewModel
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.compose.AsyncImagePainter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import com.example.ui.components.PremiumBadgeIcon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddServiceScreen(
    viewModel: GarageViewModel,
    modifier: Modifier = Modifier
) {
    val activeVehicle by viewModel.activeVehicle.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val isDark by viewModel.isDarkTheme.collectAsState()

    val useKm = userProfile.useKm
    val unitLabel = if (useKm) "KM" else "MI"

    // Form inputs collected from ViewModel
    val category by viewModel.formCategory.collectAsState()
    val formTitle by viewModel.formTitle.collectAsState()
    val dateLong by viewModel.formDate.collectAsState()
    val costStr by viewModel.formCost.collectAsState()
    val mileageStr by viewModel.formMileage.collectAsState()
    val notesStr by viewModel.formNotes.collectAsState()

    // State managers
    var isCategoryDropdownExpanded by remember { mutableStateOf(false) }
    var showOtroDialog by remember { mutableStateOf(false) }
    var customCategoryText by remember { mutableStateOf("") }
    var showHelpDialog by remember { mutableStateOf(false) }
    var saveStatusText by remember { mutableStateOf("Guardar Registro") }
    var isSaving by remember { mutableStateOf(false) }
    var savedSuccessfully by remember { mutableStateOf(false) }

    // AI Mode and simulated media attachment states
    var isAiMode by remember { mutableStateOf(false) }
    var aiPromptText by remember { mutableStateOf("") }
    var attachedImagePath by remember { mutableStateOf<String?>(null) }
    var isRecordingAudio by remember { mutableStateOf(false) }
    var attachedAudioPlayState by remember { mutableStateOf(false) }
    var attachedAudioDuration by remember { mutableStateOf<String?>(null) }
    var showAiExtractionPreview by remember { mutableStateOf(false) }
    var aiExtractedCategory by remember { mutableStateOf("Cambio de Aceite") }
    var aiExtractedCost by remember { mutableStateOf("65.00") }
    var aiExtractedMileage by remember { mutableStateOf("45200") }
    var aiExtractedNotes by remember { mutableStateOf("") }
    var isAiProcessing by remember { mutableStateOf(false) }
    var aiProcessingStateText by remember { mutableStateOf("") }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Formatter for calendar selected date
    val dateFormat = remember { SimpleDateFormat("MM/dd/yyyy", Locale.US) }
    val dateDisplay = remember(dateLong) { dateFormat.format(dateLong) }

    // Estimate display calculation
    val estimateKM = activeVehicle?.odometer ?: 45200.0
    val estimateConverted = if (useKm) estimateKM else estimateKM * 0.621371
    val estimateDisplay = String.format(Locale.US, "%,.0f", estimateConverted)

    // Help Dialog if helping icon ? is clicked
    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            title = { Text("Registrar Nuevo Servicio") },
            text = {
                Text("Esta pantalla te permite registrar un nuevo mantenimiento técnico o reparación para tu vehículo activo. Se recalculará la salud predictiva y el kilometraje registrado automáticamente.")
            },
            confirmButton = {
                TextButton(onClick = { showHelpDialog = false }) {
                    Text("Entendido")
                }
            }
        )
    }

    // Dialog for select or add custom categories ("Otro")
    if (showOtroDialog) {
        val selectedCats = if (category.isBlank()) emptyList() else category.split(", ").map { it.trim() }.filter { it.isNotEmpty() }
        AlertDialog(
            onDismissRequest = { showOtroDialog = false },
            title = {
                Text(
                    text = "Otras Categorías",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Selecciona categorías predefinidas o escribe una nueva personalizada abajo:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Compact selection pills
                    val alternatives = listOf(
                        "Motor", "Suspensión", "Eléctrico", 
                        "Transmisión", "Escape", "Carrocería", "General"
                    )
                    
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            for (j in 0..2) {
                                val alt = alternatives[j]
                                val isSelected = selectedCats.contains(alt)
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            val newCats = if (isSelected) selectedCats - alt else selectedCats + alt
                                            viewModel.setFormCategory(newCats.joinToString(", "))
                                        },
                                    shape = RoundedCornerShape(20.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    )
                                ) {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = alt,
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            for (j in 3..5) {
                                val alt = alternatives[j]
                                val isSelected = selectedCats.contains(alt)
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            val newCats = if (isSelected) selectedCats - alt else selectedCats + alt
                                            viewModel.setFormCategory(newCats.joinToString(", "))
                                        },
                                    shape = RoundedCornerShape(20.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    )
                                ) {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = alt,
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val alt = alternatives[6]
                            val isSelected = selectedCats.contains(alt)
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        val newCats = if (isSelected) selectedCats - alt else selectedCats + alt
                                        viewModel.setFormCategory(newCats.joinToString(", "))
                                    },
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = alt,
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.weight(2f))
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Custom input category text box
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "AÑADIR CATEGORÍA PERSONALIZADA:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                            fontWeight = FontWeight.Bold
                        )
                        OutlinedTextField(
                            value = customCategoryText,
                            onValueChange = { customCategoryText = it },
                            placeholder = { Text("Ej. Dirección, Retrovisores...") },
                            modifier = Modifier.fillMaxWidth().testTag("custom_category_input"),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            )
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (customCategoryText.isNotBlank()) {
                            val currentSelected = if (category.isBlank()) emptyList() else category.split(", ").map { it.trim() }.filter { it.isNotEmpty() }
                            val cleanedCustom = customCategoryText.trim()
                            if (!currentSelected.contains(cleanedCustom)) {
                                val newCats = currentSelected + cleanedCustom
                                viewModel.setFormCategory(newCats.joinToString(", "))
                            }
                            customCategoryText = ""
                        }
                        showOtroDialog = false
                    },
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Aceptar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showOtroDialog = false }
                ) {
                    Text("Cerrar")
                }
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
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text(
                                text = "Nuevo Servicio",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = contentColor
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = { viewModel.selectTab(GarageTab.DASHBOARD) },
                            modifier = Modifier.testTag("form_close_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Atrás",
                                tint = contentColor
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { viewModel.selectTab(GarageTab.PROFILE) },
                            modifier = Modifier.testTag("form_profile_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Perfil",
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
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    if (isDark) {
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF0D0F16), // Dark premium canvas background
                                Color(0xFF171B26)
                            )
                        )
                    } else {
                        SolidColor(MaterialTheme.colorScheme.background)
                    }
                )
                .padding(bottom = innerPadding.calculateBottomPadding())
        ) {


            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Hero cover Image of the vehicle with background similar to Dashboard vehicle card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.22f)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF0B0D13), // deep dark immersive background
                                    Color(0xFF131722)
                                )
                            )
                        )
                ) {
                    // Subtle glowing radial background highlight
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        Color(0xFFe75c31).copy(alpha = 0.22f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )

                    VehiclePhotoOrIllustration(
                        vehicle = activeVehicle,
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight()
                            .statusBarsPadding()
                            .padding(top = 56.dp, bottom = 12.dp),
                        contentScale = ContentScale.Fit
                    )

                    // Premium linear dark gradient overlay to ensure perfect background text contrast at the bottom
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.5f)
                                    )
                                )
                            )
                    )

                    // Overlay active vehicle metadata over bottom left of illustration
                    activeVehicle?.let { vehicle ->
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "Placa: ${vehicle.licensePlate}",
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White.copy(alpha = 0.85f),
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = vehicle.name,
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Black
                                ),
                                color = Color.White
                            )
                        }
                    }
                }

                // Wrap all lower interactive controls and forms in a padded container
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                // MODE SELECTOR BAR
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (isDark) Color.White.copy(alpha = 0.04f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        RoundedCornerShape(16.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = if (isDark) Color.White.copy(alpha = 0.12f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Manual Button
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (!isAiMode) Color(0xFFE75C31).copy(alpha = 0.18f) else Color.Transparent)
                        .clickable { isAiMode = false }
                        .padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = if (!isAiMode) Color(0xFFE75C31) else (if (isDark) Color.White.copy(alpha = 0.4f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Entrada Manual",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (!isAiMode) (if (isDark) Color.White else Color(0xFFE75C31)) else (if (isDark) Color.White.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                    )
                }

                // AI Button
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isAiMode) Color(0xFFE75C31).copy(alpha = 0.18f) else Color.Transparent)
                        .clickable { isAiMode = true }
                        .padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = if (isAiMode) Color(0xFFE75C31) else (if (isDark) Color.White.copy(alpha = 0.4f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Asistente IA",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (isAiMode) (if (isDark) Color.White else Color(0xFFE75C31)) else (if (isDark) Color.White.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    PremiumBadgeIcon(size = 14.dp)
                }
            }

            if (!isAiMode) {
                // SECTION 1: SELECCIONAR CATEGORÍA
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "SELECCIONAR CATEGORÍA",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isDark) Color.White.copy(alpha = 0.62f) else MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(start = 4.dp),
                        fontWeight = FontWeight.Bold
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDark) Color.White.copy(alpha = 0.04f) else Color.White
                        ),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = if (isDark) 0.dp else 2.dp
                        ),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = if (isDark) {
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.15f),
                                        Color.White.copy(alpha = 0.05f)
                                    )
                                )
                            } else {
                                SolidColor(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f))
                            }
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            val items = listOf(
                                Triple("Cambio de Aceite", "Aceite", Icons.Default.OilBarrel),
                                Triple("Filtros", "Filtros", Icons.Default.Build),
                                Triple("Frenos", "Frenos", Icons.Default.Settings),
                                Triple("Neumáticos", "Llantas", Icons.Default.TireRepair),
                                Triple("Batería", "Batería", Icons.Default.FlashOn)
                            )
                            
                            val standardKeys = items.map { it.first }
                            val selectedCats = if (category.isBlank()) emptyList() else category.split(", ").map { it.trim() }.filter { it.isNotEmpty() }
                            val customKeys = selectedCats.filter { it !in standardKeys }

                            // Row 1
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                for (i in 0..2) {
                                    val (key, label, icon) = items[i]
                                    val isSelected = selectedCats.contains(key)
                                    CategoryGridButton(
                                        label = label,
                                        icon = icon,
                                        isSelected = isSelected,
                                        isDark = isDark,
                                        onClick = {
                                            val newCats = if (isSelected) selectedCats - key else selectedCats + key
                                            viewModel.setFormCategory(newCats.joinToString(", "))
                                        },
                                        modifier = Modifier.weight(1f).testTag("category_btn_$label")
                                    )
                                }
                            }

                            // Row 2
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Llantas
                                val (key4, label4, icon4) = items[3]
                                val isSelected4 = selectedCats.contains(key4)
                                CategoryGridButton(
                                    label = label4,
                                    icon = icon4,
                                    isSelected = isSelected4,
                                    isDark = isDark,
                                    onClick = {
                                        val newCats = if (isSelected4) selectedCats - key4 else selectedCats + key4
                                        viewModel.setFormCategory(newCats.joinToString(", "))
                                    },
                                    modifier = Modifier.weight(1f).testTag("category_btn_$label4")
                                )

                                // Batería
                                val (key5, label5, icon5) = items[4]
                                val isSelected5 = selectedCats.contains(key5)
                                CategoryGridButton(
                                    label = label5,
                                    icon = icon5,
                                    isSelected = isSelected5,
                                    isDark = isDark,
                                    onClick = {
                                        val newCats = if (isSelected5) selectedCats - key5 else selectedCats + key5
                                        viewModel.setFormCategory(newCats.joinToString(", "))
                                    },
                                    modifier = Modifier.weight(1f).testTag("category_btn_$label5")
                                )

                                // Otro
                                val isOtroSelected = customKeys.isNotEmpty()
                                val oLabel = if (isOtroSelected) {
                                    if (customKeys.size == 1) customKeys.first() else "Otros (${customKeys.size})"
                                } else {
                                    "Otro"
                                }
                                CategoryGridButton(
                                    label = oLabel,
                                    icon = Icons.Default.MoreHoriz,
                                    isSelected = isOtroSelected,
                                    isDark = isDark,
                                    onClick = { showOtroDialog = true },
                                    modifier = Modifier.weight(1f).testTag("category_btn_Otro")
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Detalle del Servicio
                            val suggestions = remember(category) {
                                val cats = category.split(",").map { it.trim() }
                                val mainCat = cats.firstOrNull() ?: ""
                                when(mainCat) {
                                    "Neumáticos" -> listOf("Rotación y Balanceo", "Cambio de Neumáticos", "Alineación", "Reparación/Parche")
                                    "Frenos" -> listOf("Pastillas y Discos", "Cambio de Liga", "Ajuste/Revisión")
                                    "Batería" -> listOf("Test de Corriente", "Cambio de Batería", "Limpieza de Bornes")
                                    "Cambio de Aceite" -> listOf("Sintético 5W-30", "Mineral", "Semi-Sintético")
                                    "Filtros" -> listOf("Filtros de Aire y AC", "Filtro de Gasolina", "Filtro de Aceite")
                                    else -> emptyList()
                                }
                            }

                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "DETALLE DEL SERVICIO",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isDark) Color.White.copy(alpha = 0.62f) else MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.padding(bottom = 6.dp),
                                    fontWeight = FontWeight.Bold
                                )
                                OutlinedTextField(
                                    value = formTitle,
                                    onValueChange = { viewModel.setFormTitle(it) },
                                    placeholder = { Text("Ej: Cambio de 2 neumáticos") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                                    )
                                )
                                
                                if (suggestions.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        suggestions.forEach { suggestion ->
                                            SuggestionChip(
                                                onClick = { viewModel.setFormTitle(suggestion) },
                                                label = { Text(suggestion) },
                                                colors = SuggestionChipDefaults.suggestionChipColors(
                                                    containerColor = if (formTitle == suggestion) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                                                )
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            HorizontalDivider(
                                thickness = 0.5.dp,
                                color = if (isDark) Color.White.copy(alpha = 0.1f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )

                            Spacer(modifier = Modifier.height(2.dp))

                            // Snug Kilometraje and Date Row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Kilometraje
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Outlined.Speed, contentDescription = null, tint = Color(0xFFE75C31), modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text(if (useKm) "Kilometraje" else "Millaje", style = MaterialTheme.typography.labelSmall, color = if (isDark) Color.White.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    BasicTextField(
                                        value = mileageStr,
                                        onValueChange = { viewModel.setFormMileage(it) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp, fontWeight = FontWeight.Medium, color = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface),
                                        modifier = Modifier.fillMaxWidth().testTag("mileage_input"),
                                        decorationBox = { innerTextField ->
                                            if (mileageStr.isEmpty()) {
                                                Text("Ej. 45200", style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp), color = if (isDark) Color.White.copy(alpha = 0.35f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                                            }
                                            innerTextField()
                                        }
                                    )
                                }

                                // Divider
                                Box(
                                    modifier = Modifier
                                        .width(1.dp)
                                        .height(36.dp)
                                        .background(if (isDark) Color.White.copy(alpha = 0.1f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                )

                                // Fecha
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            val calendar = Calendar.getInstance()
                                            calendar.timeInMillis = dateLong
                                            DatePickerDialog(
                                                context,
                                                { _, year, month, dayOfMonth ->
                                                    val newCal = Calendar.getInstance()
                                                    newCal.set(year, month, dayOfMonth)
                                                    viewModel.setFormDate(newCal.timeInMillis)
                                                },
                                                calendar.get(Calendar.YEAR),
                                                calendar.get(Calendar.MONTH),
                                                calendar.get(Calendar.DAY_OF_MONTH)
                                            ).show()
                                        }
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.CalendarToday, contentDescription = null, tint = Color(0xFFE75C31), modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Fecha", style = MaterialTheme.typography.labelSmall, color = if (isDark) Color.White.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = dateDisplay,
                                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp, fontWeight = FontWeight.Medium),
                                        color = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.testTag("date_input")
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(2.dp))
                            
                            // Estimado / Usar Actual helper
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp)
                                    .padding(bottom = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Estimado: $estimateDisplay $unitLabel",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                    color = if (isDark) Color.White.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold
                                )

                                Text(
                                    text = "Usar Actual",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFFE75C31),
                                    modifier = Modifier
                                        .clickable {
                                            activeVehicle?.odometer?.let {
                                                viewModel.setFormMileage(it.toInt().toString())
                                            }
                                        }
                                        .testTag("use_actual_mileage_button")
                                )
                            }
                        }
                    }
                }



                // SECTION 3: NOTAS ADICIONALES
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "NOTAS ADICIONALES",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isDark) Color.White.copy(alpha = 0.62f) else MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(start = 4.dp),
                        fontWeight = FontWeight.Bold
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDark) Color.White.copy(alpha = 0.04f) else Color.White
                        ),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = if (isDark) 0.dp else 2.dp
                        ),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = if (isDark) {
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.15f),
                                        Color.White.copy(alpha = 0.05f)
                                    )
                                )
                            } else {
                                SolidColor(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f))
                            }
                        )
                    ) {
                        TextField(
                            value = notesStr,
                            onValueChange = { viewModel.setFormNotes(it) },
                            placeholder = {
                                Text(
                                    "Describe los detalles del servicio...",
                                    color = if (isDark) Color.White.copy(alpha = 0.35f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            },
                            colors = TextFieldDefaults.colors(
                                focusedTextColor = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface,
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                disabledIndicatorColor = Color.Transparent
                            ),
                            textStyle = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp)
                                .heightIn(min = 100.dp)
                                .testTag("notes_input")
                        )
                    }
                }


                // PRIMARY SAVING ACTION BUTTON
                Button(
                    onClick = {
                        if (!isSaving) {
                            isSaving = true
                            saveStatusText = "Guardando..."
                            coroutineScope.launch {
                                delay(1000) // Aesthetic delay as seen in HTML script
                                viewModel.saveServiceLog {
                                    savedSuccessfully = true
                                    saveStatusText = "¡Guardado!"
                                    coroutineScope.launch {
                                        delay(1000)
                                        savedSuccessfully = false
                                        isSaving = false
                                        saveStatusText = "Guardar Registro"
                                        viewModel.selectTab(GarageTab.DASHBOARD)
                                    }
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("save_service_button"),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (savedSuccessfully) Color(0xFF34C759) else Color(0xFFE75C31),
                        contentColor = Color.White
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (savedSuccessfully) Icons.Default.CheckCircle else Icons.Default.Save,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = saveStatusText,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            } else {
                // 1. CARACTERÍSTICA INTELIGENTE CON IA
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("ai_editor_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) Color.White.copy(alpha = 0.04f) else Color.White
                    ),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = if (isDark) 0.dp else 2.dp
                    ),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFFE75C31).copy(alpha = 0.4f),
                                Color(0xFFE75C31).copy(alpha = 0.1f)
                            )
                        )
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // 2. UNIFIED PROMPT INPUT BOX WITH EMBEDDED ACTION BUTTONS
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = if (isDark) Color.White.copy(alpha = 0.02f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isDark) Color.White.copy(alpha = 0.10f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f),
                                    shape = RoundedCornerShape(20.dp)
                                )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = aiPromptText,
                                    onValueChange = { aiPromptText = it },
                                    placeholder = {
                                        Text(
                                            text = "Escribe o graba lo que le hiciste a tu vehículo (Ej. Cambio de aceite por $65. Kms: 45600)...",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (isDark) Color.White.copy(alpha = 0.35f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                        )
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 90.dp)
                                        .testTag("ai_prompt_input"),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface,
                                        unfocusedTextColor = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface,
                                        focusedBorderColor = Color.Transparent,
                                        unfocusedBorderColor = Color.Transparent,
                                        disabledBorderColor = Color.Transparent,
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent
                                    )
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                // Embedded dismissible attachment badges/chips if files are attached
                                if (attachedImagePath != null || attachedAudioDuration != null) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 8.dp, vertical = 4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (attachedImagePath != null) {
                                            Row(
                                                modifier = Modifier
                                                    .background(Color(0xFFE75C31).copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                                    .border(1.dp, Color(0xFFE75C31).copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.CheckCircle,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(14.dp),
                                                    tint = Color(0xFFE75C31)
                                                )
                                                Text(
                                                    text = "Factura Adjunta ✓",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = if (isDark) Color.White else Color(0xFFE75C31)
                                                )
                                                Spacer(modifier = Modifier.width(2.dp))
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Quitar",
                                                    modifier = Modifier
                                                        .size(14.dp)
                                                        .clickable { attachedImagePath = null },
                                                    tint = if (isDark) Color.White.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        if (attachedAudioDuration != null) {
                                            Row(
                                                modifier = Modifier
                                                    .background(Color(0xFFE75C31).copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                                    .border(1.dp, Color(0xFFE75C31).copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = if (attachedAudioPlayState) Icons.Default.Pause else Icons.Default.PlayArrow,
                                                    contentDescription = null,
                                                    modifier = Modifier
                                                        .size(14.dp)
                                                        .clickable { attachedAudioPlayState = !attachedAudioPlayState },
                                                    tint = Color(0xFFE75C31)
                                                )
                                                Text(
                                                    text = if (attachedAudioPlayState) "Reproduciendo..." else "Oír Nota ($attachedAudioDuration)",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = if (isDark) Color.White else Color(0xFFE75C31),
                                                    modifier = Modifier.clickable { attachedAudioPlayState = !attachedAudioPlayState }
                                                )
                                                Spacer(modifier = Modifier.width(2.dp))
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Quitar",
                                                    modifier = Modifier
                                                        .size(14.dp)
                                                        .clickable {
                                                            attachedAudioDuration = null
                                                            attachedAudioPlayState = false
                                                        },
                                                    tint = if (isDark) Color.White.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Recording indicator
                                    if (isRecordingAudio) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .background(color = Color.Red, shape = CircleShape)
                                            )
                                            Text(
                                                text = "Grabando Voz...",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.Red
                                            )
                                        }
                                    } else {
                                        Spacer(modifier = Modifier.width(1.dp))
                                    }

                                    // Compact Row of Action Buttons
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // 3. IMAGE ATTACHMENT ACTION BUTTON
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .background(
                                                    color = if (attachedImagePath != null) Color(0xFFE75C31).copy(alpha = 0.25f)
                                                            else (if (isDark) Color.White.copy(alpha = 0.05f) else MaterialTheme.colorScheme.surfaceVariant),
                                                    shape = RoundedCornerShape(10.dp)
                                                )
                                                .clickable {
                                                    if (attachedImagePath == null) {
                                                        attachedImagePath = "https://example.com/invoice.jpg"
                                                        if (aiPromptText.isEmpty()) {
                                                            aiPromptText = "Cambió de aceite y filtro de aire por $75.00."
                                                        }
                                                    } else {
                                                        attachedImagePath = null
                                                    }
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = if (attachedImagePath != null) Icons.Default.CheckCircle else Icons.Default.CameraAlt,
                                                contentDescription = "Agregar Imagen",
                                                tint = if (attachedImagePath != null) Color(0xFFE75C31) else (if (isDark) Color.White.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurfaceVariant),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }

                                        // 4. MIC / VOICE RECORD ACTION BUTTON
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .background(
                                                    color = if (isRecordingAudio) Color.Red.copy(alpha = 0.25f)
                                                            else if (attachedAudioDuration != null) Color(0xFFE75C31).copy(alpha = 0.25f)
                                                            else (if (isDark) Color.White.copy(alpha = 0.05f) else MaterialTheme.colorScheme.surfaceVariant),
                                                    shape = RoundedCornerShape(10.dp)
                                                )
                                                .clickable {
                                                    if (isRecordingAudio) {
                                                        isRecordingAudio = false
                                                        attachedAudioDuration = "0:12"
                                                        if (aiPromptText.isEmpty()) {
                                                            aiPromptText = "Hice el cambio de pastillas de freno por $145.00 hoy. Kms 45850."
                                                        }
                                                    } else if (attachedAudioDuration != null) {
                                                        attachedAudioPlayState = !attachedAudioPlayState
                                                    } else {
                                                        coroutineScope.launch {
                                                            isRecordingAudio = true
                                                            delay(2000)
                                                            if (isRecordingAudio) {
                                                                isRecordingAudio = false
                                                                attachedAudioDuration = "0:12"
                                                                if (aiPromptText.isEmpty()) {
                                                                    aiPromptText = "Hice el cambio de pastillas de freno por $145.00 hoy. Kms 45850."
                                                                }
                                                            }
                                                        }
                                                    }
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = if (isRecordingAudio) Icons.Default.MicNone 
                                                               else if (attachedAudioDuration != null) {
                                                                    if (attachedAudioPlayState) Icons.Default.Pause else Icons.Default.PlayArrow
                                                               } else Icons.Default.Mic,
                                                contentDescription = "Grabar Nota Voz",
                                                tint = if (isRecordingAudio) Color.Red 
                                                       else if (attachedAudioDuration != null) Color(0xFFE75C31) 
                                                       else (if (isDark) Color.White.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurfaceVariant),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // AI Saving Button
                Button(
                    onClick = {
                        val promptTextToParse = if (aiPromptText.isNotBlank()) aiPromptText else "Cambio de aceite de motor por $70 con odómetro de 45400."
                        if (!isAiProcessing) {
                            isAiProcessing = true
                            coroutineScope.launch {
                                aiProcessingStateText = "Analizando descripción..."
                                delay(600)
                                if (attachedImagePath != null) {
                                    aiProcessingStateText = "Leyendo factura adjunta..."
                                    delay(600)
                                }
                                if (attachedAudioDuration != null) {
                                    aiProcessingStateText = "Analizando voz procesada..."
                                    delay(600)
                                }
                                aiProcessingStateText = "Extrayendo campos..."
                                delay(600)
                                
                                // Parse locally
                                val extracted = extractServiceDetails(promptTextToParse)
                                aiExtractedCategory = extracted.category
                                aiExtractedCost = extracted.cost
                                aiExtractedMileage = extracted.mileage
                                aiExtractedNotes = if (aiPromptText.isNotBlank()) aiPromptText else "Mantenimiento preventivo extraído mediante audio y fotos."
                                
                                isAiProcessing = false
                                showAiExtractionPreview = true
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("ai_process_save_button"),
                    shape = RoundedCornerShape(26.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE75C31),
                        contentColor = Color.White
                    ),
                    enabled = !isAiProcessing
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (isAiProcessing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                            Text(
                                text = aiProcessingStateText,
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Procesar y Guardar con IA",
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            )
                        }
                    }
                }
            }

        }
    }

    }
}

    // AI dialog extraction preview validation
    if (showAiExtractionPreview) {
        AlertDialog(
            onDismissRequest = { showAiExtractionPreview = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Datos Extraídos por IA",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Nuestra inteligencia local extrajo los siguientes datos de tu descripción. Confirma si son correctos para registrarlos inmediatamente:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    // Extracted params layout
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Category
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Categoría:", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.primary)
                                Text(aiExtractedCategory, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                            }
                            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                            
                            // Cost
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Costo Total:", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.primary)
                                Text("$ ${aiExtractedCost}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                            }
                            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                            // Mileage
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(if (useKm) "Kilometraje:" else "Millaje:", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.primary)
                                Text("${aiExtractedMileage} ${unitLabel}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                            }
                            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                            // Notes summary
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text("Notas del servicio:", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.outline)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = aiExtractedNotes.ifEmpty { "Sin notas específicas." },
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 3,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showAiExtractionPreview = false
                        // Set ViewModel fields
                        viewModel.setFormCategory(aiExtractedCategory)
                        viewModel.setFormCost(aiExtractedCost)
                        viewModel.setFormMileage(aiExtractedMileage)
                        viewModel.setFormNotes(aiExtractedNotes + "\n[Registrado vía Asistente de IA]")
                        
                        // Trigger actual model saving
                        isSaving = true
                        saveStatusText = "Guardando..."
                        coroutineScope.launch {
                            viewModel.saveServiceLog {
                                savedSuccessfully = true
                                saveStatusText = "¡Guardado!"
                                coroutineScope.launch {
                                    delay(1000)
                                    savedSuccessfully = false
                                    isSaving = false
                                    saveStatusText = "Guardar Registro"
                                    viewModel.selectTab(GarageTab.DASHBOARD)
                                    aiPromptText = ""
                                    attachedImagePath = null
                                    attachedAudioDuration = null
                                }
                            }
                        }
                    },
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text("Confirmar y Registrar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAiExtractionPreview = false }) {
                    Text("Volver")
                }
            }
        )
    }
}

fun extractServiceDetails(promptText: String): ServiceExtraction {
    var category = "Otro"
    val lower = promptText.lowercase()
    if (lower.contains("aceite")) category = "Cambio de Aceite"
    else if (lower.contains("freno")) category = "Frenos"
    else if (lower.contains("bater")) category = "Batería"
    else if (lower.contains("llanta") || lower.contains("neumat")) category = "Neumáticos"
    else if (lower.contains("filtro")) category = "Filtros"
    
    val costRegex = Regex("(?:\\$|por\\s+)?([0-9]+(?:\\.[0-9]+)?)")
    val costs = costRegex.findAll(promptText).mapNotNull { it.groupValues[1].toDoubleOrNull() }.toList()
    val cost = costs.firstOrNull { it < 2000.0 } ?: 75.00
    
    val mileageRegex = Regex("([0-9]+)")
    val mileages = mileageRegex.findAll(promptText).mapNotNull { it.groupValues[1].toIntOrNull() }.toList()
    val mileage = mileages.firstOrNull { it > 2000 } ?: 45620
    
    return ServiceExtraction(category, String.format(Locale.US, "%.2f", cost), mileage.toString())
}

data class ServiceExtraction(
    val category: String,
    val cost: String,
    val mileage: String
)

@Composable
fun CategoryGridButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    isDark: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {

    val borderColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
    }
    
    val containerBg = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.3f)
    }
    
    val tintColor = if (isSelected) {
        if (isDark) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outline
    }

    val textColor = if (isSelected) {
        if (isDark) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(containerBg)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tintColor,
                modifier = Modifier.size(26.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                ),
                color = textColor,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
    }
}
