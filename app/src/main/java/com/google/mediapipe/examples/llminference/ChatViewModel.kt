package com.google.mediapipe.examples.llminference

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.max

class ChatViewModel(
    private var inferenceModel: InferenceModel,
    private val speechRecognitionService: SpeechRecognitionService,
    private val textFileStorage: TextFileStorage,
    private val permissionHandler: PermissionHandler,
    private val textToSpeechManager: TextToSpeechManager
) : ViewModel() {

    private val _uiState: MutableStateFlow<UiState> = MutableStateFlow(inferenceModel.uiState)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _tokensRemaining = MutableStateFlow(-1)
    val tokensRemaining: StateFlow<Int> = _tokensRemaining.asStateFlow()

    private val _textInputEnabled: MutableStateFlow<Boolean> = MutableStateFlow(true)
    val isTextInputEnabled: StateFlow<Boolean> = _textInputEnabled.asStateFlow()

    private val _isRecording: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _recognizedText: MutableStateFlow<String> = MutableStateFlow("")
    val recognizedText: StateFlow<String> = _recognizedText.asStateFlow()

    private val _showPermissionRequest: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val showPermissionRequest: StateFlow<Boolean> = _showPermissionRequest.asStateFlow()

    private val _isSpeaking: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private var currentInferenceJob: kotlinx.coroutines.Job? = null

    fun resetInferenceModel(newModel: InferenceModel) {
        inferenceModel = newModel
        _uiState.value = inferenceModel.uiState
    }

    fun startSpeechRecognition(activity: Activity) {
        Log.d("ChatViewModel", "Starting speech recognition")
        if (permissionHandler.hasRecordAudioPermission()) {
            Log.d("ChatViewModel", "Audio permission granted, starting recognition")
            _isRecording.value = true
            speechRecognitionService.startListening { text ->
                Log.d("ChatViewModel", "Speech recognition result: $text")
                _recognizedText.value = text
                _isRecording.value = false
            }
        } else {
            Log.d("ChatViewModel", "Requesting audio permission")
            _showPermissionRequest.value = true
            permissionHandler.requestRecordAudioPermission(activity)
        }
    }

    fun stopSpeechRecognition() {
        Log.d("ChatViewModel", "Stopping speech recognition")
        _isRecording.value = false
        speechRecognitionService.stopListening()
    }

    fun onPermissionResult(granted: Boolean) {
        Log.d("ChatViewModel", "Permission result: $granted")
        _showPermissionRequest.value = false
        if (granted) {
            _isRecording.value = true
            speechRecognitionService.startListening { text ->
                Log.d("ChatViewModel", "Speech recognition result after permission: $text")
                _recognizedText.value = text
                _isRecording.value = false
            }
        }
    }

    fun toggleTextToSpeech(text: String): Boolean {
        val isSpeakingNow = textToSpeechManager.toggle(text)
        _isSpeaking.value = isSpeakingNow
        return isSpeakingNow
    }

    fun stopSpeaking() {
        textToSpeechManager.stop()
        _isSpeaking.value = false
    }

    fun sendMessage(userMessage: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value.addMessage(userMessage, USER_PREFIX)
            _uiState.value.createLoadingMessage()
            setInputEnabled(false)
            try {
                currentInferenceJob = viewModelScope.launch(Dispatchers.IO) {
                val asyncInference = inferenceModel.generateResponseAsync(userMessage, { partialResult, done ->
                    _uiState.value.appendMessage(partialResult, done)
                    if (done) {
                        setInputEnabled(true)
                            currentInferenceJob = null
                    } else {
                        _tokensRemaining.update { max(0, it - 1) }
                    }
                })
                asyncInference.addListener({
                    viewModelScope.launch(Dispatchers.IO) {
                        recomputeSizeInTokens(userMessage)
                    }
                }, Dispatchers.Main.asExecutor())
                }
            } catch (e: Exception) {
                val errorMessage = e.localizedMessage ?: "Unknown Error"
                _uiState.value.addMessage(errorMessage, MODEL_PREFIX)
                setInputEnabled(true)
                currentInferenceJob = null
            }
        }
    }

    fun stopInference() {
        currentInferenceJob?.cancel()
        currentInferenceJob = null
        setInputEnabled(true)
        _uiState.value.removeLoadingMessage()
    }

    private fun setInputEnabled(isEnabled: Boolean) {
        _textInputEnabled.value = isEnabled
    }

    fun recomputeSizeInTokens(message: String) {
        val remainingTokens = inferenceModel.estimateTokensRemaining(message)
        _tokensRemaining.value = remainingTokens
    }

    fun clearChat() {
        _uiState.value.clearMessages()
    }

    override fun onCleared() {
        super.onCleared()
        speechRecognitionService.destroy()
        textToSpeechManager.shutdown()
    }

    companion object {
        fun getFactory(context: Context) = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val inferenceModel = InferenceModel.getInstance(context)
                val speechRecognitionService = SpeechRecognitionService(context)
                val textFileStorage = TextFileStorage(context)
                val permissionHandler = PermissionHandler(context)
                val textToSpeechManager = TextToSpeechManager(context)
                return ChatViewModel(
                    inferenceModel, 
                    speechRecognitionService, 
                    textFileStorage, 
                    permissionHandler,
                    textToSpeechManager
                ) as T
            }
        }
    }
}
