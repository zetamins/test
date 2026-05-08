package com.stalkerhek.domain.entity

import kotlinx.serialization.Serializable

@Serializable
data class Profile(
    val id: String = generateProfileId(),
    val name: String,
    val portalUrl: String,
    val enabled: Boolean = true,
    val settings: ProfileSettings = ProfileSettings(),
    val channels: List<Channel> = emptyList()
)

@Serializable
data class ProfileSettings(
    val httpPort: Int = 7700,
    val hlsPort: Int = 7770,
    val username: String = "",
    val password: String = "",
    val macAddress: String = "00:11:22:33:44:55",
    val deviceId: String = "Linux拳头",
    val deviceId2: String = "PC Browser",
    val signature: String = "portal.infomir.com.ua",
    val model: String = "MAG250",
    val serialNumber: String = "000000000000",
    val mac: String = "00:11:22:33:44:55",
    val timezone: String = "Europe/Kiev",
    val filters: ProfileFilters = ProfileFilters()
)

@Serializable
data class ProfileFilters(
    val allowedCategories: List<String> = emptyList(),
    val blockedCategories: List<String> = emptyList(),
    val allowedChannels: List<String> = emptyList(),
    val blockedChannels: List<String> = emptyList()
)

@Serializable
data class Channel(
    val id: Int,
    val name: String,
    val cmd: String,
    val type: Int = 1,
    val logo: String = ""
)

private fun generateProfileId(): String {
    return java.util.UUID.randomUUID().toString()
}