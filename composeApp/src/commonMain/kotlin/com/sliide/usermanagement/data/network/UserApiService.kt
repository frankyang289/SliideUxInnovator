package com.sliide.usermanagement.data.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess

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
            val totalPages = response.headers["X-Pagination-Pages"]?.toIntOrNull() ?: 1
            val users: List<UserDto> = response.body()
            Result.success(Pair(users, totalPages))
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

            // Fetch last page
            val lastPageResult = getUsers(totalPages)
            if (lastPageResult.isFailure) return Result.failure(lastPageResult.exceptionOrNull()!!)
            val lastPageUsers = lastPageResult.getOrThrow().first

            // If last page only has a few users, also fetch the previous page
            // and merge so we always show a full list
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
     * Fetches the most recently created users
     * **/
    suspend fun getLatestUsers(): Result<List<UserDto>> {
        return try {
            val response: HttpResponse = client.get("$BASE_URL/users") {
                bearerAuth(token)
                parameter("page", 1)
                parameter("per_page", 20)
            }
            val users: List<UserDto> = response.body()
            Result.success(users)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Creates a new user. Returns 201 on success.
     * On 422, parses GoRest's validation error list and surfaces a readable message.
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
                401 -> Result.failure(Exception("Unauthorized — check your API token"))
                else -> Result.failure(Exception("Failed to create user: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Deletes a user. Returns 204 on success.
     */
    suspend fun deleteUser(userId: Long): Result<Unit> {
        return try {
            val response: HttpResponse = client.delete("$BASE_URL/users/$userId") {
                bearerAuth(token)
            }
            if (response.status.value == 204) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to delete user: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}