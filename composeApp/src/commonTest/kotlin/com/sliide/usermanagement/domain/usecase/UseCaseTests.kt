package com.sliide.usermanagement.domain.usecase

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class RelativeTimestampUseCaseTest {

    private fun timestampSecondsAgo(seconds: Long): String =
        (Clock.System.now() - seconds.seconds).toString()

    private fun timestampMinutesAgo(mins: Long): String =
        (Clock.System.now() - mins.minutes).toString()

    private fun timestampHoursAgo(hours: Long): String =
        (Clock.System.now() - hours.hours).toString()

    private fun timestampDaysAgo(days: Long): String =
        (Clock.System.now() - days.days).toString()

    @Test
    fun `just now for less than 60 seconds`() {
        assertEquals("just now", RelativeTimestampUseCase.execute(timestampSecondsAgo(30)))
    }

    @Test
    fun `singular minute`() {
        assertEquals("1 minute ago", RelativeTimestampUseCase.execute(timestampMinutesAgo(1)))
    }

    @Test
    fun `plural minutes`() {
        assertEquals("5 minutes ago", RelativeTimestampUseCase.execute(timestampMinutesAgo(5)))
    }

    @Test
    fun `singular hour`() {
        assertEquals("1 hour ago", RelativeTimestampUseCase.execute(timestampHoursAgo(1)))
    }

    @Test
    fun `plural hours`() {
        assertEquals("3 hours ago", RelativeTimestampUseCase.execute(timestampHoursAgo(3)))
    }

    @Test
    fun `yesterday for 1 day`() {
        assertEquals("yesterday", RelativeTimestampUseCase.execute(timestampDaysAgo(1)))
    }

    @Test
    fun `plural days`() {
        assertEquals("5 days ago", RelativeTimestampUseCase.execute(timestampDaysAgo(5)))
    }

    @Test
    fun `invalid timestamp returns unknown time`() {
        assertEquals("Unknown time", RelativeTimestampUseCase.execute("not-a-date"))
    }

    @Test
    fun `empty string returns unknown time`() {
        assertEquals("Unknown time", RelativeTimestampUseCase.execute(""))
    }
}

class ValidationUseCaseTest {

    // ── Email ──────────────────────────────────────────────────────────────────

    @Test
    fun `valid standard email passes`() {
        assertTrue(ValidationUseCase.validateEmail("user@example.com") is ValidationUseCase.ValidationResult.Valid)
    }

    @Test
    fun `valid subdomain email passes`() {
        assertTrue(ValidationUseCase.validateEmail("user@mail.example.co.uk") is ValidationUseCase.ValidationResult.Valid)
    }

    @Test
    fun `email without at sign fails`() {
        assertTrue(ValidationUseCase.validateEmail("userexample.com") is ValidationUseCase.ValidationResult.Invalid)
    }

    @Test
    fun `email without domain fails`() {
        assertTrue(ValidationUseCase.validateEmail("user@") is ValidationUseCase.ValidationResult.Invalid)
    }

    @Test
    fun `email without TLD fails`() {
        assertTrue(ValidationUseCase.validateEmail("user@example") is ValidationUseCase.ValidationResult.Invalid)
    }

    @Test
    fun `blank email fails`() {
        assertTrue(ValidationUseCase.validateEmail("") is ValidationUseCase.ValidationResult.Invalid)
    }

    @Test
    fun `whitespace only email fails`() {
        assertTrue(ValidationUseCase.validateEmail("   ") is ValidationUseCase.ValidationResult.Invalid)
    }

    // ── Name ───────────────────────────────────────────────────────────────────

    @Test
    fun `valid name passes`() {
        assertTrue(ValidationUseCase.validateName("Alice Smith") is ValidationUseCase.ValidationResult.Valid)
    }

    @Test
    fun `name with hyphen passes`() {
        assertTrue(ValidationUseCase.validateName("Mary-Jane Watson") is ValidationUseCase.ValidationResult.Valid)
    }

    @Test
    fun `name with apostrophe passes`() {
        assertTrue(ValidationUseCase.validateName("O'Brien") is ValidationUseCase.ValidationResult.Valid)
    }

    @Test
    fun `single char name fails`() {
        assertTrue(ValidationUseCase.validateName("A") is ValidationUseCase.ValidationResult.Invalid)
    }

    @Test
    fun `blank name fails`() {
        assertTrue(ValidationUseCase.validateName("") is ValidationUseCase.ValidationResult.Invalid)
    }

    @Test
    fun `name exceeding 60 chars fails`() {
        val longName = "A".repeat(61)
        assertTrue(ValidationUseCase.validateName(longName) is ValidationUseCase.ValidationResult.Invalid)
    }

    // ── isFormValid ────────────────────────────────────────────────────────────

    @Test
    fun `isFormValid returns true for valid inputs`() {
        assertTrue(ValidationUseCase.isFormValid("Alice Smith", "alice@example.com"))
    }

    @Test
    fun `isFormValid returns false when email invalid`() {
        assertFalse(ValidationUseCase.isFormValid("Alice Smith", "not-email"))
    }

    @Test
    fun `isFormValid returns false when name invalid`() {
        assertFalse(ValidationUseCase.isFormValid("A", "alice@example.com"))
    }

    @Test
    fun `isFormValid returns false when both invalid`() {
        assertFalse(ValidationUseCase.isFormValid("", ""))
    }
}
