package com.google.mediapipe.examples.llminference

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

class TextFileStorage(private val context: Context) {
    // Store the current conversation file name
    private var currentConversationFile: String? = null
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    // Track sync status of each file
    private val syncStatus = ConcurrentHashMap<String, Boolean>()
    
    // Listener for sync status changes
    interface SyncStatusListener {
        fun onSyncStatusChanged(fileName: String, isSynced: Boolean)
    }
    
    // Listener for when new files are loaded from Firebase
    interface FirebaseDataListener {
        fun onFirebaseDataLoaded()
    }
    
    private val syncStatusListeners = mutableListOf<SyncStatusListener>()
    private val firebaseDataListeners = mutableListOf<FirebaseDataListener>()
    
    fun addSyncStatusListener(listener: SyncStatusListener) {
        syncStatusListeners.add(listener)
    }
    
    fun removeSyncStatusListener(listener: SyncStatusListener) {
        syncStatusListeners.remove(listener)
    }
    
    fun addFirebaseDataListener(listener: FirebaseDataListener) {
        firebaseDataListeners.add(listener)
    }
    
    fun removeFirebaseDataListener(listener: FirebaseDataListener) {
        firebaseDataListeners.remove(listener)
    }
    
    private fun notifyFirebaseDataLoaded() {
        firebaseDataListeners.forEach { it.onFirebaseDataLoaded() }
    }
    
    private fun updateSyncStatus(fileName: String, isSynced: Boolean) {
        syncStatus[fileName] = isSynced
        syncStatusListeners.forEach { it.onSyncStatusChanged(fileName, isSynced) }
    }
    
    // Get sync status of a file
    fun isSynced(fileName: String): Boolean {
        return syncStatus[fileName] ?: false
    }
    
