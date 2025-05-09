package com.google.mediapipe.examples.llminference

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
import androidx.compose.ui.graphics.Color

enum class RoleplayScenario {
    INTERVIEWER,
    CUSTOMER_CARE,
    PROFESSIONAL_MEETING
}

@Composable
internal fun RoleplaySelectionRoute(
    onScenarioSelected: (RoleplayScenario) -> Unit = {}
) {
    val scenarios = listOf(
        RoleplayScenario.INTERVIEWER,
        RoleplayScenario.CUSTOMER_CARE,
        RoleplayScenario.PROFESSIONAL_MEETING
    )
    val scenarioTitles = listOf("Interviewer", "Customer Care", "Professional Meeting")
    val scenarioSubtitles = listOf(
        "Practice job interviews",
        "Handle customer queries",
        "Simulate meetings"
    )
    val scenarioColors = listOf(
        Color(0xFF42A5F5), // Blue
        Color(0xFFFFA726), // Orange
        Color(0xFF66BB6A)  // Green
    )
    val scenarioImages = listOf(
        R.drawable.intervew, // Interviewer
        R.drawable.customercare, // Customer Care
        R.drawable.professionalmeeting // Professional Meeting
    )
    val visibleStates = remember { scenarios.map { mutableStateOf(false) } }
    LaunchedEffect(Unit) {
        scenarios.indices.forEach { i ->
            delay(120L * i)
            visibleStates[i].value = true
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        scenarios.forEachIndexed { i, scenario ->
            AnimatedVisibility(
                visible = visibleStates[i].value,
                enter = fadeIn() + slideInVertically(initialOffsetY = { -40 }),
                exit = fadeOut()
            ) {
                SelectionCard(
                    title = scenarioTitles[i],
                    subtitle = scenarioSubtitles[i],
                    imageRes = scenarioImages[i],
                    backgroundColor = scenarioColors[i],
                    onClick = { onScenarioSelected(scenario) }
        )
            }
            if (i < scenarios.lastIndex) {
        Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun RoleplayButton(
    text: String,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
    ) {
        Text(text = text)
    }
} 