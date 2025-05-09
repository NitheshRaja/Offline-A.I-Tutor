@file:OptIn(androidx.compose.animation.ExperimentalAnimationApi::class)
package com.google.mediapipe.examples.llminference

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log // Import Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController // Correct import
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.mediapipe.examples.llminference.ui.theme.LLMInferenceTheme
import androidx.lifecycle.ViewModelProvider
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import androidx.compose.animation.*

// Ensure necessary Route composables are imported (adjust paths if needed)
import com.google.mediapipe.examples.llminference.ChatRoute
import com.google.mediapipe.examples.llminference.LoadingRoute
import com.google.mediapipe.examples.llminference.RoleplaySelectionRoute
import com.google.mediapipe.examples.llminference.SelectionRoute

@OptIn(androidx.compose.animation.ExperimentalAnimationApi::class)
const val START_SCREEN = "start_screen"
const val ROLEPLAY_SELECTION = "roleplay_selection"
const val LOAD_SCREEN = "load_screen"
const val CHAT_SCREEN = "chat_screen"
const val CHAT_HISTORY = "chat_history"
// const val ARG_MODEL_NAME = "modelName" // Argument might not be needed if ChatRoute handles model globally

@OptIn(androidx.compose.animation.ExperimentalAnimationApi::class)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Apply theme preference
        ThemePreferenceManager.applyTheme(ThemePreferenceManager.loadThemePreference(this))

        setContent {
            val context = LocalContext.current
            val isDarkThemeState by remember {
                mutableStateOf(ThemePreferenceManager.isCurrentlyDark(context))
            }
            LaunchedEffect(ThemePreferenceManager.loadThemePreference(context)) {
                // React if needed
            }

            LLMInferenceTheme(darkTheme = isDarkThemeState) {
                val navController = rememberNavController() // Defined here
                val startDestination = intent.getStringExtra("NAVIGATE_TO") ?: START_SCREEN // Defined here
                var currentScreen by remember { mutableStateOf(startDestination) }

                val navBackStackEntry by navController.currentBackStackEntryAsState()
                LaunchedEffect(navBackStackEntry) {
                    currentScreen = navBackStackEntry?.destination?.route ?: startDestination
                }
                
                // --- Define AppBar INSIDE setContent --- 
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

                    // Always show back button on all screens
                    // This is the main change - removed the conditional for showing the back arrow
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
                 // --- End AppBar Definition --- 

                Scaffold(
                    topBar = { 
                        // Call the AppBar defined above
                        AppBar(
                            onBackClick = {
                                if (navController.previousBackStackEntry != null) {
                                    navController.popBackStack()
                                } else {
                                    // If we're at the start destination or no back stack, 
                                    // go to HomeActivity
                                    val intent = Intent(this@MainActivity, HomeActivity::class.java)
                                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                                startActivity(intent)
                                finish()
                            }
                            }
                        )
                    }
                ) { paddingValues ->
                    Box(modifier = Modifier.padding(paddingValues)) {
                        AnimatedNavHost(
                            navController = navController,
                            startDestination = startDestination,
                            enterTransition = {
                                slideInVertically(initialOffsetY = { it }) + fadeIn()
                            },
                            exitTransition = {
                                slideOutVertically(targetOffsetY = { -it }) + fadeOut()
                            },
                            popEnterTransition = {
                                slideInVertically(initialOffsetY = { -it }) + fadeIn()
                            },
                            popExitTransition = {
                                slideOutVertically(targetOffsetY = { it }) + fadeOut()
                            }
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
                                        navController.navigate(LOAD_SCREEN) {
                                            popUpTo(ROLEPLAY_SELECTION) { inclusive = true }
                                            launchSingleTop = true
                                        }
                                    }
                                )
                            }
                            composable(LOAD_SCREEN) {
                                LoadingRoute(
                                    onModelLoaded = {
                                        navController.navigate(CHAT_SCREEN) {
                                            popUpTo(LOAD_SCREEN) { inclusive = true }
                                            launchSingleTop = true
                                        }
                                    },
                                    onGoBack = {
                                        navController.popBackStack()
                                    }
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
                            composable(CHAT_HISTORY) {
                                ChatHistoryScreen(
                                    onBackClick = { navController.popBackStack() }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
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
