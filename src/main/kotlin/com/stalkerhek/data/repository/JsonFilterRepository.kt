package com.stalkerhek.data.repository

import com.stalkerhek.domain.entity.Filter
import com.stalkerhek.domain.repository.FilterRepository
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.concurrent.ConcurrentHashMap

class JsonFilterRepository(private val filePath: String = "filters.json") : FilterRepository {
    private val filters = ConcurrentHashMap<String, Filter>()
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    init {
        loadFilters()
    }

    private fun loadFilters() {
        val file = File(filePath)
        if (file.exists()) {
            try {
                val content = file.readText()
                val list = json.decodeFromString<List<Filter>>(content)
                list.forEach { filters[it.id] = it }
            } catch (e: Exception) {
            }
        }
    }

    private fun saveFilters() {
        val content = json.encodeToString(filters.values.toList())
        File(filePath).writeText(content)
    }

    override suspend fun getFiltersByProfile(profileId: String): List<Filter> {
        return filters.values.filter { it.profileId == profileId }
    }

    override suspend fun saveFilter(filter: Filter) {
        filters[filter.id] = filter
        saveFilters()
    }

    override suspend fun deleteFilter(id: String) {
        filters.remove(id)
        saveFilters()
    }
}
