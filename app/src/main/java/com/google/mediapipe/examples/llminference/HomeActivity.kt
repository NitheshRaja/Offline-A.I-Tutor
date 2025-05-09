package com.google.mediapipe.examples.llminference

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.mediapipe.examples.llminference.auth.AuthActivity
import com.google.mediapipe.examples.llminference.ui.theme.LLMInferenceTheme
import com.google.mediapipe.examples.llminference.ui.theme.DarkColorScheme
import com.google.mediapipe.examples.llminference.ui.theme.LightColorScheme
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import kotlinx.coroutines.delay
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import com.google.mediapipe.examples.llminference.SelectionCard
import androidx.compose.ui.graphics.Shape
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.times
import androidx.compose.ui.zIndex

// Navigation routes
private object Routes {
    const val HOME = "home"
    const val ROLEPLAY_SELECTION = "roleplay_selection"
    const val PROFILE = "profile"
}

@OptIn(ExperimentalMaterial3Api::class)
class HomeActivity : ComponentActivity() {
    private lateinit var textFileStorage: TextFileStorage
    private lateinit var authStateListener: FirebaseAuth.AuthStateListener
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Apply the saved theme preference before setting content
        ThemePreferenceManager.applyTheme(ThemePreferenceManager.loadThemePreference(this))
        
        // Initialize TextFileStorage
        textFileStorage = TextFileStorage(this)
        
        // Set up the authentication state listener
        authStateListener = FirebaseAuth.AuthStateListener { auth ->
            val isLoggedIn = auth.currentUser != null
            Log.d("HomeActivity", "Auth state changed: user ${if (isLoggedIn) "logged in" else "logged out"}")
            
            if (isLoggedIn) {
                // User logged in - force a sync of Firebase data
                textFileStorage.handleAuthStateChange(true)
            } else {
                // User logged out - clear cache and redirect to auth
                textFileStorage.handleAuthStateChange(false)
                startAuthActivity()
                finish()
                return@AuthStateListener
            }
        }
        
        // Check if user is logged in
        if (FirebaseAuth.getInstance().currentUser == null) {
            startAuthActivity()
            return
        }
        
        // Force a Firebase sync when the activity starts
        textFileStorage.forceFirebaseSync()
        
