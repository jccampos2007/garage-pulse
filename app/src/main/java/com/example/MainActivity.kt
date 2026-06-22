package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
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

@Composable
fun MainAppContainer(viewModel: GarageViewModel) {
    val currentTab by viewModel.currentTab.collectAsState()

    Scaffold(
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(68.dp)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("app_bottom_nav_bar"),
                containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                tonalElevation = 8.dp
            ) {
                 // TAB 1: DASHBOARD
                NavigationBarItem(
                    selected = currentTab == GarageTab.DASHBOARD,
                    onClick = { viewModel.selectTab(GarageTab.DASHBOARD) },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == GarageTab.DASHBOARD) Icons.Default.Home else Icons.Outlined.Home,
                            contentDescription = "Inicio"
                        )
                    },
                    label = { Text("Inicio", style = MaterialTheme.typography.labelSmall) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = Color.Transparent
                    ),
                    modifier = Modifier.testTag("nav_tab_dashboard")
                )

                // TAB 2: ADD
                NavigationBarItem(
                    selected = currentTab == GarageTab.ADD,
                    onClick = { viewModel.selectTab(GarageTab.ADD) },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == GarageTab.ADD) Icons.Default.AddCircle else Icons.Outlined.AddCircleOutline,
                            contentDescription = "Añadir"
                        )
                    },
                    label = { Text("Añadir", style = MaterialTheme.typography.labelSmall) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = Color.Transparent
                    ),
                    modifier = Modifier.testTag("nav_tab_add")
                )

                // TAB 3: HISTORY
                NavigationBarItem(
                    selected = currentTab == GarageTab.HISTORY,
                    onClick = { viewModel.selectTab(GarageTab.HISTORY) },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == GarageTab.HISTORY) Icons.Default.History else Icons.Outlined.History,
                            contentDescription = "Historial"
                        )
                    },
                    label = { Text("Historial", style = MaterialTheme.typography.labelSmall) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = Color.Transparent
                    ),
                    modifier = Modifier.testTag("nav_tab_history")
                )

                // TAB 4: PROFILE
                NavigationBarItem(
                    selected = currentTab == GarageTab.PROFILE,
                    onClick = { viewModel.selectTab(GarageTab.PROFILE) },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == GarageTab.PROFILE) Icons.Default.Person else Icons.Outlined.PersonOutline,
                            contentDescription = "Perfil"
                        )
                    },
                    label = { Text("Perfil", style = MaterialTheme.typography.labelSmall) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = Color.Transparent
                    ),
                    modifier = Modifier.testTag("nav_tab_profile")
                )
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
