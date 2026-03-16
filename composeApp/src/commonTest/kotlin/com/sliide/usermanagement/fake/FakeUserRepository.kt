package com.sliide.usermanagement.fake

import com.sliide.usermanagement.domain.model.Gender
import com.sliide.usermanagement.domain.model.User
import com.sliide.usermanagement.domain.model.UserStatus
import com.sliide.usermanagement.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.datetime.Clock

class FakeUserRepository : UserRepository {

    private val _users = MutableStateFlow<List<User>>(emptyList())
    var refreshResult: Result<Unit> = Result.success(Unit)
    var createResult: Result<User>? = null  // null = auto-generate
    var deleteResult: Result<Unit> = Result.success(Unit)
    var refreshCallCount = 0
    var deleteConfirmCallCount = 0

    fun seedUsers(users: List<User>) {
        _users.value = users
    }

    override fun getUsers(): Flow<List<User>> = _users.asStateFlow()

    override suspend fun refreshUsers(): Result<Unit> {
        refreshCallCount++
        if (refreshResult.isSuccess) {
            _users.value = defaultUsers()
        }
        return refreshResult
    }

    override suspend fun createUser(name: String, email: String, gender: Gender): Result<User> {
        val user = createResult?.getOrNull() ?: User(
            id = Clock.System.now().toEpochMilliseconds(),
            name = name,
            email = email,
            gender = gender,
            status = UserStatus.ACTIVE,
            createdAt = Clock.System.now().toString()
        )
        return if (createResult?.isFailure == true) {
            createResult!!
        } else {
            _users.update { listOf(user) + it }
            Result.success(user)
        }
    }

    override suspend fun deleteUserLocally(userId: Long) {
        _users.update { it.filter { u -> u.id != userId } }
    }

    override suspend fun confirmDeleteUser(userId: Long): Result<Unit> {
        deleteConfirmCallCount++
        return deleteResult
    }

    override suspend fun restoreUser(user: User) {
        _users.update { listOf(user) + it }
    }

    private fun defaultUsers() = listOf(
        User(1L, "Alice Smith", "alice@example.com", Gender.FEMALE, UserStatus.ACTIVE, Clock.System.now().toString()),
        User(2L, "Bob Jones", "bob@example.com", Gender.MALE, UserStatus.INACTIVE, Clock.System.now().toString()),
    )
}
