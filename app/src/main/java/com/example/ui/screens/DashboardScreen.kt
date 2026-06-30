package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.HealthAndSafety
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import com.example.data.model.Vehicle
import com.example.data.model.ServiceLog
import com.example.ui.viewmodel.GarageTab
import com.example.ui.viewmodel.GarageViewModel
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.compose.AsyncImagePainter
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.isGranted
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun DashboardScreen(
    viewModel: GarageViewModel,
    modifier: Modifier = Modifier
) {
    val locationPermissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            android.Manifest.permission.ACCESS_COARSE_LOCATION,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        )
    )

    val activeVehicle by viewModel.activeVehicle.collectAsState()
    val allLogs by viewModel.allServiceLogs.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val isDark by viewModel.isDarkTheme.collectAsState()

    var showSupportDialog by remember { mutableStateOf(false) }

    val useKm = userProfile.useKm
    val unitLabel = if (useKm) "KM" else "MI"

    // Fun to convert KM value based on unit selector
    fun displayDist(distKm: Double): String {
        val converted = if (useKm) distKm else distKm * 0.621371
        val formatter = NumberFormat.getNumberInstance(Locale.US)
        formatter.maximumFractionDigits = 0
        return formatter.format(converted)
    }

    val hasServiceLogs = allLogs.any { it.vehicleId == (activeVehicle?.id ?: -1) }
    val currentOdometer = activeVehicle?.odometer ?: 42500.0
    val currentTimeMillis = System.currentTimeMillis()

    data class TaskRule(val displayName: String, val intervalKm: Double, val intervalDays: Int)

    val predictiveConfig = mapOf(
        "Cambio de Aceite" to mapOf(
            "Aceite Sintético" to TaskRule("Aceite Sintético", 10000.0, 180),
            "Aceite Mineral" to TaskRule("Aceite Mineral", 5000.0, 90),
            "Filtro de Aceite" to TaskRule("Filtro de Aceite", 10000.0, 180),
            "default" to TaskRule("Revisión", 10000.0, 180)
        ),
        "Filtros" to mapOf(
            "Filtro de Aire" to TaskRule("Filtro de Aire", 15000.0, 365),
            "Filtro de Cabina" to TaskRule("Filtro de Cabina", 15000.0, 365),
            "default" to TaskRule("Revisión de Filtros", 15000.0, 365)
        ),
        "Frenos" to mapOf(
            "Pastillas" to TaskRule("Pastillas", 25000.0, 730),
            "Líquido de Frenos" to TaskRule("Líquido de Frenos", 40000.0, 730),
            "Discos" to TaskRule("Discos", 50000.0, 1095),
            "default" to TaskRule("Revisión", 30000.0, 730)
        ),
        "Neumáticos" to mapOf(
            "Cambio de Llantas" to TaskRule("Cambio de Llantas", 40000.0, 730),
            "Alineación" to TaskRule("Alineación", 10000.0, 180),
            "Balanceo" to TaskRule("Balanceo", 10000.0, 180),
            "Rotación" to TaskRule("Rotación", 10000.0, 180),
            "default" to TaskRule("Revisión", 40000.0, 730)
        ),
        "Batería" to mapOf(
            "Reemplazo" to TaskRule("Reemplazo", 60000.0, 1095),
            "default" to TaskRule("Revisión", 60000.0, 1095)
        )
    )

    val computedTasks = predictiveConfig.mapNotNull { (catName, tasksMap) ->
        val logsForCat = allLogs.filter { it.vehicleId == (activeVehicle?.id ?: -1) && it.category.split(", ").contains(catName) }
        
        val evaluations = tasksMap.mapNotNull { (detailKey, rule) ->
            val validLogs = if (detailKey == "default") {
                logsForCat
            } else {
                logsForCat.filter { it.details.split(",").map { d -> d.trim() }.contains(detailKey) }
            }
            
            val latestLog = validLogs.maxByOrNull { it.date }
            
            val baseMileage: Double
            val baseDate: Long

            if (latestLog != null) {
                baseMileage = latestLog.mileage
                baseDate = latestLog.date
            } else {
                return@mapNotNull null
            }

            val remainingKm = (baseMileage + rule.intervalKm) - currentOdometer
            val nextDateMillis = baseDate + (rule.intervalDays * 24L * 60 * 60 * 1000)
            val remainingDays = (nextDateMillis - currentTimeMillis).toDouble() / (24.0 * 60.0 * 60.0 * 1000.0)

            val wearKm = ((currentOdometer - baseMileage) / rule.intervalKm).toFloat().coerceIn(0f, 1f)
            val elapsedDays = (currentTimeMillis - baseDate).toDouble() / (24.0 * 60.0 * 60.0 * 1000.0)
            val wearDays = (elapsedDays / rule.intervalDays).toFloat().coerceIn(0f, 1f)
            val wearIndex = Math.max(wearKm, wearDays)

            val icon = when (catName) {
                "Cambio de Aceite" -> Icons.Default.OilBarrel
                "Filtros" -> Icons.Default.Build
                "Frenos" -> Icons.Default.Settings
                "Neumáticos" -> Icons.Default.TireRepair
                "Batería" -> Icons.Default.FlashOn
                else -> Icons.Default.Build
            }
            val iconColor = if (isDark) Color.White else MaterialTheme.colorScheme.primary

            MaintenanceTaskResult(
                category = catName,
                subtitle = rule.displayName,
                nextMilestoneKm = baseMileage + rule.intervalKm,
                remainingKm = remainingKm,
                remainingDays = remainingDays,
                wearIndex = wearIndex,
                isTimeUrgent = wearDays >= wearKm,
                icon = icon,
                iconColor = iconColor
            )
        }
        
        evaluations.maxByOrNull { it.wearIndex }
    }

    val mostUrgentTask = computedTasks.maxByOrNull { it.wearIndex } ?: computedTasks.firstOrNull()
    val averageWear = if (computedTasks.isNotEmpty()) computedTasks.map { it.wearIndex }.average().toFloat() else 0f
    val overallHealthPercent = Math.max(0f, 100f * (1.0f - averageWear))

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val isScrolled by remember { derivedStateOf { scrollBehavior.state.overlappedFraction > 0.01f } }
    val headerColor by animateColorAsState(
        targetValue = if (isScrolled) MaterialTheme.colorScheme.surfaceContainerLowest else Color.Transparent,
        label = "headerColor"
    )
    val contentColor by animateColorAsState(
        targetValue = if (isScrolled) MaterialTheme.colorScheme.primary else if (isDark) Color.White else MaterialTheme.colorScheme.onSurface,
        label = "contentColor"
    )

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            Column(modifier = Modifier.background(headerColor)) {
                Spacer(modifier = Modifier.statusBarsPadding())
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            SubcomposeAsyncImage(
                                model = if (userProfile.avatarUrl.isNotBlank()) userProfile.avatarUrl else "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&w=150&q=80",
                                contentDescription = "Foto de Perfil",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                            ) {
                                val state = painter.state
                                if (state is AsyncImagePainter.State.Error || state is AsyncImagePainter.State.Loading) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = "Perfil",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                } else {
                                    SubcomposeAsyncImageContent()
                                }
                            }
                            Text(
                                text = userProfile.name.split(" ").joinToString(" ") { it.replaceFirstChar { char -> if (char.isLowerCase()) char.titlecase(Locale.getDefault()) else char.toString() } },
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.15.sp
                                ),
                                color = contentColor
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { viewModel.toggleTheme() },
                            modifier = Modifier.testTag("dashboard_theme_toggle")
                        ) {
                            Icon(
                                imageVector = if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = "Cambiar tema",
                                tint = contentColor
                            )
                        }
                        IconButton(
                            onClick = { viewModel.selectTab(GarageTab.PROFILE) },
                            modifier = Modifier.testTag("dashboard_settings_button")
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
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.selectTab(GarageTab.ADD) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape,
                modifier = Modifier
                    .padding(bottom = 16.dp, end = 4.dp)
                    .testTag("dashboard_fab_add")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Nuevo Registro",
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(bottom = innerPadding.calculateBottomPadding() + 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. ACTIVE VEHICLE HEADER CARD (Minimal, immersive, and styled to match Nuevo Servicio screen proportions and layouts)
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

                // Centered/floating illustration of the vehicle, exactly proportioned as in Servicio
                VehiclePhotoOrIllustration(
                    vehicle = activeVehicle,
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .statusBarsPadding()
                        .padding(top = 56.dp, bottom = 12.dp),
                    contentScale = ContentScale.Fit
                )

                // Visual shadow gradient overlay at the bottom so text layers remain highly readable
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.45f)
                                )
                            )
                        )
                )

                // Bottom Right: Vehicle metadata titles aligned to the right and overlaid on top of the image (matching Servicio text styling but aligned end)
                activeVehicle?.let { vehicle ->
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = "Placa: ${vehicle.licensePlate}",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White.copy(alpha = 0.85f),
                            fontWeight = FontWeight.Bold,
                            textAlign = androidx.compose.ui.text.style.TextAlign.End
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = vehicle.name,
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Black
                            ),
                            color = Color.White,
                            textAlign = androidx.compose.ui.text.style.TextAlign.End
                        )
                    }
                }
            }

            // Sub-Column to pad all subsequent content cards horizontally
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Banner de Calibración
                if ((activeVehicle?.calculatedKpd ?: 0.0) <= 0.0) {
                    val calibKm = Math.max(0.0, (activeVehicle?.odometer ?: 0.0) - (activeVehicle?.initialKm ?: activeVehicle?.odometer ?: 0.0))
                    val kpdValue = activeVehicle?.calculatedKpd ?: 0.0

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                        text = "ESTADÍSTICAS DE CONTROL",
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
                            containerColor = if (isDark) MaterialTheme.colorScheme.surfaceContainer else Color.White
                        ),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = if (isDark) 0.dp else 2.dp
                        ),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = if (isDark) {
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
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // 1. Recorrido Calibración
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = displayDist(calibKm),
                                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (isDark) Color.White else Color.Black
                                    )
                                    Text(
                                        text = "${unitLabel.lowercase()} totales",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isDark) Color.Gray else Color.DarkGray
                                    )
                                }

                                // Separator
                                androidx.compose.material3.VerticalDivider(
                                    modifier = Modifier.height(30.dp),
                                    thickness = 1.dp,
                                    color = if (isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.1f)
                                )

                                // 2. Promedio Diario
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    val convertedKpd = if (useKm) kpdValue else kpdValue * 0.621371
                                    Text(
                                        text = String.format(Locale.US, "%.2f", convertedKpd),
                                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (isDark) Color.White else Color.Black
                                    )
                                    Text(
                                        text = "${unitLabel.lowercase()}/día",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isDark) Color.Gray else Color.DarkGray
                                    )
                                }

                                // Separator
                                androidx.compose.material3.VerticalDivider(
                                    modifier = Modifier.height(30.dp),
                                    thickness = 1.dp,
                                    color = if (isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.1f)
                                )

                                // 3. GPS
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.clickable { locationPermissionsState.launchMultiplePermissionRequest() }
                                ) {
                                    Text(
                                        text = "0.0",
                                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (isDark) Color.White else Color.Black
                                    )
                                    Text(
                                        text = if (locationPermissionsState.allPermissionsGranted) "${unitLabel.lowercase()} GPS" else "GPS Inactivo",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (locationPermissionsState.allPermissionsGranted) Color(0xFF4CAF50) else (if (isDark) Color.Gray else Color.DarkGray)
                                    )
                                }
                            }
                        }
                    }
                }
            }
                // 2. CURRENT ODOMETER CARD
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ODÓMETRO ACTUAL",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    // Badge Real vs Estimado
                    val isEstimated = (activeVehicle?.calculatedKpd ?: 0.0) > 0.0
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isEstimated) MaterialTheme.colorScheme.primaryContainer else Color(0xFFE8F5E9))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = if (isEstimated) Icons.Default.AutoAwesome else Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = if (isEstimated) MaterialTheme.colorScheme.primary else Color(0xFF2E7D32),
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = if (isEstimated) "Estimado (IA)" else "Real",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (isEstimated) MaterialTheme.colorScheme.primary else Color(0xFF2E7D32)
                        )
                    }
                }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("odometer_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) MaterialTheme.colorScheme.surfaceContainer else Color.White
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = if (isDark) 0.dp else 2.dp
                ),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = if (isDark) {
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
                        .padding(20.dp)
                ) {
                    val actualKpd = activeVehicle?.calculatedKpd ?: 0.0
                    val baselineKpd = when (activeVehicle?.usageType) {
                        "TRANSPORTE_APPS" -> 150.0
                        "CARGA" -> 200.0
                        "FLOTA_COMERCIAL" -> 120.0
                        else -> 40.0
                    }
                    val kpdToDisplay = if (actualKpd > 0.0) actualKpd else baselineKpd

                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = displayDist(activeVehicle?.odometer ?: 42500.0),
                            style = MaterialTheme.typography.displayMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            fontSize = 44.sp,
                            modifier = Modifier.alignByBaseline()
                        )
                        Text(
                            text = unitLabel.lowercase(),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.alignByBaseline()
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = displayDist(kpdToDisplay),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier.alignByBaseline()
                        )
                        Text(
                            text = " ${unitLabel.lowercase()}/día",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.alignByBaseline()
                        )
                    }
                    Spacer(modifier = Modifier.height(18.dp))
                    FaintMapIllustration()
                }
            }
            }

            // 3. PREDICTIVE HEALTH CARD
            if (!hasServiceLogs) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                    text = "SALUD PREDICTIVA",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp)
                )
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("health_card"),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) MaterialTheme.colorScheme.surfaceContainer else Color.White
                    ),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = if (isDark) 0.dp else 2.dp
                    ),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = if (isDark) {
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
                            .padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column {
                                Text(
                                    text = "Salud General: 100% • Al día",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Success",
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Registra tu primer mantenimiento para iniciar el cálculo de salud predictiva de componentes críticos (aceite, frenos, neumáticos, etc.).",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "SALUD PREDICTIVA",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp)
                )
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("health_card"),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) MaterialTheme.colorScheme.surfaceContainer else Color.White
                    ),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = if (isDark) 0.dp else 2.dp
                    ),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = if (isDark) {
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
                            .padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column {
                                val wearIdx = mostUrgentTask?.wearIndex ?: 0f
                                val statusText = when {
                                    wearIdx >= 0.85f -> "Ventana crítica"
                                    wearIdx >= 0.60f -> "Atención próxima"
                                    else -> "Al día"
                                }
                                Text(
                                    text = "Salud General: ${Math.round(overallHealthPercent)}% • $statusText",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            val wearIdxForIcon = mostUrgentTask?.wearIndex ?: 0f
                            val (iconVector, iconTint) = when {
                                wearIdxForIcon >= 0.85f -> Icons.Default.Warning to MaterialTheme.colorScheme.error
                                wearIdxForIcon >= 0.60f -> Icons.Default.Warning to Color(0xFFFFB300)
                                else -> Icons.Default.CheckCircle to Color(0xFF4CAF50)
                            }
                            Icon(
                                imageVector = iconVector,
                                contentDescription = "Status Icon",
                                tint = iconTint,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Text(
                                text = mostUrgentTask?.category ?: "NA",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${Math.round((mostUrgentTask?.wearIndex ?: 0f) * 100)}%",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = if ((mostUrgentTask?.wearIndex ?: 0f) >= 0.85f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Progress-bar
                        LinearProgressIndicator(
                            progress = { mostUrgentTask?.wearIndex ?: 0f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = if ((mostUrgentTask?.wearIndex ?: 0f) >= 0.85f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        if (mostUrgentTask != null) {
                            val isOverdue = mostUrgentTask.remainingKm <= 0.0 || mostUrgentTask.remainingDays <= 0.0
                            val kpd = if ((activeVehicle?.calculatedKpd ?: 0.0) > 0.0) activeVehicle!!.calculatedKpd else 42.5
                            val daysToLimit = Math.min(
                                Math.max(0.0, mostUrgentTask.remainingKm / kpd),
                                Math.max(0.0, mostUrgentTask.remainingDays)
                            )
                            val estDateMillis = currentTimeMillis + (daysToLimit * 24L * 60 * 60 * 1000).toLong()
                            val sdf = SimpleDateFormat("dd 'de' MMM, yyyy", Locale("es", "ES"))
                            
                            val statusText = if (isOverdue) {
                                "Atención: ${mostUrgentTask.subtitle} vencido (Registrar)"
                            } else {
                                sdf.format(Date(estDateMillis))
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        viewModel.setFormDetails(mostUrgentTask.subtitle)
                                        viewModel.navigateToRegisterService(mostUrgentTask.category)
                                    }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (isOverdue) Icons.Default.Warning else Icons.Default.Info,
                                    contentDescription = null,
                                    tint = if (isOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = statusText,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                    color = if (isOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
            }

            // 4. UPCOMING TASKS CARD
            if (hasServiceLogs) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                    text = "PRÓXIMAS TAREAS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp)
                )
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) MaterialTheme.colorScheme.surfaceContainer else Color.White
                    ),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = if (isDark) 0.dp else 2.dp
                    ),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = if (isDark) {
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
                            .padding(horizontal = 8.dp, vertical = 16.dp)
                    ) {
                        val sortedTasks = computedTasks.sortedByDescending { it.wearIndex }
                        sortedTasks.forEachIndexed { index, task ->
                            if (index > 0) {
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                            
                            val dueText = if (task.remainingKm <= 0.0 || task.remainingDays <= 0.0) {
                                "VENCIDO"
                            } else {
                                if (task.isTimeUrgent) {
                                    val daysText = Math.max(1, Math.ceil(task.remainingDays).toInt())
                                    if (daysText >= 365) {
                                        val years = daysText / 365
                                        "En $years año${if (years > 1) "s" else ""}"
                                    } else {
                                        "En $daysText días"
                                    }
                                } else {
                                    val distText = displayDist(Math.max(0.0, task.remainingKm))
                                    "En $distText $unitLabel"
                                }
                            }

                            TaskItemRow(
                                title = task.category,
                                subtitle = task.subtitle,
                                milestoneText = "${displayDist(task.nextMilestoneKm)} $unitLabel",
                                dueText = dueText,
                                isError = task.wearIndex >= 0.85f || task.remainingKm <= 0.0 || task.remainingDays <= 0.0,
                                icon = task.icon,
                                iconColor = task.iconColor,
                                onClick = {
                                    viewModel.setFormDetails(task.subtitle)
                                    viewModel.navigateToRegisterService(task.category)
                                }
                            )
                        }
                    }
                }
            }
            }

            // 5. CENTRO DE SOPORTE E INFORMACIÓN
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        showSupportDialog = true
                    },
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                    } else {
                        Color(0xFFFFF5F1)
                    }
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = if (isDark) 0.dp else 2.dp
                ),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = if (isDark) {
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            )
                        )
                    } else {
                        SolidColor(MaterialTheme.colorScheme.primary.copy(alpha = 0.45f))
                    }
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SupportAgent,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "¿Necesitas ayuda o soporte?",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Preguntas frecuentes, contacto directo y más",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Ir a Soporte",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            } // Close sub-Column for padded cards
        }
    }

    if (showSupportDialog) {
        SupportCenterDialog(onDismiss = { showSupportDialog = false })
    }
}

