package dev.armaan.pd

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import dev.armaan.pd.data.Db
import dev.armaan.pd.data.HabitRepository
import dev.armaan.pd.data.SettingsStore
import dev.armaan.pd.data.TodoRepository
import dev.armaan.pd.source.CalendarReader
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import java.time.Clock
import java.time.ZoneId

/**
 * The single wiring place (D10, D11). The system clock, the system zone and the dispatchers are
 * read here and nowhere else; everything else receives them. Instrumented tests pass a fixed
 * clock, an in-memory database with isolated settings, and may fake the calendar permission check
 * (revoking a runtime permission kills the process under test).
 */
class AppContainer(
    context: Context,
    val clock: Clock = Clock.systemDefaultZone(),
    private val zoneOf: () -> ZoneId = ZoneId::systemDefault,
    inMemory: Boolean = false,
    calendarGranted: (() -> Boolean)? = null,
) {
    /** Re-read on `ACTION_TIMEZONE_CHANGED` and on foreground (SPEC §2), so it is a function. */
    fun zone(): ZoneId = zoneOf()

    val io: CoroutineDispatcher = Dispatchers.IO

    private val db = if (inMemory) Db.inMemory(context, io) else Db.build(context, io)

    val habits = HabitRepository(db.habitDao())
    val todos = TodoRepository(db.todoDao())
    val settings = if (inMemory) SettingsStore.isolated(context, io) else SettingsStore.build(context, io)
    val calendar = CalendarReader(context.contentResolver, io)

    /** SPEC §5.1: whether `READ_CALENDAR` is granted right now. */
    val calendarGranted: () -> Boolean = calendarGranted
        ?: { context.checkSelfPermission(Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED }
}
