package dev.armaan.pd.source

import android.content.ContentResolver
import android.content.ContentUris
import android.database.ContentObserver
import android.database.Cursor
import android.os.Handler
import android.os.Looper
import android.provider.CalendarContract
import android.provider.CalendarContract.Calendars
import android.provider.CalendarContract.Instances
import dev.armaan.pd.calendar.DeviceCalendar
import dev.armaan.pd.calendar.Instance
import java.time.Instant
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext

/**
 * Plumbing for SPEC §5.2: reads `CalendarContract`, read-only, and hands `:core` plain rows. Every
 * rule (dates, what shows where) lives in `dev.armaan.pd.calendar`. Without the permission each
 * read returns nothing rather than throwing.
 */
class CalendarReader(private val resolver: ContentResolver, private val io: CoroutineDispatcher) {
    /**
     * `Instances`, never `Events`, so recurrences expand. [ids] null reads every calendar with
     * `VISIBLE = 1`; otherwise only those `Calendars._ID`s.
     */
    suspend fun instances(from: Instant, until: Instant, ids: Set<String>?): List<Instance> = withContext(io) {
        val selection = if (ids == null) {
            "${Instances.VISIBLE} = 1"
        } else {
            // Inlined as numbers: they are parsed longs, never free text.
            val numeric = ids.mapNotNull(String::toLongOrNull)
            if (numeric.isEmpty()) return@withContext emptyList()
            "${Instances.CALENDAR_ID} IN (${numeric.joinToString(",")})"
        }
        val uri = Instances.CONTENT_URI.buildUpon()
            .also { ContentUris.appendId(it, from.toEpochMilli()) }
            .also { ContentUris.appendId(it, until.toEpochMilli()) }
            .build()
        read(uri, InstanceColumns, selection, "${Instances.BEGIN} ASC") { c ->
            Instance(
                eventId = c.getLong(0),
                title = c.getString(1).orEmpty(),
                begin = Instant.ofEpochMilli(c.getLong(2)),
                end = Instant.ofEpochMilli(c.getLong(3)),
                allDay = c.getInt(4) == 1,
            )
        }
    }

    /** Every calendar on the device, for Settings → Calendars. */
    suspend fun calendars(): List<DeviceCalendar> = withContext(io) {
        read(Calendars.CONTENT_URI, CalendarColumns, null, "${Calendars.ACCOUNT_NAME}, ${Calendars.CALENDAR_DISPLAY_NAME}") { c ->
            DeviceCalendar(
                id = c.getLong(0).toString(),
                name = c.getString(1).orEmpty(),
                account = c.getString(2).orEmpty(),
                color = c.getInt(3),
                visible = c.getInt(4) == 1,
            )
        }
    }

    /** Emits once on collection, then whenever the calendar provider changes. Collect only while granted. */
    val changes: Flow<Unit> = callbackFlow {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                trySend(Unit)
            }
        }
        // Throws without READ_CALENDAR (e.g. revoked between the check and here): read once, don't watch.
        val watching = try {
            resolver.registerContentObserver(CalendarContract.CONTENT_URI, true, observer)
            true
        } catch (_: SecurityException) {
            false
        }
        trySend(Unit)
        awaitClose { if (watching) resolver.unregisterContentObserver(observer) }
    }

    private fun <T> read(
        uri: android.net.Uri,
        columns: Array<String>,
        selection: String?,
        order: String,
        row: (Cursor) -> T,
    ): List<T> = try {
        resolver.query(uri, columns, selection, null, order)?.use { c ->
            buildList { while (c.moveToNext()) add(row(c)) }
        }.orEmpty()
    } catch (_: SecurityException) {
        emptyList()
    }

    private companion object {
        val InstanceColumns = arrayOf(Instances.EVENT_ID, Instances.TITLE, Instances.BEGIN, Instances.END, Instances.ALL_DAY)
        val CalendarColumns = arrayOf(
            Calendars._ID,
            Calendars.CALENDAR_DISPLAY_NAME,
            Calendars.ACCOUNT_NAME,
            Calendars.CALENDAR_COLOR,
            Calendars.VISIBLE,
        )
    }
}
