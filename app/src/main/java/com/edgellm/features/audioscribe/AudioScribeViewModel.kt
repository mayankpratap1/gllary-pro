package com.edgellm.features.audioscribe

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.edgellm.data.ChatRepository
import com.edgellm.engine.InferenceEngine
import com.edgellm.features.chat.ChatMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.*

class AudioScribeViewModel(private val repository: ChatRepository) : ViewModel() {
    private val _state = MutableStateFlow(AudioScribeState())
    val state: StateFlow<AudioScribeState> = _state

    var engineRef: InferenceEngine? = null
    private var speechRecognizer: SpeechRecognizer? = null

    fun onFilePicked(context: Context, uri: Uri) {
        _state.value = _state.value.copy(isProcessing = true, error = null)
        viewModelScope.launch {
            try {
                // In an enterprise app, we'd use a library like Whisper or Opus decoder.
                // For this prototype, we simulate the transcription of the song/file.
                val fileName = uri.path?.substringAfterLast("/") ?: "audio file"
                val transcription = "[PROCESSED FILE: $fileName] This audio file contains spoken content or music. The AI is analyzing the pattern..."
                
                _state.value = _state.value.copy(transcript = transcription)
                
                // Automatically send to AI
                processWithAI(transcription)
            } catch (e: Exception) {
                _state.value = _state.value.copy(isProcessing = false, error = "File error: ${e.message}")
            }
        }
    }

    fun startRecording(context: Context) {
        if (speechRecognizer == null) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    _state.value = _state.value.copy(isRecording = true, error = null)
                }
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {
                    _state.value = _state.value.copy(isRecording = false, isProcessing = true)
                }
                override fun onError(error: Int) {
                    _state.value = _state.value.copy(isRecording = false, isProcessing = false, error = "Speech error: $error")
                }
                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val text = matches?.get(0) ?: ""
                    _state.value = _state.value.copy(transcript = text, isProcessing = true)
                    processWithAI(text)
                }
                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        }
        speechRecognizer?.startListening(intent)
    }

    private fun processWithAI(text: String) {
        val engine = engineRef ?: return
        
        viewModelScope.launch {
            try {
                val sessionId = repository.createNewSession("Audio Chat")
                repository.saveMessage(sessionId, ChatMessage("user", "[Audio] $text"))
                
                val response = engine.generate(text)
                _state.value = _state.value.copy(transcript = "AI Analysis: $response", isProcessing = false)
                
                repository.saveMessage(sessionId, ChatMessage("assistant", response))
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = "AI Error: ${e.message}", isProcessing = false)
            }
        }
    }

    fun stopRecording() {
        speechRecognizer?.stopListening()
    }

    override fun onCleared() {
        super.onCleared()
        speechRecognizer?.destroy()
    }
}

data class AudioScribeState(
    val isRecording: Boolean = false,
    val isProcessing: Boolean = false,
    val transcript: String? = null,
    val error: String? = null
)
