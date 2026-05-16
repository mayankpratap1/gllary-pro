package com.edgellm.features.askimage

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.edgellm.data.ChatRepository
import com.edgellm.engine.InferenceEngine
import com.edgellm.features.chat.ChatMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AskImageViewModel(private val repository: ChatRepository) : ViewModel() {
    private val _state = MutableStateFlow(AskImageState())
    val state: StateFlow<AskImageState> = _state

    var engineRef: InferenceEngine? = null
    var captureUri: Uri? = null

    fun setImage(context: Context, uri: Uri) {
        viewModelScope.launch {
            val bytes = withContext(Dispatchers.IO) {
                context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            }
            _state.value = _state.value.copy(imageUri = uri, imageBytes = bytes, result = null)
        }
    }

    fun createCaptureUri(context: Context): Uri {
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "capture_${System.currentTimeMillis()}.jpg")
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
        }
        val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        captureUri = uri
        _state.value = _state.value.copy(imageUri = uri)
        return uri ?: Uri.EMPTY
    }

    fun analyze(prompt: String) {
        val engine = engineRef ?: return
        val bytes = _state.value.imageBytes ?: return
        
        _state.value = _state.value.copy(isAnalyzing = true)
        
        viewModelScope.launch {
            try {
                // Auto-create session for vision if none exists (simplified for now)
                val sessionId = repository.createNewSession("Vision Analysis")
                repository.saveMessage(sessionId, ChatMessage("user", "[Image Sent] $prompt"))
                
                // Trigger actual engine inference
                val response = engine.generateWithImage(prompt, bytes)
                _state.value = _state.value.copy(result = response, isAnalyzing = false)
                
                // Save AI response to history
                repository.saveMessage(sessionId, ChatMessage("assistant", response))
            } catch (e: Exception) {
                _state.value = _state.value.copy(result = "Error: ${e.message}", isAnalyzing = false)
            }
        }
    }
}

data class AskImageState(
    val imageUri: Uri? = null,
    val imageBytes: ByteArray? = null,
    val isAnalyzing: Boolean = false,
    val result: String? = null
)
