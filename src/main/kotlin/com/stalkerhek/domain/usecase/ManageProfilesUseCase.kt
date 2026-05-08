package com.stalkerhek.domain.usecase

import com.stalkerhek.domain.entity.Profile
import com.stalkerhek.domain.repository.ProfileRepository
import com.stalkerhek.domain.repository.StalkerRepository

class ManageProfilesUseCase(
    private val profileRepository: ProfileRepository,
    private val stalkerRepository: StalkerRepository
) {
    suspend fun getAllProfiles(): List<Profile> {
        return profileRepository.getAllProfiles()
    }

    suspend fun getProfile(id: String): Profile? {
        return profileRepository.getProfile(id)
    }

    suspend fun createProfile(profile: Profile): Result<Profile> {
        return try {
            profileRepository.saveProfile(profile)
            Result.success(profile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateProfile(profile: Profile): Result<Profile> {
        return try {
            profileRepository.updateProfile(profile)
            Result.success(profile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteProfile(id: String): Result<Unit> {
        return try {
            profileRepository.deleteProfile(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun syncChannels(profile: Profile): Result<Profile> {
        return try {
            val sessionResult = stalkerRepository.authenticate(
                profile.portalUrl,
                profile.settings.username,
                profile.settings.password,
                profile.settings
            )

            if (sessionResult.isFailure) {
                return Result.failure(sessionResult.exceptionOrNull() ?: Exception("Authentication failed"))
            }

            val session = sessionResult.getOrThrow()
            val channelsResult = stalkerRepository.getChannels(profile.portalUrl, session)

            if (channelsResult.isFailure) {
                return Result.failure(channelsResult.exceptionOrNull() ?: Exception("Failed to fetch channels"))
            }

            val updatedProfile = profile.copy(channels = channelsResult.getOrThrow())
            profileRepository.updateProfile(updatedProfile)

            Result.success(updatedProfile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}