package com.google.mediapipe.examples.llminference

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

// Function to clean up chat history content by removing timestamp numbers
private fun cleanChatHistoryContent(content: String): String {
    val lines = content.split("\n")
    val cleanedLines = mutableListOf<String>()
    
    var i = 0
    while (i < lines.size) {
        val line = lines[i].trim()
        // Skip timestamp lines (they are in format YYYYMMDD HHMMSS)
        if (line.matches(Regex("\\d{8}\\s\\d{6}"))) {
            i++
            // If next line exists and contains "User:" or "AI:", add it without the timestamp
            if (i < lines.size) {
                cleanedLines.add(lines[i])
            }
        } else if (line.startsWith("User:") || 
                   line.startsWith("AI:") || 
                   line.startsWith("AI Error:")) {
            // Direct format without timestamp (new format)
            cleanedLines.add(line)
        } else if (!line.isEmpty()) {
            // Keep other non-empty lines that aren't timestamps
            cleanedLines.add(line)
        }
        i++
    }
    
    return cleanedLines.joinToString("\n")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatHistoryScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val textFileStorage = remember { TextFileStorage(context) }
    val chatFiles = remember { mutableStateOf<List<File>>(emptyList()) }
    val isLoading = remember { mutableStateOf(true) }
    val firebaseAuth = remember { FirebaseAuth.getInstance() }
    val currentUser = remember { mutableStateOf(firebaseAuth.currentUser) }
    val scope = rememberCoroutineScope()
    val cloudSyncStatus = remember { mutableStateMapOf<String, Boolean>() }
    val showLoginPrompt = remember { mutableStateOf(!textFileStorage.isUserLoggedIn()) }
    
    // Separate lists for local and cloud chats
    val localChats = remember { mutableStateOf<List<File>>(emptyList()) }
    val cloudChats = remember { mutableStateOf<List<File>>(emptyList()) }
    
    // Set up a listener for sync status changes
    DisposableEffect(textFileStorage) {
        val syncListener = object : TextFileStorage.SyncStatusListener {
            override fun onSyncStatusChanged(fileName: String, isSynced: Boolean) {
                cloudSyncStatus[fileName] = isSynced
            }
        }
        
        val dataListener = object : TextFileStorage.FirebaseDataListener {
            override fun onFirebaseDataLoaded() {
                // Refresh the list of chat files when Firebase data is loaded
                scope.launch(Dispatchers.IO) {
                    val newFiles = textFileStorage.getChatHistoryFiles()
                    withContext(Dispatchers.Main) {
                        chatFiles.value = newFiles
                    }
                }
            }
        }
        
        textFileStorage.addSyncStatusListener(syncListener)
        textFileStorage.addFirebaseDataListener(dataListener)
        
        onDispose {
            textFileStorage.removeSyncStatusListener(syncListener)
            textFileStorage.removeFirebaseDataListener(dataListener)
        }
    }
    
    // Function to load chat history
    fun loadChatHistory() {
        isLoading.value = true
        Log.d("ChatHistoryScreen", "Starting chat history load")
        
        // First force a Firebase sync for logged-in users
        if (textFileStorage.isUserLoggedIn()) {
            Log.d("ChatHistoryScreen", "User is logged in, forcing Firebase sync")
            scope.launch {
                textFileStorage.forceFirebaseSync()
                // After sync completes, get the files
                val newFiles = textFileStorage.getChatHistoryFiles()
                withContext(Dispatchers.Main) {
                    chatFiles.value = newFiles
                    Log.d("ChatHistoryScreen", "Loaded ${newFiles.size} chat files after sync")
                    
                    // Separate local and cloud chats
                    localChats.value = newFiles.filter { !(cloudSyncStatus[it.name] ?: false) }
                    cloudChats.value = newFiles.filter { cloudSyncStatus[it.name] ?: false }
                    
                    // Initialize sync status for all files
                    chatFiles.value.forEach { file ->
                        cloudSyncStatus[file.name] = textFileStorage.isSynced(file.name)
                        // Check sync status with Firebase
                        textFileStorage.checkFileSync(file.name)
                    }
                    
                    isLoading.value = false
                }
            }
        } else {
            Log.d("ChatHistoryScreen", "User is not logged in, loading local files only")
            chatFiles.value = textFileStorage.getChatHistoryFiles()
            localChats.value = chatFiles.value
            cloudChats.value = emptyList()
            
            // Initialize sync status for all files
            chatFiles.value.forEach { file ->
                cloudSyncStatus[file.name] = textFileStorage.isSynced(file.name)
            }
            
            isLoading.value = false
        }
    }
    
    // Load chat history files initially
    LaunchedEffect(currentUser.value) {
        loadChatHistory()
        showLoginPrompt.value = !textFileStorage.isUserLoggedIn()
    }
    
    // User login state changed - critical for cross-device functionality
    DisposableEffect(Unit) {
        val authStateListener = FirebaseAuth.AuthStateListener { auth ->
            val newUser = auth.currentUser
            val wasLoggedIn = currentUser.value != null
            val isLoggedIn = newUser != null
            
            // Update current user
            currentUser.value = newUser
            
            // Handle login/logout
            if (wasLoggedIn != isLoggedIn) {
                showLoginPrompt.value = !isLoggedIn
                textFileStorage.handleAuthStateChange(isLoggedIn)
                loadChatHistory()
                    }
                }
        
        firebaseAuth.addAuthStateListener(authStateListener)
        
        onDispose {
            firebaseAuth.removeAuthStateListener(authStateListener)
        }
    }
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Add refresh button at the top
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.End
        ) {
            IconButton(
                onClick = { 
                    textFileStorage.forceFirebaseSync() 
                    loadChatHistory()
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh"
                )
            }
        }
        
        // Show login prompt if user is not logged in
        if (showLoginPrompt.value) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Login Required for Cloud Sync",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Please log in to sync your chat history across devices.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
        
        if (isLoading.value) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (chatFiles.value.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("No chat history found")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                // Cloud Chats Section
                if (cloudChats.value.isNotEmpty()) {
                    item {
                        Text(
                            text = "Cloud Chats",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                    items(cloudChats.value) { file ->
                        val isSynced = cloudSyncStatus[file.name] ?: false
                        ChatHistoryItem(
                            file = file,
                            textFileStorage = textFileStorage,
                            isSynced = isSynced,
                            onDelete = {
                                if (textFileStorage.deleteFile(file.name)) {
                                    loadChatHistory()
                                }
                            }
                        )
                    }
                }
                
                // Local Chats Section
                if (localChats.value.isNotEmpty()) {
                    item {
                        Text(
                            text = "Local Chats",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                    items(localChats.value) { file ->
                        val isSynced = cloudSyncStatus[file.name] ?: false
                    ChatHistoryItem(
                        file = file,
                            textFileStorage = textFileStorage,
                            isSynced = isSynced,
                            onDelete = {
                                if (textFileStorage.deleteFile(file.name)) {
                                    loadChatHistory()
                                }
                            }
                    )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatHistoryItem(
    file: File,
    textFileStorage: TextFileStorage,
    isSynced: Boolean,
    onDelete: () -> Unit
) {
    var showContent by remember { mutableStateOf(false) }
    var messages by remember { mutableStateOf<List<ChatHistoryMessage>>(emptyList()) }
    
    // Load file content when expanded
    LaunchedEffect(showContent) {
        if (showContent) {
            // Read the content and clean it up before displaying
            val rawContent = textFileStorage.readChatHistory(file)
            val cleanedContent = cleanChatHistoryContent(rawContent)
            messages = parseMessagesToList(cleanedContent)
        }
    }

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
            // Display date and sync status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Format filename to show only the date part (yyyyMMdd)
                val rawTimestamp = file.name
                .removePrefix("chat_history_")
                .removeSuffix(".txt")
                
                // Parse the date format
                val dateOnly = try {
                    val fullDate = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).parse(rawTimestamp)
                    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(fullDate)
                } catch (e: Exception) {
                    rawTimestamp.split("_").firstOrNull() ?: rawTimestamp
                }
                
                // Display date and cloud status
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = dateOnly,
                        style = MaterialTheme.typography.titleMedium
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    // Cloud sync status icon
                    Icon(
                        imageVector = if (isSynced) Icons.Default.Cloud else Icons.Default.CloudOff,
                        contentDescription = if (isSynced) "Synced to cloud" else "Not synced to cloud",
                        tint = if (isSynced) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                IconButton(
                    onClick = onDelete
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
            
            // Add sync status text
            Text(
                text = if (isSynced) "Saved in cloud" else "Not saved in cloud",
                style = MaterialTheme.typography.bodySmall,
                color = if (isSynced) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
            
            // Chat content section with clickable row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showContent = !showContent }
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (showContent) "Hide conversation" else "View conversation",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            if (showContent) {
                Spacer(modifier = Modifier.height(16.dp))
                
                // Display each message with visual separation
                messages.forEach { message ->
                    MessageDisplay(message)
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

// Data class to represent a message in the chat history
data class ChatHistoryMessage(
    val sender: String,
    val content: String,
    val isUser: Boolean
)

// Function to parse the cleaned content into a list of messages
private fun parseMessagesToList(content: String): List<ChatHistoryMessage> {
    val lines = content.split("\n")
    val messages = mutableListOf<ChatHistoryMessage>()
    
    var currentSender = ""
    var currentContent = StringBuilder()
    
    for (line in lines) {
        val trimmedLine = line.trim()
        if (trimmedLine.startsWith("User:")) {
            // If we were collecting a previous message, add it
            if (currentSender.isNotEmpty() && currentContent.isNotEmpty()) {
                messages.add(ChatHistoryMessage(
                    sender = currentSender,
                    content = currentContent.toString().trim(),
                    isUser = currentSender.startsWith("User")
                ))
                currentContent = StringBuilder()
            }
            currentSender = "User"
            currentContent.append(trimmedLine.removePrefix("User:").trim()).append("\n")
        } else if (trimmedLine.startsWith("AI:")) {
            // If we were collecting a previous message, add it
            if (currentSender.isNotEmpty() && currentContent.isNotEmpty()) {
                messages.add(ChatHistoryMessage(
                    sender = currentSender,
                    content = currentContent.toString().trim(),
                    isUser = currentSender.startsWith("User")
                ))
                currentContent = StringBuilder()
            }
            currentSender = "AI"
            currentContent.append(trimmedLine.removePrefix("AI:").trim()).append("\n")
        } else if (trimmedLine.startsWith("AI Error:")) {
            // If we were collecting a previous message, add it
            if (currentSender.isNotEmpty() && currentContent.isNotEmpty()) {
                messages.add(ChatHistoryMessage(
                    sender = currentSender,
                    content = currentContent.toString().trim(),
                    isUser = currentSender.startsWith("User")
                ))
                currentContent = StringBuilder()
            }
            currentSender = "AI Error"
            currentContent.append(trimmedLine.removePrefix("AI Error:").trim()).append("\n")
        } else if (trimmedLine.isNotEmpty()) {
            // Continue the current message
            currentContent.append(trimmedLine).append("\n")
        }
    }
    
    // Add the last message if there is one
    if (currentSender.isNotEmpty() && currentContent.isNotEmpty()) {
        messages.add(ChatHistoryMessage(
            sender = currentSender,
            content = currentContent.toString().trim(),
            isUser = currentSender.startsWith("User")
        ))
    }
    
    return messages
}

// Composable to display a single message
@Composable
private fun MessageDisplay(message: ChatHistoryMessage) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp)
    ) {
        // Sender label with appropriate styling
        Text(
            text = message.sender,
            style = MaterialTheme.typography.labelLarge,
            color = if (message.isUser) 
                        MaterialTheme.colorScheme.primary 
                    else 
                        MaterialTheme.colorScheme.tertiary,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // Message content
        Text(
            text = message.content,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(start = 8.dp)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Add divider between messages
        Divider(
            color = MaterialTheme.colorScheme.outlineVariant,
            thickness = 0.5.dp
        )
    }
} 