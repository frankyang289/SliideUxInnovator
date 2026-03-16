package com.sliide.usermanagement.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.sliide.usermanagement.data.mapper.toDomain
import com.sliide.usermanagement.data.mapper.toEntity
import com.sliide.usermanagement.data.network.CreateUserRequest
import com.sliide.usermanagement.data.network.UserApiService
import com.sliide.usermanagement.db.UserDatabase
import com.sliide.usermanagement.domain.model.Gender
import com.sliide.usermanagement.domain.model.User
import com.sliide.usermanagement.domain.repository.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock

class UserRepositoryImpl(
    private val apiService: UserApiService,
    private val database: UserDatabase
) : UserRepository {
    private val queries = database.userQueries

    override fun getUsers(): Flow<List<User>> {
        return queries.selectAll()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { entities -> entities.map { it.toDomain() } }
    }

    override suspend fun refreshUsers(): Result<Unit> {
        return try {
            val result = apiService.getLastPageUsers()
            if (result.isSuccess) {
                val users = result.getOrThrow()
                database.transaction {
                    queries.deleteAll()
                    users.forEach { dto ->
                        queries.insertOrReplace(
                            id = dto.id,
                            name = dto.name,
                            email = dto.email,
                            gender = dto.gender,
                            status = dto.status,
                            createdAt = Clock.System.now().toString()
                        )
                    }
                }
                Result.success(Unit)
            } else {
                Result.failure(result.exceptionOrNull() ?: Exception("Unknown error"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun createUser(name: String, email: String, gender: Gender): Result<User> {
        return try {
            val request = CreateUserRequest(
                name = name,
                email = email,
                gender = gender.value
            )
            val result = apiService.createUser(request)
            if (result.isSuccess) {
                val dto = result.getOrThrow()
                val createdAt = Clock.System.now().toString()
                // Persist to local cache
                queries.insertOrReplace(
                    id = dto.id,
                    name = dto.name,
                    email = dto.email,
                    gender = dto.gender,
                    status = dto.status,
                    createdAt = createdAt
                )
                val user = dto.toDomain().copy(createdAt = createdAt)
                Result.success(user)
            } else {
                Result.failure(result.exceptionOrNull() ?: Exception("Failed to create user"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteUserLocally(userId: Long) {
        queries.deleteById(userId)
    }

    override suspend fun confirmDeleteUser(userId: Long): Result<Unit> {
        return try {
            apiService.deleteUser(userId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun restoreUser(user: User) {
        val entity = user.toEntity()
        queries.insertOrReplace(
            id = entity.id,
            name = entity.name,
            email = entity.email,
            gender = entity.gender,
            status = entity.status,
            createdAt = entity.createdAt
        )
    }
}
