package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.foundation.clickable
import androidx.compose.runtime.LaunchedEffect
import android.Manifest
import android.os.Build
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.local.AppDatabase
import com.example.data.repository.GarageRepository
import com.example.ui.screens.AddServiceScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.AuthFlowContainer
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.GarageTab
import com.example.ui.viewmodel.GarageViewModel
import com.example.ui.viewmodel.GarageViewModelFactory
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.worker.OdometerUpdateWorker
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {

    // Setup local Room database and MVVM repository
    private val database by lazy { AppDatabase.getDatabase(this) }
    private val repository by lazy { GarageRepository(database.databaseDao()) }

    // Spawn GarageViewModel using custom Factory supplying context
    private val viewModel: GarageViewModel by viewModels {
        GarageViewModelFactory(repository, applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Setup strict Edge-to-Edge full notch-safe content flow
        enableEdgeToEdge()

        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.hide(WindowInsetsCompat.Type.navigationBars())
        insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        
        // Enqueue background predictive maintenance worker (runs once a day)
        val workRequest = PeriodicWorkRequestBuilder<OdometerUpdateWorker>(1, TimeUnit.DAYS).build()
        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            "OdometerUpdateWork",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
        
        setContent {
            val isDarkTheme by viewModel.isDarkTheme.collectAsState()
            val isLoggedIn by viewModel.isLoggedIn.collectAsState()
            val showSplash by viewModel.showSplash.collectAsState()

            MyApplicationTheme(darkTheme = isDarkTheme) {
                if (showSplash || !isLoggedIn) {
                    AuthFlowContainer(
                        viewModel = viewModel,
                        onAuthSuccess = { /* Ready */ }
                    )
                } else {
                    MainAppContainer(viewModel = viewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MainAppContainer(viewModel: GarageViewModel) {
    val currentTab by viewModel.currentTab.collectAsState()
    val profile by viewModel.userProfile.collectAsState()

    val permissionsToRequest = mutableListOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
    }

    val multiplePermissionsState = rememberMultiplePermissionsState(permissionsToRequest)

    LaunchedEffect(Unit) {
        if (!multiplePermissionsState.allPermissionsGranted) {
            multiplePermissionsState.launchMultiplePermissionRequest()
        }
    }

    LaunchedEffect(multiplePermissionsState.allPermissionsGranted, profile.isPremium) {
        if (multiplePermissionsState.allPermissionsGranted && profile.isPremium) {
            // Ensure service is running if permissions are granted and feature is enabled
            viewModel.togglePremiumTelemetry(true)
        }
    }

    Scaffold(
        bottomBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("app_bottom_nav_bar"),
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val tabs = listOf(
                        GarageTab.DASHBOARD to Triple("Inicio", Icons.Default.Home, Icons.Outlined.Home),
                        GarageTab.ADD to Triple("Añadir", Icons.Default.AddCircle, Icons.Outlined.AddCircleOutline),
                        GarageTab.HISTORY to Triple("Historial", Icons.Default.History, Icons.Outlined.History),
                        GarageTab.PROFILE to Triple("Perfil", Icons.Default.Person, Icons.Outlined.PersonOutline)
                    )

                    tabs.forEach { (tab, info) ->
                        val (label, iconSelected, iconUnselected) = info
                        val selected = currentTab == tab
                        val color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable { viewModel.selectTab(tab) },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = if (selected) iconSelected else iconUnselected,
                                    contentDescription = label,
                                    tint = color,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = color
                                )
                            }
                        }
                    }
                }
            }
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        // Render corresponding screen with edge-to-edge padding
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (currentTab) {
                GarageTab.DASHBOARD -> DashboardScreen(
                    viewModel = viewModel,
                    modifier = Modifier
                        .padding(bottom = innerPadding.calculateBottomPadding())
                        .consumeWindowInsets(innerPadding)
                )
                GarageTab.HISTORY -> HistoryScreen(
                    viewModel = viewModel,
                    modifier = Modifier
                        .padding(bottom = innerPadding.calculateBottomPadding())
                        .consumeWindowInsets(innerPadding)
                )
                GarageTab.ADD -> AddServiceScreen(
                    viewModel = viewModel,
                    modifier = Modifier
                        .padding(bottom = innerPadding.calculateBottomPadding())
                        .consumeWindowInsets(innerPadding)
                )
                GarageTab.PROFILE -> ProfileScreen(
                    viewModel = viewModel,
                    modifier = Modifier
                        .padding(bottom = innerPadding.calculateBottomPadding())
                        .consumeWindowInsets(innerPadding)
                )
            }
        }
    }
}
