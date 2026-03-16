package com.sliide.usermanagement.domain.model

data class User(
    val id: Long,
    val name: String,
    val email: String,
    val gender: Gender,
    val status: UserStatus,
    val createdAt: String
)

enum class Gender(val value: String) {
    MALE("male"),
    FEMALE("female");

    companion object {
        fun from(value: String) = entries.firstOrNull { it.value == value } ?: MALE
    }
}

enum class UserStatus(val value: String) {
    ACTIVE("active"),
    INACTIVE("inactive");

    companion object {
        fun from(value: String) = entries.firstOrNull { it.value == value } ?: ACTIVE
    }
}
