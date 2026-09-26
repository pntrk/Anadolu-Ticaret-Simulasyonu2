package com.example.data

import java.util.UUID

data class ConsortiumChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val projectId: String,
    val senderId: String,
    val senderName: String,
    val senderRole: String = "Konsorsiyum Ortağı",
    val messageText: String,
    val timestampMs: Long = System.currentTimeMillis(),
    val isSystemMessage: Boolean = false
) {
    companion object {
        fun fromMap(map: Map<String, Any?>, docId: String = ""): ConsortiumChatMessage {
            return ConsortiumChatMessage(
                id = if (docId.isNotEmpty()) docId else (map["id"] as? String ?: UUID.randomUUID().toString()),
                projectId = map["projectId"] as? String ?: "",
                senderId = map["senderId"] as? String ?: "",
                senderName = map["senderName"] as? String ?: "Bilinmeyen Şirket",
                senderRole = map["senderRole"] as? String ?: "Konsorsiyum Ortağı",
                messageText = map["messageText"] as? String ?: "",
                timestampMs = (map["timestampMs"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                isSystemMessage = map["isSystemMessage"] as? Boolean ?: false
            )
        }
    }

    fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "projectId" to projectId,
            "senderId" to senderId,
            "senderName" to senderName,
            "senderRole" to senderRole,
            "messageText" to messageText,
            "timestampMs" to timestampMs,
            "isSystemMessage" to isSystemMessage
        )
    }
}
