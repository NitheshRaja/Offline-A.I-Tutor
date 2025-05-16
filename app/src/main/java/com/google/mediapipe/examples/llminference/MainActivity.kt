@file:OptIn(androidx.compose.animation.ExperimentalAnimationApi::class)
package com.google.mediapipe.examples.llminference

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import com.google.mediapipe.examples.llminference.ui.theme.LLMInferenceTheme
import android.os.Build
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.lifecycle.ViewModelProvider

@OptIn(androidx.compose.animation.ExperimentalAnimationApi::class)
const val START_SCREEN = "start_screen"
const val ROLEPLAY_SELECTION = "roleplay_selection"
const val LOAD_SCREEN = "load_screen"
const val CHAT_SCREEN = "chat_screen"
const val CHAT_HISTORY = "chat_history"

@OptIn(androidx.compose.animation.ExperimentalAnimationApi::class)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val context = LocalContext.current
            var themePref by remember { mutableStateOf(ThemePreferenceManager.loadThemePreference(context)) }
            val isDarkTheme = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                true
            } else {
                when (themePref) {
                    ThemePreferenceManager.THEME_DARK -> true
                    ThemePreferenceManager.THEME_LIGHT -> false
                    else -> isSystemInDarkTheme()
                }
            }

            LLMInferenceTheme(darkTheme = isDarkTheme) {
                val navController = rememberNavController()
                val startDestination = intent.getStringExtra("NAVIGATE_TO") ?: START_SCREEN
                var currentScreen by remember { mutableStateOf(startDestination) }
                val navBackStackEntry by navController.currentBackStackEntryAsState()

                LaunchedEffect(navBackStackEntry) {
                    currentScreen = navBackStackEntry?.destination?.route ?: startDestination
                }

                @OptIn(ExperimentalMaterial3Api::class)
                @Composable
                fun AppBar(onBackClick: () -> Unit) {
                    val title = when (currentScreen) {
                        START_SCREEN -> stringResource(R.string.start_selection_title)
                        ROLEPLAY_SELECTION -> stringResource(R.string.roleplay_selection_title)
                        LOAD_SCREEN -> stringResource(R.string.loading_title)
                        CHAT_HISTORY -> stringResource(R.string.chat_history_title)
                        CHAT_SCREEN -> stringResource(R.string.chat_title)
                        else -> "Offline AI Tutor"
                    }
                    TopAppBar(
                        title = { Text(title) },
                        navigationIcon = {
                            IconButton(onClick = onBackClick) {
                                Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.back_button))
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            titleContentColor = MaterialTheme.colorScheme.onPrimary,
                            navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }

                Scaffold(
                    topBar = {
                        AppBar(onBackClick = {
                            if (navController.previousBackStackEntry != null) {
                                navController.popBackStack()
                            } else {
                                val intent = Intent(this@MainActivity, HomeActivity::class.java)
                                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                                startActivity(intent)
                                finish()
                            }
                        })
                    }
                ) { paddingValues ->
                    Box(modifier = Modifier.padding(paddingValues)) {
                        AnimatedNavHost(
                            navController = navController,
                            startDestination = startDestination,
                            enterTransition = { slideInVertically(initialOffsetY = { it }) + fadeIn() },
                            exitTransition = { slideOutVertically(targetOffsetY = { -it }) + fadeOut() },
                            popEnterTransition = { slideInVertically(initialOffsetY = { -it }) + fadeIn() },
                            popExitTransition = { slideOutVertically(targetOffsetY = { it }) + fadeOut() }
                        ) {
                            composable(START_SCREEN) {
                                SelectionRoute(
                                    onModelSelected = {
                                        navController.navigate(LOAD_SCREEN) {
                                            popUpTo(START_SCREEN) { inclusive = true }
                                            launchSingleTop = true
                                        }
                                    }
                                )
                            }

                            composable(ROLEPLAY_SELECTION) {
                                RoleplaySelectionRoute(
                                    onScenarioSelected = { scenario ->
                                        navController.navigate("$LOAD_SCREEN/${scenario.name}") {
                                            popUpTo(ROLEPLAY_SELECTION) { inclusive = true }
                                            launchSingleTop = true
                                        }
                                    }
                                )
                            }

                            // Plain loading route for model-only
                            composable(LOAD_SCREEN) {
                                LoadingRoute(
                                    onModelLoaded = {
                                        navController.navigate(CHAT_SCREEN) {
                                            popUpTo(LOAD_SCREEN) { inclusive = true }
                                            launchSingleTop = true
                                        }
                                    },
                                    onGoBack = { navController.popBackStack() }
                                )
                            }

                            // Parameterized loading route for roleplay
                            composable(
                                "$LOAD_SCREEN/{scenarioType}",
                                arguments = listOf(navArgument("scenarioType") { type = NavType.StringType })
                            ) { backStackEntry ->
                                val scenarioType = backStackEntry.arguments?.getString("scenarioType")
                                val scenario = scenarioType?.let {
                                    try { RoleplayScenario.valueOf(it) } catch (_: IllegalArgumentException) { null }
                                }
                                LoadingRoute(
                                    onModelLoaded = {
                                        navController.navigate("$CHAT_SCREEN/$scenarioType") {
                                            popUpTo(LOAD_SCREEN) { inclusive = true }
                                            launchSingleTop = true
                                        }
                                    },
                                    onGoBack = { navController.popBackStack() }
                                )
                            }

                            composable(CHAT_SCREEN) {
                                ChatRoute(
                                    onClose = {
                                        navController.navigate(START_SCREEN) {
                                            popUpTo(CHAT_SCREEN) { inclusive = true }
                                            launchSingleTop = true
                                        }
                                    }
                                )
                            }

                            composable(
                                "$CHAT_SCREEN/{scenarioType}",
                                arguments = listOf(navArgument("scenarioType") { type = NavType.StringType })
                            ) { backStackEntry ->
                                val scenarioType = backStackEntry.arguments?.getString("scenarioType")
                                val scenario = scenarioType?.let { RoleplayScenario.valueOf(it) }
                                ChatRoute(
                                    onClose = {
                                        navController.navigate(START_SCREEN) {
                                            popUpTo(CHAT_SCREEN) { inclusive = true }
                                            launchSingleTop = true
                                        }
                                    },
                                    selectedScenario = scenario
                                )
                            }

                            composable(CHAT_HISTORY) {
                                ChatHistoryScreen(onBackClick = { navController.popBackStack() })
                            }

                            composable("home") {
                                HomeScreen(
                                    isDarkTheme = isDarkTheme,
                                    onThemeChange = { dark ->
                                        val newPref = if (dark) ThemePreferenceManager.THEME_DARK else ThemePreferenceManager.THEME_LIGHT
                                        ThemePreferenceManager.saveThemePreference(context, newPref)
                                        themePref = newPref
                                    },
                                    onLogout = { /* stub */ }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PermissionHandler.RECORD_AUDIO_PERMISSION_REQUEST) {
            val granted = grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
            try {
                val factory = ChatViewModel.getFactory(this)
                val chatViewModel = ViewModelProvider(this, factory)[ChatViewModel::class.java]
                chatViewModel.onPermissionResult(granted)
            } catch (e: IllegalStateException) {
                Log.e("MainActivity", "ViewModel not available for permission result", e)
            }
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(0, R.anim.scale_out)
    }
}
