package com.stalkerhek.domain.usecase

import com.stalkerhek.domain.entity.Channel
import com.stalkerhek.domain.entity.ProfileFilters
import com.stalkerhek.domain.repository.FilterRepository

class FilterChannelsUseCase(
    private val filterRepository: FilterRepository
) {
    fun filterChannels(channels: List<Channel>, filters: ProfileFilters): List<Channel> {
        var result = channels

        if (filters.allowedCategories.isNotEmpty()) {
            result = result.filter { filters.allowedCategories.contains(it.name) || it.id in getCategoryChannelIds(filters.allowedCategories) }
        }

        if (filters.blockedCategories.isNotEmpty()) {
            result = result.filter { !filters.blockedCategories.contains(it.name) && it.id !in getCategoryChannelIds(filters.blockedCategories) }
        }

        if (filters.allowedChannels.isNotEmpty()) {
            val allowedIds = filters.allowedChannels.mapNotNull { it.toIntOrNull() }
            result = result.filter { it.id in allowedIds }
        }

        if (filters.blockedChannels.isNotEmpty()) {
            val blockedIds = filters.blockedChannels.mapNotNull { it.toIntOrNull() }
            result = result.filter { it.id !in blockedIds }
        }

        return result
    }

    private fun getCategoryChannelIds(categories: List<String>): List<Int> {
        return emptyList()
    }

    suspend fun getFiltersByProfile(profileId: String) = filterRepository.getFiltersByProfile(profileId)
    suspend fun saveFilter(filter: com.stalkerhek.domain.entity.Filter) = filterRepository.saveFilter(filter)
    suspend fun deleteFilter(id: String) = filterRepository.deleteFilter(id)
}