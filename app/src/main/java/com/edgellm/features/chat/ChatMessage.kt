package com.edgellm.features.chat

/**
 * Common data model for chat messages used across all features.
 */
data class ChatMessage(
    val role: String,
    val content: String,
    val thinkingContent: String? = null
)
