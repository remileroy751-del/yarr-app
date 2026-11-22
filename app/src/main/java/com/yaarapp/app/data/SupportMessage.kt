package com.yaarapp.app.data

data class SupportMessage(
    val id: String,
    val userUid: String,
    val message: String,
    val status: String,
    val adminReply: String?,
    val createdAt: Long,
    val repliedAt: Long?
)
