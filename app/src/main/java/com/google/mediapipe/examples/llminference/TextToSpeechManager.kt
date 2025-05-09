package com.google.mediapipe.examples.llminference

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale
import java.util.UUID

/**
 * Manager class for Text-to-Speech functionality.
 * Works offline and handles reading AI responses aloud.
 */
class TextToSpeechManager(context: Context) {
    private val TAG = "TextToSpeechManager"
    private var textToSpeech: TextToSpeech? = null
    private var isInitialized = false
    private var isSpeaking = false
    
    init {
        // Initialize TTS engine
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                // Set language to device default
                val result = textToSpeech?.setLanguage(Locale.getDefault())
                
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e(TAG, "Language not supported")
                } else {
                    isInitialized = true
                    Log.d(TAG, "TTS initialized successfully")
                    
                    // Set utterance progress listener
                    textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {
                            isSpeaking = true
                            Log.d(TAG, "TTS started speaking: $utteranceId")
                        }
                        
                        override fun onDone(utteranceId: String?) {
                            isSpeaking = false
                            Log.d(TAG, "TTS finished speaking: $utteranceId")
                        }
                        
                        @Deprecated("Deprecated in Java")
                        override fun onError(utteranceId: String?) {
                            isSpeaking = false
                            Log.e(TAG, "TTS error: $utteranceId")
                        }
                    })
                }
            } else {
                Log.e(TAG, "TTS initialization failed with status: $status")
            }
        }
    }
    
    /**
     * Speak the given text aloud.
     * @param text The text to be spoken
     * @return True if speech started successfully, false otherwise
     */
    fun speak(text: String): Boolean {
        if (!isInitialized) {
            Log.e(TAG, "TTS not initialized")
            return false
        }
        
        // Generate a unique utterance ID
        val utteranceId = UUID.randomUUID().toString()
        
        // Use the speak method with QUEUE_FLUSH to stop any current speech
        textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        
        return true
    }
    
    /**
     * Stop any ongoing speech.
     */
    fun stop() {
        if (isInitialized && isSpeaking) {
            textToSpeech?.stop()
            isSpeaking = false
        }
    }
    
    /**
     * Toggle between speaking and silence.
     * @param text The text to speak if starting speech
     * @return True if now speaking, false if stopped
     */
    fun toggle(text: String): Boolean {
        return if (isSpeaking) {
            stop()
            false
        } else {
            speak(text)
            true
        }
    }
    
    /**
     * Check if TTS is currently speaking.
     * @return True if speaking, false otherwise
     */
    fun isSpeaking(): Boolean {
        return isSpeaking
    }
    
    /**
     * Release TTS resources when no longer needed.
     */
    fun shutdown() {
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        textToSpeech = null
        isInitialized = false
        isSpeaking = false
    }
} 