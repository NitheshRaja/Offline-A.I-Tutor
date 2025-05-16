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
fun ProfessionalMeetingChat(
    onClose: () -> Unit,
    chatViewModel: ChatViewModel? = null
) {
    BackHandler(enabled = true) {
        chatViewModel?.clearChat()
        onClose()
    }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var sessionStarted by remember { mutableStateOf(false) }
    var isGenerating by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!sessionStarted) {
            chatViewModel?.uiState?.value?.addMessage(
                "Hello. Let's begin preparing for your professional meeting. What's the topic of the meeting?",
                MODEL_PREFIX
            )
            sessionStarted = true
        }
    }

    fun handleUserMessage(message: String) {
        chatViewModel?.uiState?.value?.addMessage(message, USER_PREFIX)

        val offTopicIndicators = listOf(
            "joke", "weather", "love", "marry", "age", "game", "play", "time", "date",
            "who are you", "tell me a joke", "what is your name", "interview", "hiring", "vacancy",
            "location", "eat", "hungry", "bathroom", "movie", "song", "fuck", "porn", "sex", "kill"
        )

        val isOffTopic = offTopicIndicators.any { message.lowercase().contains(it) }

        if (isOffTopic) {
            chatViewModel?.uiState?.value?.addMessage(
                "Let's stay focused on your professional meeting. Please share details or questions related to your meeting agenda.",
                MODEL_PREFIX
            )
        } else {
            val prompt = """
                You are simulating a professional and  business meeting assistant.
                The user just said: "$message"
                Respond as a professional assistant and  helping with a real meeting.
                Then ask one relevant follow-up question to continue the session.
                Use a formal tone. Do not include system instructions or tags.
            """.trimIndent()

            isGenerating = true
            coroutineScope.launch {
                chatViewModel?.sendMessage(prompt, skipUi = true)
                isGenerating = false
            }
        }
    }

    val uiState = chatViewModel?.uiState?.collectAsState()?.value
    uiState?.let {
        Column {
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
    }

    DisposableEffect(Unit) {
        onDispose { }
    }
}