@Composable
fun TaskItemRow(
    title: String,
    subtitle: String,
    milestoneText: String,
    dueText: String,
    isError: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.5f))
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = milestoneText,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = dueText.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun TelemetryItem(
    label: String,
    value: String,
    progress: Float,
    color: Color
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.alignByBaseline()
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.alignByBaseline()
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
        )
    }
}

@Composable
fun VehicleIllustration(modifier: Modifier = Modifier) {
    SubcomposeAsyncImage(
        model = com.example.R.drawable.img_default_car_1781706668505,
        contentDescription = "Vehículo",
        contentScale = ContentScale.Fit,
        modifier = modifier
    )
}

@Composable
fun FaintMapIllustration(
    modifier: Modifier = Modifier
) {
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    
    // Exact official Google Maps color palette (light/dark mode variants matching the user's vector image)
    val mapBackground = if (isDark) Color(0xFF1E2124) else Color(0xFFE2E4E9)
    val riverColor = if (isDark) Color(0xFF1E568F) else Color(0xFF3B99FC)
    val parkColor = if (isDark) Color(0xFF2E5E3D) else Color(0xFF91D595)
    val standardRoadColor = if (isDark) Color(0xFF2E323A) else Color(0xFFFFFFFF)
    val standardRoadOutline = if (isDark) Color(0xFF1B1D20) else Color(0xFFD0D6DC)
    
    // Main arterial highway routes (Vibrant orange/gold from the user's vector image)
    val highwayOutline = if (isDark) Color(0xFF5A4414) else Color(0xFFF39F21)
    val highwayFill = if (isDark) Color(0xFF8B6C24) else Color(0xFFFFC043)
    val majorAvenueOutline = if (isDark) Color(0xFF2A2D33) else Color(0xFFC0C6CC)
    val majorAvenueFill = if (isDark) Color(0xFF363B42) else Color(0xFFF5F6F8)
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(mapBackground)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val density = drawContext.density.density
            
            // 1. Draw Park/Greenery Polygons (matched to the new vector shape layout)
            
            // Major Center leaf park
            val centerPark = Path().apply {
                moveTo(width * 0.46f, height * 0.48f)
                cubicTo(
                    width * 0.52f, height * 0.38f,
                    width * 0.70f, height * 0.26f,
                    width * 0.82f, height * 0.28f
                )
                cubicTo(
                    width * 0.85f, height * 0.32f,
                    width * 0.82f, height * 0.50f,
                    width * 0.64f, height * 0.65f
                )
                cubicTo(
                    width * 0.54f, height * 0.70f,
                    width * 0.44f, height * 0.58f,
                    width * 0.46f, height * 0.48f
                )
                close()
            }
            drawPath(path = centerPark, color = parkColor)
            
            // Top middle park
            val parkTop = Path().apply {
                moveTo(width * 0.35f, 0f)
                cubicTo(
                    width * 0.38f, height * 0.16f,
                    width * 0.44f, height * 0.20f,
                    width * 0.47f, 0f
                )
                close()
            }
            drawPath(path = parkTop, color = parkColor)
            
            // Middle Left park
            val parkML = Path().apply {
                moveTo(0f, height * 0.46f)
                cubicTo(
                    width * 0.08f, height * 0.46f,
                    width * 0.12f, height * 0.58f,
                    0f, height * 0.62f
                )
                close()
            }
            drawPath(path = parkML, color = parkColor)
            
            // Bottom center park
            val parkBC = Path().apply {
                moveTo(width * 0.54f, height)
                cubicTo(
                    width * 0.56f, height * 0.85f,
                    width * 0.70f, height * 0.85f,
                    width * 0.72f, height
                )
                close()
            }
            drawPath(path = parkBC, color = parkColor)
            
            // A walking trail inside the main park for premium detail
            val centerParkTrail = Path().apply {
                moveTo(width * 0.50f, height * 0.55f)
                cubicTo(
                    width * 0.58f, height * 0.44f,
                    width * 0.68f, height * 0.46f,
                    width * 0.78f, height * 0.34f
                )
            }
            drawPath(
                path = centerParkTrail,
                color = Color.White.copy(alpha = 0.45f),
                style = Stroke(width = 2.5f * density, cap = StrokeCap.Round)
            )
            
            // 2. Draw Google Maps style winding River/Water bodies (Fluid sweeping curves)
            // Left to center horizontal wave river
            val riverPath = Path().apply {
                moveTo(0f, height * 0.04f)
                cubicTo(
                    width * 0.24f, height * 0.12f,
                    width * 0.18f, height * 0.55f,
                    width * 0.42f, height * 0.62f
                )
                cubicTo(
                    width * 0.58f, height * 0.67f,
                    width * 0.85f, height * 0.40f,
                    width * 1.05f, height * 0.38f
                )
            }
            drawPath(
                path = riverPath,
                color = riverColor,
                style = Stroke(width = 34f * density / 2.7f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
            
            // Bottom Right estuary segment (from the vector image)
            val riverEstuary = Path().apply {
                moveTo(width * 0.76f, height)
                cubicTo(
                    width * 0.80f, height * 0.75f,
                    width * 1.02f, height * 0.42f,
                    width, height * 0.40f
                )
                lineTo(width, height)
                close()
            }
            drawPath(path = riverEstuary, color = riverColor)
            
            // 3. Draw Minor Streets (Thin background urban network grid)
            val streetGrid = listOf(
                Pair(Offset(0f, height * 0.20f), Offset(width, height * 0.16f)),
                Pair(Offset(0f, height * 0.48f), Offset(width, height * 0.45f)),
                Pair(Offset(0f, height * 0.76f), Offset(width, height * 0.82f)),
                
                Pair(Offset(width * 0.12f, 0f), Offset(width * 0.16f, height)),
                Pair(Offset(width * 0.28f, 0f), Offset(width * 0.32f, height)),
                Pair(Offset(width * 0.62f, 0f), Offset(width * 0.66f, height)),
                Pair(Offset(width * 0.78f, 0f), Offset(width * 0.84f, height)),
                
                Pair(Offset(width * 0.45f, 0f), Offset(0f, height * 0.40f)),
                Pair(Offset(width * 0.58f, height), Offset(width, height * 0.30f)),
                Pair(Offset(0f, height * 0.05f), Offset(width * 0.40f, height * 0.45f)),
                Pair(Offset(width * 0.85f, height), Offset(width, height * 0.68f))
            )
            
            streetGrid.forEach { street ->
                // Minor road background outline for depth
                drawLine(
                    color = standardRoadOutline,
                    start = street.first,
                    end = street.second,
                    strokeWidth = 7f * density / 2.7f,
                    cap = StrokeCap.Round
                )
                // Minor road actual surface fill
                drawLine(
                    color = standardRoadColor,
                    start = street.first,
                    end = street.second,
                    strokeWidth = 4.5f * density / 2.7f,
                    cap = StrokeCap.Round
                )
            }
            
            // 4. Draw Major Arterial Avenues (Double lined outlines)
            val majorAvenues = listOf(
                Pair(Offset(0f, height * 0.32f), Offset(width, height * 0.32f)),
                Pair(Offset(width * 0.70f, 0f), Offset(width * 0.70f, height))
            )
            majorAvenues.forEach { avenue ->
                drawLine(
                    color = majorAvenueOutline,
                    start = avenue.first,
                    end = avenue.second,
                    strokeWidth = 14f * density / 2.7f,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = majorAvenueFill,
                    start = avenue.first,
                    end = avenue.second,
                    strokeWidth = 10f * density / 2.7f,
                    cap = StrokeCap.Round
                )
            }
            
            // 5. Draw Primary Golden Highways (Golden Yellow sweeping routes)
            
            // Sweeping diagonal/vertical arterial
            val highwayVertical = Path().apply {
                moveTo(width * 0.48f, 0f)
                cubicTo(
                    width * 0.42f, height * 0.30f,
                    width * 0.41f, height * 0.60f,
                    width * 0.45f, height * 0.80f
                )
                cubicTo(
                    width * 0.47f, height * 0.85f,
                    width * 0.30f, height * 0.90f,
                    0f, height * 0.96f
                )
            }
            
            // Secondary branch splitting at bottom junction
            val highwayBranch = Path().apply {
                moveTo(width * 0.45f, height * 0.80f)
                cubicTo(
                    width * 0.47f, height * 0.86f,
                    width * 0.54f, height * 0.94f,
                    width * 0.56f, height
                )
            }
            
            // Grand sweeping horizontal highway
            val highwayHorizontal = Path().apply {
                moveTo(0f, height * 0.10f)
                lineTo(width * 0.28f, height * 0.30f)
                quadraticTo(
                    width * 0.48f, height * 0.26f,
                    width * 0.80f, height * 0.18f
                )
                cubicTo(
                    width * 0.88f, height * 0.16f,
                    width * 0.94f, height * 0.35f,
                    width, height * 0.35f
                )
            }
            
            val allHighways = listOf(highwayVertical, highwayBranch, highwayHorizontal)
            
            // Draw all highway outlines first for neat intersections
            allHighways.forEach { path ->
                drawPath(
                    path = path,
                    color = highwayOutline,
                    style = Stroke(width = 18f * density / 2.7f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }
            
            // Draw all highway orange-gold fills on top
            allHighways.forEach { path ->
                drawPath(
                    path = path,
                    color = highwayFill,
                    style = Stroke(width = 12f * density / 2.7f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }
        }

        // 6. Floating UI details (Google Maps compass and locate overlay controls to complete the modern hud look!)
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(
                        color = if (isDark) Color(0xFF303134) else Color(0xFFFFFFFF),
                        shape = CircleShape
                    )
                    .shadow(elevation = 1.dp, shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Explore,
                    contentDescription = null,
                    tint = if (isDark) Color(0xFFE8EAED) else Color(0xFF4285F4),
                    modifier = Modifier.size(16.dp)
                )
            }

            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(
                        color = if (isDark) Color(0xFF303134) else Color(0xFFFFFFFF),
                        shape = CircleShape
                    )
                    .shadow(elevation = 1.dp, shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MyLocation,
                    contentDescription = null,
                    tint = Color(0xFF4285F4),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun SupportCenterDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar")
            }
        },
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SupportAgent,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    text = "Centro de Soporte",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Asistencia directa de GaragePulse para el cuidado y mantenimiento preventivo de tu vehículo.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // 1. CANALES DE AYUDA DIRECTA
                Text(
                    text = "CONTACTAR UN ASESOR",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                    color = MaterialTheme.colorScheme.primary
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ContactRow(
                            icon = Icons.Default.Phone,
                            title = "Soporte Telefónico",
                            subtitle = "0800-444-PULSE (7857)",
                            onClick = { /* Simulated intent */ }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        ContactRow(
                            icon = Icons.Default.Email,
                            title = "Atención por Email",
                            subtitle = "soporte@garagepulse.com",
                            onClick = { /* Simulated intent */ }
                        )
                    }
                }

                // 2. PREGUNTAS FRECUENTES
                Text(
                    text = "PREGUNTAS FRECUENTES",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                    color = MaterialTheme.colorScheme.primary
                )

                FaqExpandableSection(
                    question = "¿Cada cuánto debo registrar un servicio?",
                    answer = "Es altamente recomendable registrar cada cambio de aceite (comúnmente cada 5,000 a 10,000 kilómetros o 6 meses), rotación de neumáticos e inspecciones mecánicas periódicas para mantener al día el algoritmo de salud predictiva."
                )

                FaqExpandableSection(
                    question = "¿Cómo se calcula la Salud Predictiva?",
                    answer = "Nuestro sistema analiza el historial acumulado de servicios registrados y la distancia recorrida desde tus eventos previos para calcular el desgaste proyectado de componentes críticos (aceite, frenos y neumáticos), alertándote antes de llegar a la ventana límite de riesgo."
                )

                FaqExpandableSection(
                    question = "¿Cómo actualizo el kilometraje total?",
                    answer = "El kilometraje (odómetro) actual de tu vehículo se actualiza automáticamente al ingresar un nuevo registro de mantenimiento utilizando el botón de agregar (+) en el menú inferior."
                )
            }
        },
        shape = RoundedCornerShape(28.dp),
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = true)
    )
}

