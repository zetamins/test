package com.stalkerhek.domain.entity

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: String = generateUserId(),
    val username: String,
    val passwordHash: String,
    val role: UserRole = UserRole.USER,
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
enum class UserRole {
    ADMIN,
    USER
}

private fun generateUserId(): String {
    return java.util.UUID.randomUUID().toString()
}