package dev.armaan.pd.data

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.armaan.pd.habit.Habit
import dev.armaan.pd.todo.Todo
import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.SUNDAY
import java.time.DayOfWeek.WEDNESDAY
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
class DaoTest {
    private lateinit var db: Db
    private lateinit var habits: HabitRepository
    private lateinit var todos: TodoRepository

    private fun oct(day: Int) = LocalDate.of(2026, 10, day)

    private val stretch = Habit(0, "Stretch", setOf(MONDAY, WEDNESDAY, SUNDAY), createdOn = oct(19), sortOrder = 0)

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, Db::class.java).build()
        habits = HabitRepository(db.habitDao())
        todos = TodoRepository(db.todoDao())
    }

    @After
    fun tearDown() = db.close()

    private suspend fun storedHabit(): Habit {
        habits.write(stretch)
        return habits.loaded.first().habits.single()
    }

    @Test
    fun habit_roundTripsWithItsWeekdays() = runTest {
        val stored = storedHabit()
        assertEquals(stretch.copy(id = stored.id), stored)
        assertEquals(setOf(MONDAY, WEDNESDAY, SUNDAY), stored.weekdays)
    }

    @Test
    fun completion_primaryKeyRejectsDuplicates() = runTest {
        val id = storedHabit().id
        habits.setCompletion(id, oct(21), done = true)
        habits.setCompletion(id, oct(21), done = true)
        assertEquals(mapOf(id to setOf(oct(21))), habits.loaded.first().completions)
    }

    @Test
    fun completion_doneFalseRemovesOnlyThatDate() = runTest {
        val id = storedHabit().id
        habits.setCompletion(id, oct(19), done = true)
        habits.setCompletion(id, oct(21), done = true)
        habits.setCompletion(id, oct(21), done = false)
        assertEquals(mapOf(id to setOf(oct(19))), habits.loaded.first().completions)
    }

    @Test
    fun deletingAHabit_cascadesItsCompletions() = runTest {
        val id = storedHabit().id
        habits.setCompletion(id, oct(21), done = true)
        habits.delete(id)
        val loaded = habits.loaded.first()
        assertEquals(emptyList(), loaded.habits)
        assertEquals(emptyMap(), loaded.completions)
        assertEquals(0, db.query("SELECT * FROM habit_completion", null).use { it.count })
    }

    @Test
    fun editingAHabit_keepsItsCompletions() = runTest {
        // A REPLACE-style write would delete the row and cascade away the streak.
        val stored = storedHabit()
        habits.setCompletion(stored.id, oct(21), done = true)
        habits.write(stored.copy(name = "Stretch more", weekdays = setOf(FRIDAY)))
        val loaded = habits.loaded.first()
        assertEquals("Stretch more", loaded.habits.single().name)
        assertEquals(mapOf(stored.id to setOf(oct(21))), loaded.completions)
    }

    @Test
    fun restore_bringsBackTheSameIdAndCompletions() = runTest {
        val stored = storedHabit()
        habits.setCompletion(stored.id, oct(19), done = true)
        habits.setCompletion(stored.id, oct(21), done = true)
        habits.delete(stored.id)

        habits.restore(stored, setOf(oct(19), oct(21)))

        val loaded = habits.loaded.first()
        assertEquals(listOf(stored), loaded.habits)
        assertEquals(mapOf(stored.id to setOf(oct(19), oct(21))), loaded.completions)
    }

    @Test
    fun todo_withNullDueDateRoundTrips() = runTest {
        val todo = Todo(0, "Buy milk", dueDate = null, createdAt = Instant.parse("2026-10-22T09:00:00Z"), completedOn = null)
        todos.write(todo)
        val stored = todos.loaded.first().todos.single()
        assertEquals(todo.copy(id = stored.id), stored)
    }

    @Test
    fun todo_writeWithAnIdUpsertsIt() = runTest {
        todos.write(Todo(0, "Buy milk", oct(23), Instant.parse("2026-10-22T09:00:00Z"), null))
        val stored = todos.loaded.first().todos.single()

        todos.write(stored.copy(completedOn = oct(22)))
        assertEquals(listOf(stored.copy(completedOn = oct(22))), todos.loaded.first().todos)

        // Clear history, then Undo: the row comes back under its old id.
        todos.delete(listOf(stored.id))
        todos.write(stored)
        assertEquals(listOf(stored), todos.loaded.first().todos)
    }
}
