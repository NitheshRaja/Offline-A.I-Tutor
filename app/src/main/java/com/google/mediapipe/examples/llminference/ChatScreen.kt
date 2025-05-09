package com.google.mediapipe.examples.llminference

import android.Manifest
import android.app.Activity
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.StateFlow
import java.text.SimpleDateFormat
import java.util.*
import android.util.Log

@Composable
internal fun ChatRoute(
    onClose: () -> Unit
) {
    val context = LocalContext.current.applicationContext
    val chatViewModel: ChatViewModel = viewModel(factory = ChatViewModel.getFactory(context))

    // Reset InferenceModel and clear chat when entering ChatScreen
    LaunchedEffect(Unit) {
        val inferenceModel = InferenceModel.getInstance(context)
        chatViewModel.resetInferenceModel(inferenceModel)
        chatViewModel.clearChat()
    }

    val uiState by chatViewModel.uiState.collectAsStateWithLifecycle()
    val textInputEnabled by chatViewModel.isTextInputEnabled.collectAsStateWithLifecycle()
    ChatScreen(
        context,
        uiState,
        textInputEnabled,
        remainingTokens = chatViewModel.tokensRemaining,
        resetTokenCount = {
            chatViewModel.recomputeSizeInTokens("")
        },
        onSendMessage = { message ->
            chatViewModel.sendMessage(message)
        },
        onChangedMessage = { message ->
            chatViewModel.recomputeSizeInTokens(message)
        },
        onClose = onClose
    )
}

@Composable
fun ChatScreen(
    context: Context,
    uiState: UiState,
    textInputEnabled: Boolean,
    remainingTokens: StateFlow<Int>,
    resetTokenCount: () -> Unit,
    onSendMessage: (String) -> Unit,
    onChangedMessage: (String) -> Unit,
    onClose: () -> Unit
) {
    var userMessage by rememberSaveable { mutableStateOf("") }
    val tokens by remainingTokens.collectAsState(initial = -1)
    val chatViewModel: ChatViewModel = viewModel(factory = ChatViewModel.getFactory(context))
    val isRecording by chatViewModel.isRecording.collectAsState()
    val recognizedText by chatViewModel.recognizedText.collectAsState()
    val showPermissionRequest by chatViewModel.showPermissionRequest.collectAsState()
    val isGeneratingResponse = uiState.messages.any { it.isLoading }
    
    // Add states for dialogs
    var showExitConfirmation by remember { mutableStateOf(false) }
    var showSaveOptions by remember { mutableStateOf(false) }
    val textFileStorage = remember { TextFileStorage(context) }
    
    // Update userMessage when recognizedText changes
    LaunchedEffect(recognizedText) {
        if (recognizedText.isNotEmpty()) {
            userMessage = recognizedText
            onChangedMessage(recognizedText)
        }
    }

    // Find the nearest activity context
    val activityContext = LocalContext.current
    val activity = when (activityContext) {
        is Activity -> activityContext
        else -> null
    }

    // Handle exit with save options
    fun handleExit() {
        if (uiState.messages.isNotEmpty()) {
            showExitConfirmation = true
        } else {
            onClose()
        }
    }

    // Save chat to local storage
    fun saveToLocal() {
        val content = uiState.messages.asReversed().joinToString("\n\n") { message ->
            "${if (message.isFromUser) "User" else "AI"}: ${message.message}"
        }
        textFileStorage.saveTextToFile(content)
        onClose()
    }

    // Save chat to cloud
    fun saveToCloud() {
        try {
            // Start a new conversation
            textFileStorage.startNewConversation()
            // Append all messages in reversed order
            uiState.messages.asReversed().forEach { message ->
                textFileStorage.appendToConversation(
                    if (message.isFromUser) "User" else "AI",
                    message.message
                )
            }
            Log.d("ChatScreen", "Saved conversation to cloud")
        } catch (e: Exception) {
            Log.e("ChatScreen", "Error saving chat to cloud", e)
        }
        onClose()
    }

    // Exit Confirmation Dialog
    if (showExitConfirmation) {
        AlertDialog(
            onDismissRequest = { showExitConfirmation = false },
            title = { Text("Save Chat?") },
            text = { Text("Would you like to save this conversation before exiting?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showExitConfirmation = false
                        showSaveOptions = true
                    }
                ) {
                    Text("Yes")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showExitConfirmation = false
                        onClose()
                    }
                ) {
                    Text("No")
                }
            }
        )
    }

    // Save Options Dialog
    if (showSaveOptions) {
        AlertDialog(
            onDismissRequest = { showSaveOptions = false },
            title = { Text("Save Location") },
            text = { Text("Where would you like to save this conversation?") },
            confirmButton = {
                if (textFileStorage.isUserLoggedIn()) {
                    TextButton(
                        onClick = {
                            showSaveOptions = false
                            saveToCloud()
                        }
                    ) {
                        Text("Cloud")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showSaveOptions = false
                        saveToLocal()
                    }
                ) {
                    Text("Local")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.Bottom
    ) {
        // Top bar with close button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = InferenceModel.model.toString(),
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = if (tokens >= 0) "$tokens ${stringResource(R.string.tokens_remaining)}" else "",
                style = MaterialTheme.typography.titleSmall
            )
            Row {
                if (isGeneratingResponse) {
                    IconButton(
                        onClick = { chatViewModel.stopInference() }
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Stop Response")
                    }
                }
                IconButton(
                    onClick = {
                        InferenceModel.getInstance(context).resetSession()
                        chatViewModel.clearChat()
                        resetTokenCount()
                    },
                    enabled = textInputEnabled
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Clear Chat")
                }

                IconButton(
                    onClick = { handleExit() },
                    enabled = textInputEnabled
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close Chat")
                }
            }
        }

        if (tokens == 0) {
            // Show warning label that context is full
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.LightGray)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.context_full_message),
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.Red,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            reverseLayout = true
        ) {
            items(uiState.messages) { chat ->
                ChatItem(chat)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Microphone button
            IconButton(
                onClick = {
                    if (isRecording) {
                        chatViewModel.stopSpeechRecognition()
                    } else if (activity != null) {
                        chatViewModel.startSpeechRecognition(activity)
                    }
                },
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                Icon(
                    imageVector = if (isRecording) Icons.Default.MicOff else Icons.Default.Mic,
                    contentDescription = if (isRecording) "Stop Recording" else "Start Recording",
                    tint = if (isRecording) Color.Red else MaterialTheme.colorScheme.primary
                )
            }

            TextField(
                value = userMessage,
                onValueChange = { userMessage = it
                    if (!userMessage.contains(" ") || userMessage.trim() != userMessage) {
                        onChangedMessage(userMessage)
                    }
                },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                ),
                label = { Text("Type a message") },
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                enabled = textInputEnabled && !isRecording
            )

            IconButton(
                onClick = {
                    if (userMessage.isNotEmpty()) {
                        onSendMessage(userMessage)
                        userMessage = ""
                    }
                },
                enabled = textInputEnabled && userMessage.isNotEmpty() && !isRecording
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send"
                )
            }
        }
    }
}

