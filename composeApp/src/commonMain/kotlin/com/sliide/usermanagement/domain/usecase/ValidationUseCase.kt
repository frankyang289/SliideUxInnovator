package com.sliide.usermanagement.domain.usecase

object ValidationUseCase {

    private val EMAIL_REGEX = Regex(
        "^[a-zA-Z0-9.!#\$%&'*+/=?^_`{|}~-]+" +
        "@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?" +
        "(?:\\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*" +
        "\\.[a-zA-Z]{2,}$"
    )

    // Name: 2–60 chars, letters (including unicode), spaces, hyphens, apostrophes
    private val NAME_REGEX = Regex("^[\\p{L}\\s'\\-]{2,60}$")

    sealed class ValidationResult {
        object Valid : ValidationResult()
        data class Invalid(val reason: String) : ValidationResult()
    }

    fun validateEmail(email: String): ValidationResult {
        if (email.isBlank()) return ValidationResult.Invalid("Email cannot be empty")
        if (email.length > 254) return ValidationResult.Invalid("Email is too long")
        if (!EMAIL_REGEX.matches(email.trim())) return ValidationResult.Invalid("Please enter a valid email")
        return ValidationResult.Valid
    }

    fun validateName(name: String): ValidationResult {
        if (name.isBlank()) return ValidationResult.Invalid("Name cannot be empty")
        if (name.trim().length < 2) return ValidationResult.Invalid("Name must be at least 2 characters")
        if (name.length > 60) return ValidationResult.Invalid("Name must be less than 60 characters")
        if (!NAME_REGEX.matches(name.trim())) return ValidationResult.Invalid("Name contains invalid characters")
        return ValidationResult.Valid
    }

    fun isFormValid(name: String, email: String): Boolean {
        return validateName(name) is ValidationResult.Valid &&
               validateEmail(email) is ValidationResult.Valid
    }
}
