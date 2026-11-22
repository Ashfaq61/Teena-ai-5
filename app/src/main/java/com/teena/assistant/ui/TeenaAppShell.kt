package com.teena.assistant.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.StopCircle
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.teena.assistant.BuildConfig
import com.teena.assistant.ui.navigation.DestinationGroup
import com.teena.assistant.ui.navigation.TeenaDestination
import com.teena.assistant.ui.screens.AboutScreen
import com.teena.assistant.ui.screens.ApiManagerScreen
import com.teena.assistant.ui.screens.ApkBuildScreen
import com.teena.assistant.ui.screens.ChatScreen
import com.teena.assistant.ui.screens.DashboardScreen
import com.teena.assistant.ui.screens.LiveVoiceScreen
import com.teena.assistant.ui.screens.MemoryHistoryScreen
import com.teena.assistant.ui.screens.OnboardingScreen
import com.teena.assistant.ui.screens.PermissionsScreen
import com.teena.assistant.ui.screens.SectionStatusScreen
import com.teena.assistant.ui.screens.SettingsScreen
import com.teena.assistant.ui.theme.TeenaColors
import com.teena.assistant.ui.viewmodel.DashboardViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeenaAppShell(
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    if (!uiState.onboardingCompleted) {
        OnboardingScreen(
            uiState = uiState,
            onRefreshPermissions = viewModel::refreshDevicePermissions,
            onOpenBatteryOptimization = { viewModel.openBatteryOptimizationSettings() },
            onOpenColorOsAutoStart = { viewModel.openColorOsAutoStart() },
            onOpenAppDetails = { viewModel.openAppDetailsSettings() },
            onCompleteOnboarding = viewModel::completeOnboarding
        )
        return
    }

    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = TeenaDestination.fromRoute(navBackStackEntry?.destination?.route)

    fun navigateTo(destination: TeenaDestination) {
        navController.navigate(destination.route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = TeenaColors.Surface,
                drawerContentColor = TeenaColors.TextPrimary
            ) {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "TEENA AI",
                            style = MaterialTheme.typography.headlineMedium,
                            color = TeenaColors.Cyan
                        )
                        Text(
                            text = "${BuildConfig.APPLICATION_ID} • v${BuildConfig.VERSION_NAME}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TeenaColors.TextSecondary
                        )
                    }
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = TeenaColors.Border
                    )

                    DestinationGroup.entries.forEach { group ->
                        Text(
                            text = group.title.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = TeenaColors.TextSecondary,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                        )
                        TeenaDestination.entries
                            .filter { it.group == group }
                            .forEach { destination ->
                                NavigationDrawerItem(
                                    label = {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(destination.title)
                                            Text(
                                                text = "P${destination.targetPhase}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = TeenaColors.TextSecondary
                                            )
                                        }
                                    },
                                    selected = currentDestination == destination,
                                    onClick = {
                                        scope.launch { drawerState.close() }
                                        navigateTo(destination)
                                    },
                                    icon = {
                                        Icon(
                                            imageVector = destination.icon,
                                            contentDescription = destination.title
                                        )
                                    },
                                    colors = NavigationDrawerItemDefaults.colors(
                                        selectedContainerColor = TeenaColors.SurfaceElevated,
                                        selectedIconColor = TeenaColors.Cyan,
                                        selectedTextColor = TeenaColors.TextPrimary,
                                        unselectedIconColor = TeenaColors.TextSecondary,
                                        unselectedTextColor = TeenaColors.TextSecondary
                                    ),
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                                )
                            }
                    }
                }
            }
        }
    ) {
        Scaffold(
            containerColor = TeenaColors.Void,
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = currentDestination.title,
                                style = MaterialTheme.typography.titleLarge,
                                color = TeenaColors.TextPrimary
                            )
                            Text(
                                text = if (uiState.emergencyStopped) {
                                    "EMERGENCY STOP ENGAGED"
                                } else {
                                    "Phase 1–4 Build • ${uiState.connectedProviderCount} provider(s)"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = if (uiState.emergencyStopped) TeenaColors.Rose else TeenaColors.Cyan
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(
                                imageVector = Icons.Rounded.Menu,
                                contentDescription = "Open navigation menu",
                                tint = TeenaColors.TextPrimary
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                if (uiState.emergencyStopped) {
                                    viewModel.clearEmergencyStop()
                                } else {
                                    viewModel.triggerEmergencyStop()
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.StopCircle,
                                contentDescription = if (uiState.emergencyStopped) "Clear Emergency Stop" else "Emergency Stop",
                                tint = if (uiState.emergencyStopped) TeenaColors.Amber else TeenaColors.Rose
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = TeenaColors.Surface
                    )
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = TeenaColors.Surface
                ) {
                    TeenaDestination.bottomNavDestinations.forEach { destination ->
                        NavigationBarItem(
                            selected = currentDestination == destination,
                            onClick = { navigateTo(destination) },
                            icon = {
                                Icon(
                                    imageVector = destination.icon,
                                    contentDescription = destination.title
                                )
                            },
                            label = {
                                Text(
                                    text = destination.shortTitle,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = TeenaColors.Cyan,
                                selectedTextColor = TeenaColors.Cyan,
                                indicatorColor = TeenaColors.SurfaceElevated,
                                unselectedIconColor = TeenaColors.TextSecondary,
                                unselectedTextColor = TeenaColors.TextSecondary
                            )
                        )
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = TeenaDestination.DASHBOARD.route,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                TeenaDestination.entries.forEach { destination ->
                    composable(destination.route) {
                        when (destination) {
                            TeenaDestination.DASHBOARD -> DashboardScreen(
                                uiState = uiState,
                                onNavigate = ::navigateTo,
                                onEmergencyStop = viewModel::triggerEmergencyStop,
                                onClearEmergencyStop = viewModel::clearEmergencyStop
                            )
                            TeenaDestination.CHAT -> ChatScreen()
                            TeenaDestination.LIVE_VOICE -> LiveVoiceScreen()
                            TeenaDestination.API_MANAGER -> ApiManagerScreen()
                            TeenaDestination.MEMORY_HISTORY -> MemoryHistoryScreen()
                            TeenaDestination.PERMISSIONS -> PermissionsScreen(
                                uiState = uiState,
                                onRefreshPermissions = viewModel::refreshDevicePermissions,
                                onOpenAppDetails = { viewModel.openAppDetailsSettings() },
                                onOpenBatteryOptimization = { viewModel.openBatteryOptimizationSettings() },
                                onOpenColorOsAutoStart = { viewModel.openColorOsAutoStart() },
                                onOpenAccessibilitySettings = { viewModel.openAccessibilitySettings() },
                                onOpenNotificationListenerSettings = { viewModel.openNotificationListenerSettings() }
                            )
                            TeenaDestination.SETTINGS -> SettingsScreen(
                                uiState = uiState,
                                onEmergencyStop = viewModel::triggerEmergencyStop,
                                onClearEmergencyStop = viewModel::clearEmergencyStop,
                                onReopenOnboarding = viewModel::reopenOnboarding,
                                onOpenPermissions = { navigateTo(TeenaDestination.PERMISSIONS) },
                                onOpenApiManager = { navigateTo(TeenaDestination.API_MANAGER) },
                                onOpenBatteryOptimization = { viewModel.openBatteryOptimizationSettings() },
                                onOpenColorOsAutoStart = { viewModel.openColorOsAutoStart() }
                            )
                            TeenaDestination.ABOUT -> AboutScreen(uiState = uiState)
                            TeenaDestination.APK_BUILD -> ApkBuildScreen(uiState = uiState)
                            else -> SectionStatusScreen(
                                destination = destination,
                                uiState = uiState
                            )
                        }
                    }
                }
            }
        }
    }
}
