package com.example.contactmanager.model

import java.util.Date

data class CallLog(
    val id: String,
    val phoneNumber: String,
    val contactName: String?,
    val date: Date,
    val duration: Long,
    val type: CallType
)

enum class CallType {
    INCOMING,
    OUTGOING,
    MISSED
} 