@Composable
private fun ContactRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            imageVector = Icons.Default.ArrowForward,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun FaqExpandableSection(
    question: String,
    answer: String
) {
    var expanded by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = question,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            if (expanded) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = answer,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

data class MaintenanceTaskResult(
    val category: String,
    val subtitle: String,
    val nextMilestoneKm: Double,
    val remainingKm: Double,
    val remainingDays: Double,
    val wearIndex: Float,
    val isTimeUrgent: Boolean,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val iconColor: Color
)

@Composable
fun VehiclePhotoOrIllustration(
    vehicle: Vehicle?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit
) {
    if (vehicle == null) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.material3.CircularProgressIndicator(
                strokeWidth = 2.dp,
                modifier = Modifier.size(24.dp),
                color = Color(0xFFE75C31)
            )
        }
        return
    }

    val photoUrl = vehicle.photoUri
    val isMotorcycle = vehicle.type == "Motorcycle"

    var photoModel: Any? = photoUrl

    // Dynamically map the Chery Arauca or its Stitch URL to the correct, beautifully-loaded CDN image URL
    if (photoUrl.isNullOrBlank() && vehicle.name.contains("Arauca", ignoreCase = true)) {
        photoModel = "https://lh3.googleusercontent.com/aida-public/AB6AXuDqhohr5P2dCxFvCtHhd8LekMaLiFz-QggHJfhsrrcVqfFKuZuDDlSbDBlaT3H1rSTzcsCZw--vWptlNLmvJi6IVrQgEsk1tb6vB8aXeCphAqFozzE6J5S3Ez7B-PBMalJpYSdrPKfUdQX8-rmvmtsu9C1yAo3ZvoSH_EcLZBQYUp8_IH0qADqtNCdOUyIeXA9XwU-t5M_YrkMKrGbmae3u7hdQBSjhbvDWI0bgcvy8ZlXWGyM7pkkB4dZ1W5Y2MDYKh58xeg6N_fE"
    } else if (photoUrl != null && (photoUrl.contains("stitch.withgoogle.com") || photoUrl.contains("4476657333708437640"))) {
        photoModel = "https://lh3.googleusercontent.com/aida-public/AB6AXuDqhohr5P2dCxFvCtHhd8LekMaLiFz-QggHJfhsrrcVqfFKuZuDDlSbDBlaT3H1rSTzcsCZw--vWptlNLmvJi6IVrQgEsk1tb6vB8aXeCphAqFozzE6J5S3Ez7B-PBMalJpYSdrPKfUdQX8-rmvmtsu9C1yAo3ZvoSH_EcLZBQYUp8_IH0qADqtNCdOUyIeXA9XwU-t5M_YrkMKrGbmae3u7hdQBSjhbvDWI0bgcvy8ZlXWGyM7pkkB4dZ1W5Y2MDYKh58xeg6N_fE"
    }

    if (vehicle.name.contains("Vespa", ignoreCase = true)) {
        photoModel = com.example.R.drawable.img_vespa_gts_300_1781704971852
    } else if (vehicle.name.contains("Hilux", ignoreCase = true)) {
        photoModel = com.example.R.drawable.img_toyota_hilux_2_8_1781704989578
    }

    val isIllustration = photoModel == null || 
                         photoModel is Int || 
                         (photoModel is String && photoModel.isBlank())

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        val innerModifier = Modifier.fillMaxSize()

        if (photoModel != null) {
            SubcomposeAsyncImage(
                model = photoModel,
                contentDescription = vehicle.name,
                contentScale = contentScale,
                modifier = innerModifier
            ) {
                val state = painter.state
                if (state is AsyncImagePainter.State.Error) {
                    // Return fallback element when load fails
                    if (isMotorcycle) {
                        MotorcycleIllustration(modifier = Modifier.fillMaxSize())
                    } else {
                        VehicleIllustration(modifier = Modifier.fillMaxSize())
                    }
                } else if (state is AsyncImagePainter.State.Loading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                } else {
                    SubcomposeAsyncImageContent()
                }
            }
        } else {
            // Immediate fallback to native Canvas vector if no custom photo
            if (isMotorcycle) {
                MotorcycleIllustration(modifier = innerModifier)
            } else {
                VehicleIllustration(modifier = innerModifier)
            }
        }
    }
}

@Composable
fun MotorcycleIllustration(modifier: Modifier = Modifier) {
    SubcomposeAsyncImage(
        model = com.example.R.drawable.img_default_moto_1781706682868,
        contentDescription = "Moto",
        contentScale = ContentScale.Fit,
        modifier = modifier
    )
}

