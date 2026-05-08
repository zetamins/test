package com.stalkerhek.data.repository

import com.stalkerhek.domain.entity.User
import com.stalkerhek.domain.repository.UserRepository
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.concurrent.ConcurrentHashMap

class JsonUserRepository(private val filePath: String = "users.json") : UserRepository {
    private val users = ConcurrentHashMap<String, User>()
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    init {
        loadUsers()
    }

    private fun loadUsers() {
        val file = File(filePath)
        if (file.exists()) {
            try {
                val content = file.readText()
                val list = json.decodeFromString<List<User>>(content)
                list.forEach { users[it.id] = it }
            } catch (e: Exception) {
            }
        }
    }

    private fun saveUsers() {
        val content = json.encodeToString(users.values.toList())
        File(filePath).writeText(content)
    }

    override suspend fun getAllUsers(): List<User> = users.values.toList()

    override suspend fun getUser(username: String): User? = users.values.find { it.username == username }

    override suspend fun saveUser(user: User) {
        users[user.id] = user
        saveUsers()
    }

    override suspend fun deleteUser(id: String) {
        users.remove(id)
        saveUsers()
    }

    override suspend fun updateUser(user: User) {
        users[user.id] = user
        saveUsers()
    }
}
