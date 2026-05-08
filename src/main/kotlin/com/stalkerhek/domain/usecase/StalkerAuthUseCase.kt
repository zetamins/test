package com.stalkerhek.domain.usecase

import com.stalkerhek.domain.entity.ProfileSettings
import com.stalkerhek.domain.entity.StalkerSession
import com.stalkerhek.domain.repository.StalkerRepository

class StalkerAuthUseCase(
    private val stalkerRepository: StalkerRepository
) {
    suspend fun handshake(portalUrl: String, settings: ProfileSettings): Result<String> {
        return stalkerRepository.handshake(portalUrl, settings)
    }

    suspend fun authenticate(
        portalUrl: String,
        login: String,
        password: String,
        settings: ProfileSettings
    ): Result<StalkerSession> {
        return stalkerRepository.authenticate(portalUrl, login, password, settings)
    }

    suspend fun keepAlive(portalUrl: String, session: StalkerSession): Result<Boolean> {
        return stalkerRepository.keepAlive(portalUrl, session)
    }

    suspend fun createLink(
        portalUrl: String,
        session: StalkerSession,
        channelId: Int,
        cmd: String
    ): Result<String> {
        return stalkerRepository.createLink(portalUrl, session, channelId, cmd)
    }
}