package com.sliide.usermanagement.domain.repository

import com.sliide.usermanagement.domain.model.Gender
import com.sliide.usermanagement.domain.model.User
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    /**
     * Emits the cached user list immediately, then refreshes from network.
     * Offline: emits only the cached list.
     */
    fun getUsers(): Flow<List<User>>

    /**
     * Fetches fresh users from the network and updates the cache.
     */
    suspend fun refreshUsers(): Result<Unit>

    /**
     * Creates a new user via API and prepends to local cache on success.
     */
    suspend fun createUser(name: String, email: String, gender: Gender): Result<User>

    /**
     * Removes user from local cache immediately (optimistic).
     * Calls API delete in background — caller handles undo.
     */
    suspend fun deleteUserLocally(userId: Long)

    /**
     * Commits the deletion to the API after the undo window expires.
     */
    suspend fun confirmDeleteUser(userId: Long): Result<Unit>

    /**
     * Restores a previously locally-deleted user back into the cache.
     */
    suspend fun restoreUser(user: User)
}
