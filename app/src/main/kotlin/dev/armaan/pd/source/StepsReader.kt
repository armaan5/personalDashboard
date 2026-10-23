package dev.armaan.pd.source

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.HealthConnectFeatures
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateGroupByPeriodRequest
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import dev.armaan.pd.app.Event
import dev.armaan.pd.app.HealthCheck
import dev.armaan.pd.steps.HealthSdk
import dev.armaan.pd.time.today
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Period
import java.time.ZoneId
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/**
 * Plumbing for SPEC §5.3: reads Health Connect `StepsRecord` totals, read-only, and hands `:core`
 * plain numbers. Every rule (states, the week, the ring) lives in `dev.armaan.pd.steps`.
 */
class StepsReader(private val context: Context, private val io: CoroutineDispatcher) {
    private val client by lazy { HealthConnectClient.getOrCreate(context) }

    /** The SDK status, and whether `READ_STEPS` is granted (false whenever Health Connect can't run). */
    suspend fun check(): HealthCheck = withContext(io) {
        val sdk = when (HealthConnectClient.getSdkStatus(context)) {
            HealthConnectClient.SDK_AVAILABLE -> HealthSdk.Available
            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> HealthSdk.NeedsInstall
            else -> HealthSdk.Unavailable
        }
        val granted = sdk == HealthSdk.Available &&
            (orNull { ReadSteps in client.permissionController.getGrantedPermissions() } ?: false)
        HealthCheck(sdk, granted)
    }

    /**
     * What the request asks for: `READ_STEPS`, plus background reads where Health Connect offers
     * them, so the widget's 30-minute refresh can read steps too (D26). Only `READ_STEPS` decides
     * the card's state.
     */
    fun permissions(): Set<String> = buildSet {
        add(ReadSteps)
        val background = orNull {
            client.features.getFeatureStatus(HealthConnectFeatures.FEATURE_READ_HEALTH_DATA_IN_BACKGROUND)
        }
        if (background == HealthConnectFeatures.FEATURE_STATUS_AVAILABLE) add(ReadInBackground)
    }

    /**
     * Today's total and the last 7 days (SPEC §5.3), for the card and the widget. Null when either
     * read fails, including a background read without [ReadInBackground].
     */
    suspend fun read(now: Instant, zone: ZoneId): Event.StepsLoaded? {
        val today = today(now, zone)
        val total = total(today.atStartOfDay(zone).toInstant(), now) ?: return null
        // Whole days: Health Connect drops a slice the range cuts short, which was today's.
        // It reads these local times in the system zone, which is what zone() returns outside tests.
        val days = daily(today.minusDays(6).atStartOfDay(), today.plusDays(1).atStartOfDay()) ?: return null
        return Event.StepsLoaded(today, total, days)
    }

    /** Total steps in `[from, until)`. Null when the read fails (revoked, backgrounded, provider gone). */
    private suspend fun total(from: Instant, until: Instant): Long? = withContext(io) {
        orNull {
            client.aggregate(
                AggregateRequest(setOf(StepsRecord.COUNT_TOTAL), TimeRangeFilter.between(from, until)),
            )[StepsRecord.COUNT_TOTAL] ?: 0L
        }
    }

    /** Daily totals by local date for `[from, until)`; days without records are absent. Null on failure. */
    private suspend fun daily(from: LocalDateTime, until: LocalDateTime): Map<LocalDate, Long>? = withContext(io) {
        orNull {
            client.aggregateGroupByPeriod(
                AggregateGroupByPeriodRequest(
                    metrics = setOf(StepsRecord.COUNT_TOTAL),
                    timeRangeFilter = TimeRangeFilter.between(from, until),
                    timeRangeSlicer = Period.ofDays(1),
                ),
            ).associate { it.startTime.toLocalDate() to (it.result[StepsRecord.COUNT_TOTAL] ?: 0L) }
        }
    }

    /** Health Connect throws for a revoked permission, a background read or a missing provider. */
    private inline fun <T> orNull(read: () -> T): T? = try {
        read()
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        null
    }

    companion object {
        val ReadSteps: String = HealthPermission.getReadPermission(StepsRecord::class)
        const val ReadInBackground: String = HealthPermission.PERMISSION_READ_HEALTH_DATA_IN_BACKGROUND
    }
}
