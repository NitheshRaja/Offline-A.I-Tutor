package com.google.mediapipe.examples.llminference

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun CustomerCareChat(
    onClose: () -> Unit,
    chatViewModel: ChatViewModel? = null
) {
    BackHandler(enabled = true) {
        chatViewModel?.clearChat()
        onClose()
    }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var chatStarted by remember { mutableStateOf(false) }

    // First AI welcome message
    LaunchedEffect(Unit) {
        if (!chatStarted) {
            chatViewModel?.uiState?.value?.addMessage(
                "Hello! Welcome to customer care. How can I assist you today?",
                MODEL_PREFIX
            )
            chatStarted = true
        }
    }

    fun handleUserMessage(message: String) {
        chatViewModel?.uiState?.value?.addMessage(message, USER_PREFIX)

        val offTopicIndicators = listOf(
            "interview", "job", "hiring", "vacancy", "position", "resume", "salary", "recruiter",
            "weather", "joke", "news", "date", "time", "who are you", "how old", "where are you",
            "play game", "marry", "love", "do you", "can you", "what is", "how to", "bathroom", "hungry",
            "fuck you", "bit", "porn", "sex", "kill"
        )

        val isOffTopic = offTopicIndicators.any { message.lowercase().contains(it) }

        if (isOffTopic) {
            chatViewModel?.uiState?.value?.addMessage(
                "This chat is for customer care support. Let's focus on your product or service issues. How can I assist you today?",
                MODEL_PREFIX
            )
        } else {
            val customerPrompt = """
                You are a helpful, friendly customer care assistant.
                The customer said: "$message"
                Provide a useful response and ask one follow-up question to better understand or resolve the concern.
                Avoid system tags or instructions. Stay concise and professional.
            """.trimIndent()

            // 👇 Send hidden prompt
            chatViewModel?.sendMessage(customerPrompt, skipUi = true)
        }
    }

    val uiState = chatViewModel?.uiState?.collectAsState()?.value
    uiState?.let {
        ChatScreen(
            context = context,
            uiState = it,
            textInputEnabled = true,
            remainingTokens = chatViewModel.tokensRemaining,
            resetTokenCount = { chatViewModel.recomputeSizeInTokens("") },
            onSendMessage = { handleUserMessage(it) },
            onChangedMessage = { chatViewModel.recomputeSizeInTokens(it) },
            onClose = onClose
        )
    }

    DisposableEffect(Unit) {
        onDispose { }
    }
}
