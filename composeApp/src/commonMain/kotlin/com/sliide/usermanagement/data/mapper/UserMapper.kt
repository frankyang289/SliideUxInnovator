package com.sliide.usermanagement.data.mapper

import com.sliide.usermanagement.data.network.UserDto
import com.sliide.usermanagement.db.UserEntity
import com.sliide.usermanagement.domain.model.Gender
import com.sliide.usermanagement.domain.model.User
import com.sliide.usermanagement.domain.model.UserStatus
import kotlinx.datetime.Clock

fun UserDto.toDomain(): User = User(
    id = id,
    name = name,
    email = email,
    gender = Gender.from(gender),
    status = UserStatus.from(status),
    createdAt = Clock.System.now().toString()
)

fun UserDto.toEntity(createdAt: String = Clock.System.now().toString()): UserEntity = UserEntity(
    id = id,
    name = name,
    email = email,
    gender = gender,
    status = status,
    createdAt = createdAt
)

fun UserEntity.toDomain(): User = User(
    id = id,
    name = name,
    email = email,
    gender = Gender.from(gender),
    status = UserStatus.from(status),
    createdAt = createdAt
)

fun User.toEntity(): UserEntity = UserEntity(
    id = id,
    name = name,
    email = email,
    gender = gender.value,
    status = status.value,
    createdAt = createdAt
)
