package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ServiceLog
import com.example.ui.viewmodel.GarageTab
import com.example.ui.viewmodel.GarageViewModel
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: GarageViewModel,
    modifier: Modifier = Modifier
) {
    val allLogs by viewModel.allServiceLogs.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val isDark by viewModel.isDarkTheme.collectAsState()

    var selectedFilter by remember { mutableStateOf("TODOS") }
    var selectedLogForDetail by remember { mutableStateOf<ServiceLog?>(null) }

    val useKm = userProfile.useKm
    val unitLabel = if (useKm) "km" else "mi"

    // Filtered logs
    val filteredLogs = remember(allLogs, selectedFilter) {
        if (selectedFilter == "TODOS") {
            allLogs
        } else {
            allLogs.filter { it.type.uppercase() == selectedFilter }
        }
    }

    // Group logs by month string "OCTUBRE 2023"
    val groupedLogs = remember(filteredLogs) {
        filteredLogs.groupBy { getMonthHeader(it.date) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = { viewModel.selectTab(GarageTab.DASHBOARD) }
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Atrás",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Text(
                            text = "Historial",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.selectTab(GarageTab.PROFILE) },
                        modifier = Modifier.testTag("history_profile_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Perfil",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.selectTab(GarageTab.ADD) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape,
                modifier = Modifier
                    .padding(bottom = 16.dp, end = 4.dp)
                    .testTag("history_fab_add")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Nuevo Registro",
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Screen Title & Subtitle block
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "Historial",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Seguimiento completo de mantenimiento",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            // Filter Chips Row
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val filters = listOf("TODOS", "PREVENTIVO", "REPARACIONES")
                items(filters) { filter ->
                    val isSelected = selectedFilter == filter
                    val chipContainerColor = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    }
                    val chipContentColor = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .background(chipContainerColor)
                            .clickable { selectedFilter = filter }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .testTag("filter_chip_$filter"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = filter,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = chipContentColor
                        )
                    }
                }
            }

            // Empty State view if no matches found
            if (groupedLogs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SearchOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            modifier = Modifier.size(64.dp)
                        )
                        Text(
                            text = "No se encontraron registros",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Por favor agrega un mantenimiento haciendo clic en el botón '+'",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            } else {
                // Service Logs chronologic timeline
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    groupedLogs.forEach { (monthHeader, logsList) ->
                        // Month Category Header
                        item {
                            Text(
                                text = monthHeader,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(start = 2.dp, top = 8.dp, bottom = 4.dp)
                            )
                        }

                        // Collection card containing all logs for this month
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isDark) MaterialTheme.colorScheme.surfaceContainerLowest else Color.White
                                ),
                                elevation = CardDefaults.cardElevation(
                                    defaultElevation = if (isDark) 0.dp else 2.dp
                                ),
                                border = CardDefaults.outlinedCardBorder().copy(
                                    brush = if (isDark) {
                                        Brush.linearGradient(
                                            colors = listOf(
                                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.05f)
                                            )
                                        )
                                    } else {
                                        SolidColor(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f))
                                    }
                                )
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    logsList.forEachIndexed { index, log ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { selectedLogForDetail = log }
                                                .padding(horizontal = 16.dp, vertical = 14.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Circular colored avatar with specific Category Icon
                                            val containerColor = when (log.category) {
                                                "Cambio de Aceite" -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                                                "Neumáticos" -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.15f)
                                                "Frenos" -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.15f)
                                                "Motor" -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                                                "Suspensión" -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.15f)
                                                else -> MaterialTheme.colorScheme.surfaceVariant
                                            }
                                            val iconColor = when (log.category) {
                                                "Cambio de Aceite" -> MaterialTheme.colorScheme.primary
                                                "Neumáticos" -> MaterialTheme.colorScheme.secondary
                                                "Frenos" -> MaterialTheme.colorScheme.tertiary
                                                "Motor" -> MaterialTheme.colorScheme.primary
                                                "Suspensión" -> MaterialTheme.colorScheme.secondary
                                                else -> MaterialTheme.colorScheme.outline
                                            }
                                            val iconVector = when (log.category) {
                                                "Cambio de Aceite" -> Icons.Default.OilBarrel
                                                "Neumáticos" -> Icons.Default.TireRepair
                                                "Frenos" -> Icons.Default.Build
                                                "Motor" -> Icons.Default.MinorCrash
                                                "Suspensión" -> Icons.Default.Speed
                                                else -> Icons.Default.DirectionsCar
                                            }

                                            Box(
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .clip(CircleShape)
                                                    .background(containerColor),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = iconVector,
                                                    contentDescription = null,
                                                    tint = iconColor,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }

                                            Spacer(modifier = Modifier.width(16.dp))

                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                                        Text(
                                                            text = log.title,
                                                            style = MaterialTheme.typography.titleLarge.copy(
                                                                fontWeight = FontWeight.Bold,
                                                                fontSize = 16.sp
                                                            ),
                                                            color = MaterialTheme.colorScheme.onSurface,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                        Spacer(modifier = Modifier.height(4.dp))
                                                        Text(
                                                            text = "${formatDateString(log.date)} • ${log.description}",
                                                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                                                            color = MaterialTheme.colorScheme.outline,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                    Column(horizontalAlignment = Alignment.End) {
                                                        Text(
                                                            text = "$${String.format(Locale.US, "%.2f", log.cost)}",
                                                            style = MaterialTheme.typography.labelMedium.copy(
                                                                fontWeight = FontWeight.Bold,
                                                                fontSize = 14.sp
                                                            ),
                                                            color = MaterialTheme.colorScheme.onSurface
                                                        )
                                                        Spacer(modifier = Modifier.height(4.dp))
                                                        Text(
                                                            text = "${displayDistValue(log.mileage, useKm)} $unitLabel",
                                                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                                                            color = MaterialTheme.colorScheme.outline
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        // Divider line (not spanning full width - inset left)
                                        if (index < logsList.size - 1) {
                                            Divider(
                                                modifier = Modifier.padding(start = 72.dp),
                                                thickness = 0.5.dp,
                                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        selectedLogForDetail?.let { log ->
            ServiceDetailDialog(
                log = log,
                useKm = useKm,
                unitLabel = unitLabel,
                onDismiss = { selectedLogForDetail = null },
                onDelete = { viewModel.deleteServiceLog(log) }
            )
        }
    }
}



// FORMAT TIME HELPERS
fun getMonthHeader(timestamp: Long): String {
    val cal = Calendar.getInstance()
    cal.timeInMillis = timestamp
    val monthStr = when (cal.get(Calendar.MONTH)) {
        Calendar.JANUARY -> "ENERO"
        Calendar.FEBRUARY -> "FEBRERO"
        Calendar.MARCH -> "MARZO"
        Calendar.APRIL -> "ABRIL"
        Calendar.MAY -> "MAYO"
        Calendar.JUNE -> "JUNIO"
        Calendar.JULY -> "JULIO"
        Calendar.AUGUST -> "AGOSTO"
        Calendar.SEPTEMBER -> "SEPTIEMBRE"
        Calendar.OCTOBER -> "OCTUBRE"
        Calendar.NOVEMBER -> "NOVIEMBRE"
        Calendar.DECEMBER -> "DICIEMBRE"
        else -> "OTROS"
    }
    return "$monthStr ${cal.get(Calendar.YEAR)}"
}

fun formatDateString(timestamp: Long): String {
    val cal = Calendar.getInstance()
    cal.timeInMillis = timestamp
    val day = cal.get(Calendar.DAY_OF_MONTH)
    val monthShort = when (cal.get(Calendar.MONTH)) {
        Calendar.JANUARY -> "Ene"
        Calendar.FEBRUARY -> "Feb"
        Calendar.MARCH -> "Mar"
        Calendar.APRIL -> "Abr"
        Calendar.MAY -> "May"
        Calendar.JUNE -> "Jun"
        Calendar.JULY -> "Jul"
        Calendar.AUGUST -> "Ago"
        Calendar.SEPTEMBER -> "Sep"
        Calendar.OCTOBER -> "Oct"
        Calendar.NOVEMBER -> "Nov"
        Calendar.DECEMBER -> "Dic"
        else -> "Otros"
    }
    return "$day $monthShort"
}

fun displayDistValue(distKm: Double, useKm: Boolean): String {
    val converted = if (useKm) distKm else distKm * 0.621371
    val formatter = NumberFormat.getNumberInstance(Locale.US)
    formatter.maximumFractionDigits = 0
    return formatter.format(converted)
}

@Composable
fun ServiceDetailDialog(
    log: ServiceLog,
    useKm: Boolean,
    unitLabel: String,
    onDismiss: () -> Unit,
    onDelete: (() -> Unit)? = null
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("dismiss_detail_dialog_button")
            ) {
                Text("Cerrar", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = onDelete?.let {
            {
                TextButton(
                    onClick = {
                        it()
                        onDismiss()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("delete_detail_dialog_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Eliminar")
                }
            }
        },
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val containerColor = when (log.category) {
                    "Cambio de Aceite" -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                    "Neumáticos" -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.15f)
                    "Frenos" -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.15f)
                    "Motor" -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                    "Suspensión" -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.15f)
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
                val iconColor = when (log.category) {
                    "Cambio de Aceite" -> MaterialTheme.colorScheme.primary
                    "Neumáticos" -> MaterialTheme.colorScheme.secondary
                    "Frenos" -> MaterialTheme.colorScheme.tertiary
                    "Motor" -> MaterialTheme.colorScheme.primary
                    "Suspensión" -> MaterialTheme.colorScheme.secondary
                    else -> MaterialTheme.colorScheme.outline
                }
                val iconVector = when (log.category) {
                    "Cambio de Aceite" -> Icons.Default.OilBarrel
                    "Neumáticos" -> Icons.Default.TireRepair
                    "Frenos" -> Icons.Default.Build
                    "Motor" -> Icons.Default.MinorCrash
                    "Suspensión" -> Icons.Default.Speed
                    else -> Icons.Default.DirectionsCar
                }
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(containerColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = iconVector,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = "Detalles de Servicio",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Main Info Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = log.title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Divider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                            thickness = 1.dp
                        )
                        
                        DetailRow(label = "Categoría", value = log.category)
                        DetailRow(label = "Tipo", value = log.type.uppercase())
                        val cal = Calendar.getInstance().apply { timeInMillis = log.date }
                        val fullDateStr = "${cal.get(Calendar.DAY_OF_MONTH)} de ${
                            when (cal.get(Calendar.MONTH)) {
                                Calendar.JANUARY -> "Enero"
                                Calendar.FEBRUARY -> "Febrero"
                                Calendar.MARCH -> "Marzo"
                                Calendar.APRIL -> "Abril"
                                Calendar.MAY -> "Mayo"
                                Calendar.JUNE -> "Junio"
                                Calendar.JULY -> "Julio"
                                Calendar.AUGUST -> "Agosto"
                                Calendar.SEPTEMBER -> "Septiembre"
                                Calendar.OCTOBER -> "Octubre"
                                Calendar.NOVEMBER -> "Noviembre"
                                Calendar.DECEMBER -> "Diciembre"
                                else -> "Otros"
                            }
                        } del ${cal.get(Calendar.YEAR)}"
                        DetailRow(label = "Fecha", value = fullDateStr)
                        DetailRow(label = "Kilometraje", value = "${displayDistValue(log.mileage, useKm)} $unitLabel")
                        DetailRow(
                            label = "Costo",
                            value = "$${String.format(Locale.US, "%.2f", log.cost)}",
                            valueColor = MaterialTheme.colorScheme.primary,
                            valueBold = true
                        )
                    }
                }

                // Description Block
                if (log.description.isNotBlank()) {
                    Text(
                        text = "DESCRIPCIÓN DEL MANTENIMIENTO",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = log.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
            }
        },
        shape = RoundedCornerShape(24.dp),
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = true)
    )
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    valueBold: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = if (valueBold) FontWeight.Bold else FontWeight.Medium
            ),
            color = valueColor
        )
    }
}
