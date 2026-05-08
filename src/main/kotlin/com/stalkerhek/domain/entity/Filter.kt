package com.stalkerhek.domain.entity

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class Filter(
    val id: String = UUID.randomUUID().toString(),
    val profileId: String,
    val name: String,
    val type: FilterType = FilterType.ALLOWED,
    val categoryIds: List<Int> = emptyList(),
    val channelIds: List<Int> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
enum class FilterType {
    ALLOWED,
    BLOCKED
}

@Serializable
data class FilterStore(
    val filters: List<Filter> = emptyList()
)