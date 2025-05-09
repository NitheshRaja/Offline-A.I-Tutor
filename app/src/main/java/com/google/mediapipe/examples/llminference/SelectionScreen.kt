package com.google.mediapipe.examples.llminference

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.width

@Composable
internal fun SelectionRoute(
    onModelSelected: () -> Unit = {},
) {
    val models = Model.entries
    val modelTitles = models.map { it.name }
    val modelSubtitles = models.map { null } // Remove 'Select this model' subtitle
    val modelColors = listOf(
        Color(0xFF00B8D4), // Cyan
        Color(0xFF8BC34A), // Light Green
        Color(0xFFFF7043), // Deep Orange
        Color(0xFF7C4DFF), // Deep Purple
        Color(0xFFFFC107), // Amber
        Color(0xFF009688), // Teal
        Color(0xFFEC407A), // Pink
        Color(0xFF43A047)  // Green
    )
    val modelImages = listOf(
        R.drawable.gemma, // GEMMA3_CPU
        R.drawable.gemma, // GEMMA3_GPU
        R.drawable.deepseek, // DEEPSEEK_CPU
        R.drawable.model_gears // PHI4_CPU
    )
    val modelCaptions = listOf(
        "Gemma 3B CPU: Fast and efficient for most tasks.",
        "Gemma 3B GPU: Accelerated performance on GPU.",
        "DeepSeek CPU: Distilled Qwen model for research.",
        "Phi-4 CPU: Lightweight and versatile."
    )
    val visibleStates = remember { models.map { mutableStateOf(false) } }
    LaunchedEffect(Unit) {
        models.indices.forEach { i ->
            delay(120L * i)
            visibleStates[i].value = true
        }
    }
    LazyColumn(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
    ) {
        items(models.size) { i ->
            AnimatedVisibility(
                visible = visibleStates[i].value,
                enter = fadeIn() + slideInVertically(initialOffsetY = { -40 }),
                exit = fadeOut()
            ) {
                SelectionCard(
                    title = modelTitles[i],
                    subtitle = modelCaptions[i], // Show caption as subtitle
                    imageRes = modelImages[i],
                    backgroundColor = modelColors[i % modelColors.size],
                onClick = {
                        InferenceModel.model = models[i]
                    onModelSelected()
                },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .padding(horizontal = 8.dp, vertical = 8.dp)
                )
            }
            if (i < models.lastIndex) {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
