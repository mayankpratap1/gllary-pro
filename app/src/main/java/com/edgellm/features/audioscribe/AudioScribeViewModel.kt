package com.edgellm.features.audioscribe

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.lifecycle.ViewModel
import com.edgellm.engine.InferenceEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.*

class AudioScribeViewModel : ViewModel() {
    private val _state = MutableStateFlow(AudioScribeState())
    val state: StateFlow<AudioScribeState> = _state

    var engineRef: InferenceEngine? = null
    private var speechRecognizer: SpeechRecognizer? = null

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
                    _state.value = _state.value.copy(transcript = text, isProcessing = false)
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