    // Get current user ID or default to device-specific ID
    private fun getCurrentUserId(): String {
        return auth.currentUser?.uid ?: "device_${android.provider.Settings.Secure.getString(
            context.contentResolver,
            android.provider.Settings.Secure.ANDROID_ID
        )}"
    }
    
    // Check if user is logged in
    fun isUserLoggedIn(): Boolean {
        return auth.currentUser != null
    }
    
    // Save text to a new file (legacy method for backward compatibility)
    fun saveTextToFile(text: String) {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "chat_history_$timestamp.txt"
        
        try {
            context.openFileOutput(fileName, Context.MODE_PRIVATE).use { outputStream ->
                outputStream.write(text.toByteArray())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    // Start a new conversation file
    fun startNewConversation(): String {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        currentConversationFile = "chat_history_$timestamp.txt"
        
        // Create a new conversation document in Firestore
        val userId = getCurrentUserId()
        val conversationData = hashMapOf(
            "userId" to userId,
            "timestamp" to timestamp,
            "messages" to arrayListOf<Map<String, Any>>(),
            "lastUpdated" to Date()
        )
        
        try {
            if (isUserLoggedIn()) {
                db.collection("conversations")
                    .document("${userId}_$timestamp")
                    .set(conversationData)
                    .addOnSuccessListener {
                        updateSyncStatus(currentConversationFile!!, true)
                        Log.d("TextFileStorage", "Created new conversation in Firebase: ${userId}_$timestamp")
                    }
                    .addOnFailureListener { e ->
                        Log.e("TextFileStorage", "Error creating conversation", e)
                        updateSyncStatus(currentConversationFile!!, false)
                    }
            } else {
                Log.d("TextFileStorage", "User not logged in, not creating Firebase document")
                updateSyncStatus(currentConversationFile!!, false)
            }
        } catch (e: Exception) {
            Log.e("TextFileStorage", "Error creating conversation", e)
            updateSyncStatus(currentConversationFile!!, false)
        }
        
        return currentConversationFile!!
    }
    
    // Get or create the current conversation file
    fun getCurrentConversationFile(): String {
        if (currentConversationFile == null) {
            startNewConversation()
        }
        return currentConversationFile!!
    }
    
    // Clear current conversation reference
    fun clearCurrentConversation() {
        currentConversationFile = null
    }
    
    // Append message to the current conversation
    fun appendToConversation(prefix: String, message: String) {
        val fileName = getCurrentConversationFile()
        // Add extra spacing for better readability
        val formattedMessage = "$prefix: $message\n\n\n"
        
        try {
            // Check if file exists
            val exists = context.fileList().contains(fileName)
            
            // Save to local storage
            context.openFileOutput(fileName, if (exists) Context.MODE_APPEND else Context.MODE_PRIVATE).use { outputStream ->
                outputStream.write(formattedMessage.toByteArray())
            }
            
            // Update sync status to indicate sync in progress
            updateSyncStatus(fileName, false)
            
            // Save to Firebase
            val userId = getCurrentUserId()
            val timestamp = fileName.removePrefix("chat_history_").removeSuffix(".txt")
            val messageData = hashMapOf(
                "sender" to prefix,
                "content" to message,
                "timestamp" to Date()
            )
            
            if (isUserLoggedIn()) {
                val docRef = db.collection("conversations").document("${userId}_$timestamp")
                
                // Check if the document exists first
                docRef.get()
                    .addOnSuccessListener { document ->
                        if (document.exists()) {
                            // Document exists, update it
                            docRef.update(
                                "messages", com.google.firebase.firestore.FieldValue.arrayUnion(messageData),
                                "lastUpdated", Date()
                            ).addOnSuccessListener {
                                updateSyncStatus(fileName, true)
                                Log.d("TextFileStorage", "Updated conversation in Firebase: ${userId}_$timestamp")
                            }.addOnFailureListener { e ->
                                Log.e("TextFileStorage", "Error updating document", e)
                                updateSyncStatus(fileName, false)
                            }
                        } else {
                            // Document doesn't exist, create it
                            val conversationData = hashMapOf(
                                "userId" to userId,
                                "timestamp" to timestamp,
                                "messages" to arrayListOf(messageData),
                                "lastUpdated" to Date()
                            )
                            docRef.set(conversationData)
                                .addOnSuccessListener {
                                    updateSyncStatus(fileName, true)
                                    Log.d("TextFileStorage", "Created new conversation with message in Firebase: ${userId}_$timestamp")
                                }
                                .addOnFailureListener { e ->
                                    Log.e("TextFileStorage", "Error creating document", e)
                                    updateSyncStatus(fileName, false)
                                }
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("TextFileStorage", "Error checking document existence", e)
                        updateSyncStatus(fileName, false)
                    }
            } else {
                Log.d("TextFileStorage", "User not logged in, not updating Firebase")
                updateSyncStatus(fileName, false)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            updateSyncStatus(fileName, false)
        }
    }
    
    // Get chat history files from local storage only
    fun getChatHistoryFiles(): List<File> {
        // Get local files
        val localFiles = context.filesDir.listFiles { file ->
            file.name.startsWith("chat_history_") && file.name.endsWith(".txt")
        }?.sortedByDescending { it.lastModified() } ?: emptyList()
        
        // Initialize sync status for all files as false
        localFiles.forEach { file ->
            if (!syncStatus.containsKey(file.name)) {
                syncStatus[file.name] = false
            }
        }
        
        return localFiles
    }
    
    // Force a sync from Firebase - will download all conversations for current user
    fun forceFirebaseSync() {
        if (isUserLoggedIn()) {
            Log.d("TextFileStorage", "Forcing Firebase sync for user: ${getCurrentUserId()}")
            syncFirebaseConversations(true)
        } else {
            Log.d("TextFileStorage", "Not forcing Firebase sync - user not logged in")
        }
    }
    
    // Sync conversations from Firebase to local storage
    private fun syncFirebaseConversations(forceRefresh: Boolean = false) {
        if (!isUserLoggedIn()) {
            Log.d("TextFileStorage", "User not logged in, skipping Firebase sync")
            return
        }
        
        val userId = getCurrentUserId()
        Log.d("TextFileStorage", "Syncing Firebase conversations for user: $userId")
        
        db.collection("conversations")
            .whereEqualTo("userId", userId)
            .orderBy("lastUpdated", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                var filesCreated = false
                Log.d("TextFileStorage", "Got ${documents.size()} conversations from Firebase")
                
                if (documents.isEmpty) {
                    Log.d("TextFileStorage", "No conversations found in Firebase for user: $userId")
                }
                
                for (document in documents) {
                    val data = document.data
                    val timestamp = data["timestamp"] as? String ?: continue
                    val fileName = "chat_history_$timestamp.txt"
                    
                    Log.d("TextFileStorage", "Processing conversation: $fileName")
                    
                    // Mark this file as synced
                    updateSyncStatus(fileName, true)
                    
                    // Always create/update local file when force refresh is enabled
                    if (forceRefresh || !context.fileList().contains(fileName)) {
                        // Create local file
                        val messages = data["messages"] as? ArrayList<HashMap<String, Any>> ?: continue
                        val content = StringBuilder()
                        
                        for (message in messages) {
                            val sender = message["sender"] as? String ?: continue
                            val messageContent = message["content"] as? String ?: continue
                            content.append("$sender: $messageContent\n\n\n")
                        }
                        
                        try {
                            context.openFileOutput(fileName, Context.MODE_PRIVATE).use { outputStream ->
                                outputStream.write(content.toString().toByteArray())
                            }
                            filesCreated = true
                            Log.d("TextFileStorage", "Created/Updated local file from Firebase: $fileName")
                        } catch (e: Exception) {
                            Log.e("TextFileStorage", "Error creating local file from Firebase", e)
                        }
                    } else {
                        Log.d("TextFileStorage", "File already exists locally: $fileName")
                    }
                }
                
                // Notify listeners if files were created or force refresh was enabled
                if (filesCreated || forceRefresh) {
                    Log.d("TextFileStorage", "Notifying listeners of data load")
                    notifyFirebaseDataLoaded()
                }
            }
            .addOnFailureListener { e ->
                Log.e("TextFileStorage", "Error fetching conversations", e)
            }
    }
    
    // Check if a specific file is synced with Firebase
    fun checkFileSync(fileName: String) {
        if (!isUserLoggedIn()) {
            Log.d("TextFileStorage", "User not logged in, marking file as not synced: $fileName")
            updateSyncStatus(fileName, false)
            return
        }
        
        val userId = getCurrentUserId()
        val timestamp = fileName.removePrefix("chat_history_").removeSuffix(".txt")
        
        db.collection("conversations")
            .document("${userId}_$timestamp")
            .get()
            .addOnSuccessListener { document ->
                val exists = document.exists()
                updateSyncStatus(fileName, exists)
                Log.d("TextFileStorage", "File sync check for $fileName: $exists")
            }
            .addOnFailureListener { e ->
                Log.e("TextFileStorage", "Error checking if file is synced", e)
                updateSyncStatus(fileName, false)
            }
    }
    
    fun readChatHistory(file: File): String {
        return try {
            context.openFileInput(file.name).bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }
    
    fun deleteFile(fileName: String): Boolean {
        try {
            // Delete from local storage
            val localSuccess = context.deleteFile(fileName)
            
            // Delete from Firebase
            if (isUserLoggedIn()) {
                val userId = getCurrentUserId()
                val timestamp = fileName.removePrefix("chat_history_").removeSuffix(".txt")
                
                db.collection("conversations")
                    .document("${userId}_$timestamp")
                    .delete()
                    .addOnSuccessListener {
                        // Remove from sync status tracking
                        syncStatus.remove(fileName)
                        Log.d("TextFileStorage", "Deleted conversation from Firebase: ${userId}_$timestamp")
                    }
                    .addOnFailureListener { e ->
                        Log.e("TextFileStorage", "Error deleting conversation", e)
                    }
            }
            
            return localSuccess
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }
    
    // Fetch all conversations for current user directly from Firebase
    suspend fun getFirebaseConversations(): List<FirebaseConversation> {
        if (!isUserLoggedIn()) {
            Log.d("TextFileStorage", "User not logged in, returning empty conversations list")
            return emptyList()
        }
        
        val userId = getCurrentUserId()
        val conversations = mutableListOf<FirebaseConversation>()
        
        try {
            Log.d("TextFileStorage", "Fetching Firebase conversations for user: $userId")
            val querySnapshot = db.collection("conversations")
                .whereEqualTo("userId", userId)
                .orderBy("lastUpdated", Query.Direction.DESCENDING)
                .get()
                .await()
                
            Log.d("TextFileStorage", "Got ${querySnapshot.documents.size} conversations from Firebase")
            
            for (document in querySnapshot.documents) {
                val data = document.data ?: continue
                val timestamp = data["timestamp"] as? String ?: continue
                val messages = data["messages"] as? ArrayList<HashMap<String, Any>> ?: continue
                
                val parsedMessages = messages.mapNotNull { message ->
                    val sender = message["sender"] as? String ?: return@mapNotNull null
                    val content = message["content"] as? String ?: return@mapNotNull null
                    FirebaseMessage(sender, content)
                }
                
                conversations.add(FirebaseConversation(timestamp, parsedMessages))
                
                // Mark the file as synced
                val fileName = "chat_history_$timestamp.txt"
                updateSyncStatus(fileName, true)
            }
        } catch (e: Exception) {
            Log.e("TextFileStorage", "Error fetching Firebase conversations", e)
        }
        
        return conversations
    }
    
    // Handle user login/logout events
    fun handleAuthStateChange(isLoggedIn: Boolean) {
        if (isLoggedIn) {
            Log.d("TextFileStorage", "User logged in, forcing Firebase sync")
            forceFirebaseSync()
        } else {
            Log.d("TextFileStorage", "User logged out, clearing sync status")
            syncStatus.clear()
            clearCurrentConversation()
        }
    }
    
    // Data classes for Firebase operations
    data class FirebaseMessage(val sender: String, val content: String)
    data class FirebaseConversation(val timestamp: String, val messages: List<FirebaseMessage>)
} 