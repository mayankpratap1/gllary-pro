package com.edgellm.data

import com.edgellm.data.db.ChatDao
import com.edgellm.data.db.ChatMessageEntity
import com.edgellm.data.db.ChatSession
import com.edgellm.features.chat.ChatMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Enterprise-grade repository using Room for persistent chat history.
 */
class ChatRepository(private val chatDao: ChatDao) {

    /** Returns all chat sessions sorted by date. */
    val sessions: Flow<List<ChatSession>> = chatDao.getSessions()

    /** Returns messages for a specific session. */
    fun getMessages(sessionId: Long): Flow<List<ChatMessage>> {
        return chatDao.getMessagesForSession(sessionId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    /** Create a new session and return its ID. */
    suspend fun createNewSession(title: String): Long {
        val session = ChatSession(title = title)
        return chatDao.insertSession(session)
    }

    /** Save a message to the database. */
    suspend fun saveMessage(sessionId: Long, message: ChatMessage) {
        chatDao.insertMessage(message.toEntity(sessionId))
        // Update session's lastUpdated time
        val session = ChatSession(id = sessionId, title = "Updated Chat", lastUpdated = System.currentTimeMillis())
        chatDao.updateSession(session)
    }

    /** Delete a session and all its messages. */
    suspend fun deleteSession(sessionId: Long) {
        chatDao.deleteMessagesForSession(sessionId)
        chatDao.deleteSession(sessionId)
    }

    // --- Mappers ---
    private fun ChatMessageEntity.toDomain() = ChatMessage(
        role = role,
        content = content,
        thinkingContent = thinkingContent
    )

    private fun ChatMessage.toEntity(sessionId: Long) = ChatMessageEntity(
        sessionId = sessionId,
        role = role,
        content = content,
        thinkingContent = thinkingContent
    )
}
