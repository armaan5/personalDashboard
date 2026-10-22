package dev.armaan.pd

import android.content.Context
import dev.armaan.pd.data.Db
import dev.armaan.pd.data.HabitRepository
import dev.armaan.pd.data.TodoRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import java.time.Clock
import java.time.ZoneId

/**
 * The single wiring place (D10, D11). The system clock, the system zone and the dispatchers are
 * read here and nowhere else; everything else receives them.
 */
class AppContainer(context: Context) {
    val clock: Clock = Clock.systemDefaultZone()

    /** Re-read on `ACTION_TIMEZONE_CHANGED` and on foreground (SPEC §2), so it is a function. */
    fun zone(): ZoneId = ZoneId.systemDefault()

    val io: CoroutineDispatcher = Dispatchers.IO

    private val db = Db.build(context, io)

    val habits = HabitRepository(db.habitDao())
    val todos = TodoRepository(db.todoDao())
}
