package com.stalkerhek.data.repository

import com.stalkerhek.domain.entity.Profile
import com.stalkerhek.domain.repository.ProfileRepository
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.concurrent.ConcurrentHashMap

class JsonProfileRepository(private val filePath: String = "profiles.json") : ProfileRepository {
    private val profiles = ConcurrentHashMap<String, Profile>()
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    init {
        loadProfiles()
    }

    private fun loadProfiles() {
        val file = File(filePath)
        if (file.exists()) {
            try {
                val content = file.readText()
                val list = json.decodeFromString<List<Profile>>(content)
                list.forEach { profiles[it.id] = it }
            } catch (e: Exception) {
            }
        }
    }

    private fun saveProfiles() {
        val content = json.encodeToString(profiles.values.toList())
        File(filePath).writeText(content)
    }

    override suspend fun getAllProfiles(): List<Profile> = profiles.values.toList()

    override suspend fun getProfile(id: String): Profile? = profiles[id]

    override suspend fun saveProfile(profile: Profile) {
        profiles[profile.id] = profile
        saveProfiles()
    }

    override suspend fun deleteProfile(id: String) {
        profiles.remove(id)
        saveProfiles()
    }

    override suspend fun updateProfile(profile: Profile) {
        profiles[profile.id] = profile
        saveProfiles()
    }
}
