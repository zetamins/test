package com.stalkerhek.domain.repository

import com.stalkerhek.domain.entity.Profile
import com.stalkerhek.domain.entity.Channel

interface ProfileRepository {
    suspend fun getAllProfiles(): List<Profile>
    suspend fun getProfile(id: String): Profile?
    suspend fun saveProfile(profile: Profile)
    suspend fun deleteProfile(id: String)
    suspend fun updateProfile(profile: Profile)
}

interface StalkerRepository {
    suspend fun authenticate(portalUrl: String, login: String, password: String, settings: com.stalkerhek.domain.entity.ProfileSettings): Result<com.stalkerhek.domain.entity.StalkerSession>
    suspend fun getCategories(portalUrl: String, session: com.stalkerhek.domain.entity.StalkerSession): Result<List<Category>>
    suspend fun getChannels(portalUrl: String, session: com.stalkerhek.domain.entity.StalkerSession, categoryId: Int? = null): Result<List<Channel>>
    suspend fun createLink(portalUrl: String, session: com.stalkerhek.domain.entity.StalkerSession, channelId: Int, cmd: String): Result<String>
    suspend fun keepAlive(portalUrl: String, session: com.stalkerhek.domain.entity.StalkerSession): Result<Boolean>
    suspend fun handshake(portalUrl: String, settings: com.stalkerhek.domain.entity.ProfileSettings): Result<String>
}

interface UserRepository {
    suspend fun getAllUsers(): List<com.stalkerhek.domain.entity.User>
    suspend fun getUser(username: String): com.stalkerhek.domain.entity.User?
    suspend fun saveUser(user: com.stalkerhek.domain.entity.User)
    suspend fun deleteUser(id: String)
    suspend fun updateUser(user: com.stalkerhek.domain.entity.User)
}

interface FilterRepository {
    suspend fun getFiltersByProfile(profileId: String): List<com.stalkerhek.domain.entity.Filter>
    suspend fun saveFilter(filter: com.stalkerhek.domain.entity.Filter)
    suspend fun deleteFilter(id: String)
}

data class Category(
    val id: Int,
    val name: String,
    val title: String
)