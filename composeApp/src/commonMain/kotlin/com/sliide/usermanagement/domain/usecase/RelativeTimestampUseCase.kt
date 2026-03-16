package com.sliide.usermanagement.domain.usecase

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/**
 * Converts an ISO-8601 timestamp string into a human-readable relative time
 * string, e.g. "5 minutes ago", "2 hours ago", "3 days ago".
 *
 * All logic lives in commonMain — no platform code needed.
 */
object RelativeTimestampUseCase {

    fun execute(isoTimestamp: String): String {
        return try {
            val instant = Instant.parse(isoTimestamp)
            val now = Clock.System.now()
            val diffSeconds = (now - instant).inWholeSeconds

            when {
                diffSeconds < 60 -> "just now"
                diffSeconds < 3600 -> {
                    val mins = diffSeconds / 60
                    if (mins == 1L) "1 minute ago" else "$mins minutes ago"
                }
                diffSeconds < 86400 -> {
                    val hours = diffSeconds / 3600
                    if (hours == 1L) "1 hour ago" else "$hours hours ago"
                }
                diffSeconds < 2592000 -> {
                    val days = diffSeconds / 86400
                    if (days == 1L) "yesterday" else "$days days ago"
                }
                diffSeconds < 31536000 -> {
                    val months = diffSeconds / 2592000
                    if (months == 1L) "1 month ago" else "$months months ago"
                }
                else -> {
                    val years = diffSeconds / 31536000
                    if (years == 1L) "1 year ago" else "$years years ago"
                }
            }
        } catch (e: Exception) {
            "Unknown time"
        }
    }
}
