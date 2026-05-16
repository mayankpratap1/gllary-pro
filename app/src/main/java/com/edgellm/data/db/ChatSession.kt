package com.edgellm.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_sessions")
data class ChatSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val lastUpdated: Long = System.currentTimeMillis(),
    val engineType: String? = null // "LiteRT" or "GGUF"
)
