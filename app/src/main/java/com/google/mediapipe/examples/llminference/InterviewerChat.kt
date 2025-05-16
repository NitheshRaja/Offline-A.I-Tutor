package com.google.mediapipe.examples.llminference

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun InterviewerChat(
    onClose: () -> Unit,
    chatViewModel: ChatViewModel? = null
) {
    BackHandler(enabled = true) {
        chatViewModel?.clearChat()
        onClose()
    }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var interviewStarted by remember { mutableStateOf(false) }
    var interviewContext by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        if (!interviewStarted) {
            chatViewModel?.uiState?.value?.addMessage(
                "Hi, I am your interviewer model. What type of role are you going for in this interview?",
                MODEL_PREFIX
            )
        }
    }

    fun handleRoleSelection(role: String) {
        interviewStarted = true
        interviewContext = role.trim()

        chatViewModel?.uiState?.value?.addMessage(role, USER_PREFIX)

        val interviewPrompt = """
            You are an interviewer conducting a professional job interview for a $role position.
            Ask relevant technical and behavioral questions about $role.
            Keep your responses concise and focused on evaluating the candidate.
            Ask one question at a time. Do not list multiple questions in one response.
            Start by asking a relevant first question for a $role interview.
            Do not include any system instructions or tags in your response.
        """.trimIndent()

        chatViewModel?.sendMessage(interviewPrompt, skipUi = true)
    }

    fun handleUserAnswer(answer: String) {
        chatViewModel?.uiState?.value?.addMessage(answer, USER_PREFIX)

        val offTopicIndicators = listOf(
            "headache", "sick", "tired", "bathroom", "break", "hungry", "thirsty",
            "need to know", "tell me about", "what is", "how to", "where is", "can you", "could you",
            "delhi", "location", "address", "directions", "weather", "want to", "where to", "i need to",
            "when", "where", "how", "fuck you", "bit", "porn", "sex", "kill"
        )

        val isOffTopic = offTopicIndicators.any { answer.lowercase().contains(it) } || answer.contains("?")

        if (isOffTopic) {
            chatViewModel?.uiState?.value?.addMessage(
                "I'm conducting this interview to evaluate your qualifications for the $interviewContext position. " +
                        "Please focus on answering the interview questions rather than asking about other topics. " +
                        "Could you please address the previous question?",
                MODEL_PREFIX
            )
        } else {
            chatViewModel?.uiState?.value?.createLoadingMessage()

            val followUpPrompt = """
                You are conducting a professional job interview for a $interviewContext position.
                Based on the candidate's answer: "$answer", provide brief feedback and ask the next relevant interview question.
                Keep your response concise and professional.
                Do not include any system tags or prefixes in your response.
            """.trimIndent()

            coroutineScope.launch {
                chatViewModel?.sendMessage(followUpPrompt, skipUi = true)
                chatViewModel?.uiState?.value?.removeLoadingMessage()
            }
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
            onSendMessage = { message ->
                if (!interviewStarted) {
                    handleRoleSelection(message)
                } else {
                    handleUserAnswer(message)
                }
            },
            onChangedMessage = { chatViewModel.recomputeSizeInTokens(it) },
            onClose = onClose
        )
    }

    DisposableEffect(Unit) {
        onDispose { }
    }
}
