package dev.armaan.pd.source

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateGroupByPeriodRequest
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import dev.armaan.pd.app.HealthCheck
import dev.armaan.pd.steps.HealthSdk
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Period
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

    /** Total steps in `[from, until)`. Null when the read fails (revoked, backgrounded, provider gone). */
    suspend fun total(from: Instant, until: Instant): Long? = withContext(io) {
        orNull {
            client.aggregate(
                AggregateRequest(setOf(StepsRecord.COUNT_TOTAL), TimeRangeFilter.between(from, until)),
            )[StepsRecord.COUNT_TOTAL] ?: 0L
        }
    }

    /** Daily totals by local date for `[from, until)`; days without records are absent. Null on failure. */
    suspend fun daily(from: LocalDateTime, until: LocalDateTime): Map<LocalDate, Long>? = withContext(io) {
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
    }
}