        setContent {
            // Use the ThemePreferenceManager to determine the current theme state
            val currentThemePreference = ThemePreferenceManager.loadThemePreference(LocalContext.current)
            var isDarkThemeState by remember { mutableStateOf(ThemePreferenceManager.isCurrentlyDark(this)) }

            // Update the state if the preference changes externally (e.g., system setting)
            LaunchedEffect(currentThemePreference) {
                isDarkThemeState = ThemePreferenceManager.isCurrentlyDark(this@HomeActivity)
            }
            
            val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
            val scope = rememberCoroutineScope()
            var showProfileDialog by remember { mutableStateOf(false) }
            val context = LocalContext.current
            
            if (showProfileDialog) {
                val currentUser = FirebaseAuth.getInstance().currentUser
                Dialog(
                    onDismissRequest = { showProfileDialog = false }
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant 
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Profile Details",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant 
                                )
                            }
                            
                            Divider(
                                modifier = Modifier.padding(vertical = 8.dp),
                                color = MaterialTheme.colorScheme.outline 
                            )
                            
                            ProfileItemThemed(
                                label = "Username",
                                value = currentUser?.displayName ?: currentUser?.email?.substringBefore("@") ?: "Not available"
                            )
                            ProfileItemThemed(
                                label = "Email",
                                value = currentUser?.email ?: "Not available"
                            )
                            ProfileItemThemed(
                                label = "Account Type",
                                value = if (currentUser?.isEmailVerified == true) "Verified User" else "Standard User"
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Button(
                                onClick = { showProfileDialog = false },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Close")
                            }
                        }
                    }
                }
            }
            
            LLMInferenceTheme(
                darkTheme = isDarkThemeState 
            ) {
                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        ModalDrawerSheet {
                            Spacer(modifier = Modifier.height(16.dp))
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = "Profile",
                                        modifier = Modifier.size(40.dp)
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Text(
                                        text = "Welcome!",
                                        style = MaterialTheme.typography.titleLarge
                                    )
                                }
                                
                                Divider(modifier = Modifier.padding(vertical = 16.dp))
                                
                                NavigationDrawerItem(
                                    icon = { 
                                        Icon(
                                            imageVector = if (isDarkThemeState) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
                                            contentDescription = "Toggle Theme"
                                        )
                                    },
                                    label = { Text(if (isDarkThemeState) "Switch to Light Mode" else "Switch to Dark Mode") },
                                    selected = false,
                                    onClick = { 
                                        val newTheme = if (isDarkThemeState) ThemePreferenceManager.THEME_LIGHT else ThemePreferenceManager.THEME_DARK
                                        ThemePreferenceManager.saveThemePreference(context, newTheme)
                                        isDarkThemeState = !isDarkThemeState
                                    }
                                )
                                
                                val menuItems = listOf(
                                    NavigationDrawerItemData(
                                        icon = Icons.Default.List,
                                        label = "Models",
                                        onClick = {
                                            scope.launch {
                                                drawerState.close()
                                            }
                                            val intent = Intent(this@HomeActivity, MainActivity::class.java)
                                            intent.putExtra("NAVIGATE_TO", "start_screen")
                                            (context as? Activity)?.startActivity(intent)
                                            (context as? Activity)?.overridePendingTransition(R.anim.scale_in, R.anim.scale_out)
                                        }
                                    ),
                                    NavigationDrawerItemData(
                                        icon = Icons.Default.History,
                                        label = "Chat History",
                                        onClick = {
                                            scope.launch {
                                                drawerState.close()
                                            }
                                            val intent = Intent(this@HomeActivity, MainActivity::class.java)
                                            intent.putExtra("NAVIGATE_TO", "chat_history")
                                            (context as? Activity)?.startActivity(intent)
                                            (context as? Activity)?.overridePendingTransition(R.anim.scale_in, R.anim.scale_out)
                                        }
                                    ),
                                    NavigationDrawerItemData(
                                        icon = Icons.Default.Person,
                                        label = "Profile",
                                        onClick = {
                                            scope.launch {
                                                drawerState.close()
                                            }
                                            showProfileDialog = true
                                        }
                                    ),
                                    NavigationDrawerItemData(
                                        icon = Icons.Default.ExitToApp,
                                        label = "Logout",
                                        onClick = {
                                            scope.launch {
                                                drawerState.close()
                                            }
                                            handleLogout()
                                        }
                                    )
                                )

                                val visibleStates = remember { menuItems.map { mutableStateOf(false) } }

                                LaunchedEffect(drawerState.isOpen) {
                                    if (drawerState.isOpen) {
                                        menuItems.indices.forEach { i ->
                                            delay(100L * i)
                                            visibleStates[i].value = true
                                        }
                                    } else {
                                        menuItems.indices.forEach { i ->
                                            visibleStates[i].value = false
                                        }
                                    }
                                }

                                menuItems.forEachIndexed { i, item ->
                                    AnimatedVisibility(
                                        visible = visibleStates[i].value,
                                        enter = fadeIn() + slideInVertically(initialOffsetY = { -40 }),
                                        exit = fadeOut()
                                    ) {
                                        NavigationDrawerItem(
                                            icon = { Icon(item.icon, contentDescription = item.label) },
                                            label = { Text(item.label) },
                                            selected = false,
                                            onClick = item.onClick
                                        )
                                    }
                                }
                            }
                        }
                    }
                ) {
                    Scaffold(
                        topBar = {
                            TopAppBar(
                                title = { Text("Offline AI Tutor") },
                                navigationIcon = {
                                    IconButton(onClick = {
                                        scope.launch {
                                            drawerState.open()
                                        }
                                    }) {
                                        Icon(Icons.Default.Menu, "Menu")
                                    }
                                }
                            )
                        }
                    ) { paddingValues ->
                        Surface(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(paddingValues),
                            color = MaterialTheme.colorScheme.background
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                HomeContent(
                                    onStartAiChat = {
                                        val intent = Intent(this@HomeActivity, MainActivity::class.java)
                                        intent.putExtra("NAVIGATE_TO", "load_screen")
                                        (context as? Activity)?.startActivity(intent)
                                        (context as? Activity)?.overridePendingTransition(R.anim.scale_in, R.anim.scale_out)
                                    },
                                    onStartRoleplay = {
                                        val intent = Intent(this@HomeActivity, MainActivity::class.java)
                                        intent.putExtra("NAVIGATE_TO", "roleplay_selection")
                                        (context as? Activity)?.startActivity(intent)
                                        (context as? Activity)?.overridePendingTransition(R.anim.scale_in, R.anim.scale_out)
                                    },
                                    onSelectModel = {
                                        val intent = Intent(this@HomeActivity, MainActivity::class.java)
                                        intent.putExtra("NAVIGATE_TO", "start_screen")
                                        (context as? Activity)?.startActivity(intent)
                                        (context as? Activity)?.overridePendingTransition(R.anim.scale_in, R.anim.scale_out)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    
    override fun onStart() {
        super.onStart()
        // Add the auth state listener when activity starts
        FirebaseAuth.getInstance().addAuthStateListener(authStateListener)
        // Apply theme in onStart as well in case preference changed while activity was stopped
        ThemePreferenceManager.applyTheme(ThemePreferenceManager.loadThemePreference(this))
    }
    
    override fun onStop() {
        super.onStop()
        // Remove the auth state listener when activity stops
        FirebaseAuth.getInstance().removeAuthStateListener(authStateListener)
    }
    
    private fun handleLogout() {
        // Notify TextFileStorage about logout before signing out
        textFileStorage.handleAuthStateChange(false)
        
        // Sign out from Firebase
        FirebaseAuth.getInstance().signOut()
        
        // Start auth activity
        startAuthActivity()
    }
    
    private fun startAuthActivity() {
        val intent = Intent(this, AuthActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or 
                      Intent.FLAG_ACTIVITY_CLEAR_TASK
        (this as? Activity)?.startActivity(intent)
        (this as? Activity)?.overridePendingTransition(R.anim.scale_in, R.anim.scale_out)
        finish()
    }
}

@Composable
fun ProfileItemThemed(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    isDarkTheme: Boolean,
    onThemeChange: (Boolean) -> Unit,
    onLogout: () -> Unit
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    var showModelInfo by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Offline AI Tutor",
                        style = MaterialTheme.typography.headlineMedium
                    )
                },
                actions = {
                    IconButton(onClick = { onThemeChange(!isDarkTheme) }) {
                        Icon(
                            imageVector = if (isDarkTheme) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
                            contentDescription = "Toggle Theme"
                        )
                    }
                    
                    IconButton(onClick = { showModelInfo = !showModelInfo }) {
                        Icon(
                            imageVector = Icons.Filled.Info,
                            contentDescription = "Model Info"
                        )
                    }
                    
                    IconButton(onClick = { navController.navigate(Routes.PROFILE) }) {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = "Profile"
                        )
                    }

                    IconButton(onClick = onLogout) {
                        Icon(
                            imageVector = Icons.Filled.ExitToApp,
                            contentDescription = "Logout"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            NavHost(
                navController = navController,
                startDestination = Routes.HOME,
                modifier = Modifier.fillMaxSize()
            ) {
                composable(Routes.HOME) {
                    HomeContent(
                        onStartAiChat = {
                            val intent = Intent(context, MainActivity::class.java)
                            if (InferenceModel.model != null) {
                                intent.putExtra("NAVIGATE_TO", "chat_screen")
                            } else {
                                intent.putExtra("NAVIGATE_TO", "start_screen")
                            }
                            (context as? Activity)?.startActivity(intent)
                            (context as? Activity)?.overridePendingTransition(R.anim.scale_in, R.anim.scale_out)
                        },
                        onStartRoleplay = {
                            navController.navigate(Routes.ROLEPLAY_SELECTION)
                        },
                        onSelectModel = {
                            val intent = Intent(context, MainActivity::class.java)
                            intent.putExtra("NAVIGATE_TO", "start_screen")
                            (context as? Activity)?.startActivity(intent)
                            (context as? Activity)?.overridePendingTransition(R.anim.scale_in, R.anim.scale_out)
                        }
                    )
                }

                composable(Routes.ROLEPLAY_SELECTION) {
                    RoleplaySelectionScreen(
                        onRoleSelected = { role ->
                            val intent = Intent(context, MainActivity::class.java)
                            intent.putExtra("NAVIGATE_TO", "start_screen")
                            intent.putExtra("SELECTED_ROLE", role)
                            (context as? Activity)?.startActivity(intent)
                            (context as? Activity)?.overridePendingTransition(R.anim.scale_in, R.anim.scale_out)
                        }
                    )
                }

                composable(Routes.PROFILE) {
                    ProfileScreen(
                        onBack = { navController.popBackStack() }
                    )
                }
            }

            if (showModelInfo) {
                AlertDialog(
                    onDismissRequest = { showModelInfo = false },
                    title = { Text("Model Information") },
                    text = {
                        Column(
                            modifier = Modifier
                                .padding(vertical = 8.dp)
                                .clickable {
                                    showModelInfo = false
                                    val intent = Intent(context, MainActivity::class.java)
                                    intent.putExtra("NAVIGATE_TO", "start_screen")
                                    (context as? Activity)?.startActivity(intent)
                                    (context as? Activity)?.overridePendingTransition(R.anim.scale_in, R.anim.scale_out)
                                }
                        ) {
                            Text(
                                "Current Model: ${InferenceModel.model?.name ?: "No model selected"}",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Backend: ${InferenceModel.model?.preferredBackend?.name ?: "N/A"}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Temperature: ${InferenceModel.model?.temperature ?: "N/A"}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Click to change model",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showModelInfo = false }) {
                            Text("Close")
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun ProfileScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var userInfo by remember { mutableStateOf(SecureStorage.getUserInfo(context)) }
    
    // Effect to update user info when the screen is composed
    LaunchedEffect(Unit) {
        userInfo = SecureStorage.getUserInfo(context)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "User Profile",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                ProfileItem("Username", userInfo?.username ?: "Not available")
                ProfileItem("Email", userInfo?.email ?: "Not available")
                ProfileItem("Account Type", userInfo?.accountType ?: "Not available")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Back to Home")
        }
    }
}

@Composable
fun ProfileItem(label: String, value: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun HomeContent(
    onStartAiChat: () -> Unit,
    onStartRoleplay: () -> Unit,
    onSelectModel: () -> Unit
) {
    val cards = listOf(
        Triple("Start with AI", "Chat with AI instantly", R.drawable.ai_head),
        Triple("Roleplay Selection", "Practice real conversations", R.drawable.roleplay),
        Triple("Model Selection", "Choose your AI model", R.drawable.model_gears)
    )
    val actions = listOf(onStartAiChat, onStartRoleplay, onSelectModel)
    val colors = listOf(
        Color(0xFFd90429), // Blue for AI
        Color(0xFFd8b3f9), // Orange for Roleplay
        Color(0xFF57cc99)  // Purple for Model
    )
    val overlap = 129.dp
    var visibleCount by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        for (i in cards.indices) {
            delay(180L)
            visibleCount = i + 1
        }
    }
    val horizontalOverlap = 0.dp // Positive for rightward shift
    val cardShapeList = listOf(
        RoundedCornerShape(topStart = 32.dp, bottomEnd = 45.dp), // Top card
        RoundedCornerShape(topStart = 32.dp, bottomEnd = 45.dp), // Middle card (diagonal)
        RoundedCornerShape(topStart = 32.dp, bottomEnd = 32.dp) // Bottom card
    )
    val cardHeight = 155.dp
    val stackHeight = cardHeight + (cards.size - 1) * overlap
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .height(stackHeight)
                .fillMaxWidth(),
            contentAlignment = Alignment.TopCenter
        ) {
            cards.take(visibleCount).forEachIndexed { i, card ->
                SelectionCard(
                    title = card.first,
                    subtitle = card.second,
                    imageRes = card.third,
                    backgroundColor = colors[i],
                    onClick = actions[i],
                    modifier = Modifier
                        .offset(x = horizontalOverlap * i, y = overlap * i)
                        .zIndex(i.toFloat())
                        .height(cardHeight),
                    shape = cardShapeList[i]
                )
            }
        }
    }
}

@Composable
fun RoleplaySelectionScreen(
    onRoleSelected: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "Select Roleplay Mode",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        Button(
            onClick = { onRoleSelected("interviewer") },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
        ) {
            Text("Interviewer")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { onRoleSelected("customer_care") },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
        ) {
            Text("Customer Care")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { onRoleSelected("professional_meeting") },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
        ) {
            Text("Professional Meeting")
        }
    }
}

data class NavigationDrawerItemData(
    val icon: ImageVector,
    val label: String,
    val onClick: () -> Unit
) 