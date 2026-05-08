package com.stalkerhek.domain.entity

data class StalkerSession(
    val profileId: String = "",
    val token: String,
    val realToken: String = "",
    val mac: String = "",
    val deviceId: String = "",
    val deviceId2: String = "",
    val signature: String = "",
    val userLogin: String = "",
    val userPassword: String = "",
    val expiresAt: Long = 0L
)