@Composable
fun ChatItem(
    chatMessage: ChatMessage
) {
    val backgroundColor = if (chatMessage.isFromUser) {
        MaterialTheme.colorScheme.tertiaryContainer
    } else if (chatMessage.isThinking) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.secondaryContainer
    }

    val bubbleShape = if (chatMessage.isFromUser) {
        RoundedCornerShape(20.dp, 4.dp, 20.dp, 20.dp)
    } else {
        RoundedCornerShape(4.dp, 20.dp, 20.dp, 20.dp)
    }

    val horizontalAlignment = if (chatMessage.isFromUser) {
        Alignment.End
    } else {
        Alignment.Start
    }

    // Get a reference to the chat view model to control TTS
    val chatViewModel = viewModel<ChatViewModel>(factory = ChatViewModel.getFactory(LocalContext.current))
    val isSpeaking by chatViewModel.isSpeaking.collectAsState()
    
    // This tracks if this specific message is being spoken
    var isThisMessageSpeaking by remember { mutableStateOf(false) }

    Column(
        horizontalAlignment = horizontalAlignment,
        modifier = Modifier
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .fillMaxWidth()
    ) {
        val author = if (chatMessage.isFromUser) {
            stringResource(R.string.user_label)
        } else if (chatMessage.isThinking) {
            stringResource(R.string.thinking_label)
        } else {
            stringResource(R.string.model_label)
        }
        Text(
            text = author,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = if (chatMessage.isFromUser) Arrangement.End else Arrangement.Start
        ) {
            BoxWithConstraints {
                Card(
                    colors = CardDefaults.cardColors(containerColor = backgroundColor),
                    shape = bubbleShape,
                    modifier = Modifier.widthIn(0.dp, maxWidth * 0.85f)
                ) {
                    if (chatMessage.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(16.dp)
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                        Text(
                            text = chatMessage.message,
                                modifier = Modifier
                                    .padding(16.dp)
                                    .weight(1f)
                            )
                            
                            // Only show TTS button for AI messages
                            if (!chatMessage.isFromUser && !chatMessage.isThinking && !chatMessage.isLoading) {
                                IconButton(
                                    onClick = {
                                        // If this message is already speaking, stop it
                                        if (isThisMessageSpeaking) {
                                            chatViewModel.stopSpeaking()
                                            isThisMessageSpeaking = false
                                        } else {
                                            // If something else was speaking, stop it first
                                            if (isSpeaking) {
                                                chatViewModel.stopSpeaking()
                                            }
                                            // Start speaking this message
                                            isThisMessageSpeaking = chatViewModel.toggleTextToSpeech(chatMessage.message)
                                        }
                                    },
                                    modifier = Modifier
                                        .padding(8.dp)
                                        .align(Alignment.Top)
                                ) {
                                    Icon(
                                        imageVector = if (isThisMessageSpeaking) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                        contentDescription = if (isThisMessageSpeaking) "Stop Speaking" else "Read Aloud",
                                        tint = if (isThisMessageSpeaking) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
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
