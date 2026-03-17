package com.sliide.usermanagement.data.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType

class UserApiService(
    private val client: HttpClient,
    private val token: String
) {

    /**
     * Fetches a specific page of users.
     * Returns a Pair of (users, totalPages).
     */
    suspend fun getUsers(page: Int): Result<Pair<List<UserDto>, Int>> {
        return try {
            val response: HttpResponse = client.get("$BASE_URL/users") {
                bearerAuth(token)
                parameter("page", page)
                parameter("per_page", 20)
            }
            when (response.status.value) {
                200 -> {
                    val totalPages = response.headers["X-Pagination-Pages"]?.toIntOrNull() ?: 1
                    val users: List<UserDto> = response.body()
                    Result.success(Pair(users, totalPages))
                }
                else -> Result.failure(resolveGetError(response.status.value))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetches the last page of users.
     */
    suspend fun getLastPageUsers(): Result<List<UserDto>> {
        return try {
            val firstResult = getUsers(1)
            if (firstResult.isFailure) return Result.failure(firstResult.exceptionOrNull()!!)

            val (firstPageUsers, totalPages) = firstResult.getOrThrow()

            if (totalPages <= 1) return Result.success(firstPageUsers)

            val lastPageResult = getUsers(totalPages)
            if (lastPageResult.isFailure) return Result.failure(lastPageResult.exceptionOrNull()!!)
            val lastPageUsers = lastPageResult.getOrThrow().first

            if (lastPageUsers.size < 20 && totalPages >= 2) {
                val secondToLastResult = getUsers(totalPages - 1)
                if (secondToLastResult.isSuccess) {
                    val combined = secondToLastResult.getOrThrow().first + lastPageUsers
                    return Result.success(combined)
                }
            }

            Result.success(lastPageUsers)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetches the most recently created users (page 1 = highest IDs = newest).
     */
    suspend fun getLatestUsers(): Result<List<UserDto>> {
        return try {
            val response: HttpResponse = client.get("$BASE_URL/users") {
                bearerAuth(token)
                parameter("page", 1)
                parameter("per_page", 20)
            }
            when (response.status.value) {
                200 -> Result.success(response.body())
                else -> Result.failure(resolveGetError(response.status.value))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Creates a new user. Returns 201 on success.
     * On 422, parses GoRest's field-level validation errors and surfaces them directly.
     */
    suspend fun createUser(request: CreateUserRequest): Result<UserDto> {
        return try {
            val response: HttpResponse = client.post("$BASE_URL/users") {
                bearerAuth(token)
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            when (response.status.value) {
                201 -> Result.success(response.body())
                422 -> {
                    val errors: List<GoRestErrorDto> = try {
                        response.body()
                    } catch (e: Exception) {
                        emptyList()
                    }
                    val message = if (errors.isNotEmpty()) {
                        errors.joinToString("\n") { "${it.field}: ${it.message}" }
                    } else {
                        "Validation failed (422)"
                    }
                    Result.failure(Exception(message))
                }
                else -> Result.failure(resolveWriteError(response.status.value, "create user"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Deletes a user by ID. Returns 204 on success.
     * 404 is treated as success — if the user doesn't exist, the desired end state is achieved.
     */
    suspend fun deleteUser(userId: Long): Result<Unit> {
        return try {
            val response: HttpResponse = client.delete("$BASE_URL/users/$userId") {
                bearerAuth(token)
            }
            when (response.status.value) {
                204, 404 -> Result.success(Unit)
                else -> Result.failure(resolveWriteError(response.status.value, "delete user"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    /**
     * Maps common error status codes for GET requests to user-readable messages.
     */
    private fun resolveGetError(statusCode: Int): Exception = when (statusCode) {
        401 -> Exception("Unauthorized — check your API token")
        429 -> Exception("Rate limit reached — please wait a moment and try again")
        500, 502, 503 -> Exception("GoRest server error — please try again later")
        else -> Exception("Unexpected error fetching users: $statusCode")
    }

    /**
     * Maps common error status codes for write operations (POST, DELETE) to user-readable messages.
     * The [operation] label is included in the fallback message for easier debugging.
     */
    private fun resolveWriteError(statusCode: Int, operation: String): Exception = when (statusCode) {
        400 -> Exception("Bad request — malformed request body")
        401 -> Exception("Unauthorized — check your API token")
        429 -> Exception("Rate limit reached — please wait a moment and try again")
        500, 502, 503 -> Exception("GoRest server error — please try again later")
        else -> Exception("Failed to $operation: $statusCode")
    }
}