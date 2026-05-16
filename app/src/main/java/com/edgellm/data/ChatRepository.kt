package com.edgellm.data

import com.edgellm.features.chat.ChatMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Singleton Repository to persist chat history across screen transitions.
 * In a corporate app, this would be backed by Room or DataStore.
 */
object ChatRepository {
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages

    fun addMessage(message: ChatMessage) {
        _messages.value = _messages.value + message
    }

    fun updateLastMessage(message: ChatMessage) {
        if (_messages.value.isNotEmpty()) {
            val newList = _messages.value.toMutableList()
            newList[newList.size - 1] = message
            _messages.value = newList
        }
    }

    fun clear() {
        _messages.value = emptyList()
    }
}
