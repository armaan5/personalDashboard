package dev.armaan.pd.data

import dev.armaan.pd.app.Event
import dev.armaan.pd.habit.Habit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate

/** Plumbing: Room in, `:core` snapshots out; performs the habit effects (SPEC §4.4). */
class HabitRepository(private val dao: HabitDao) {
    val loaded: Flow<Event.HabitsLoaded> =
        combine(dao.habits(), dao.completions()) { habits, completions ->
            Event.HabitsLoaded(
                habits = habits.map { it.toDomain() },
                completions = completions.groupBy({ it.habitId }, { it.date }).mapValues { it.value.toSet() },
            )
        }

    /** [Effect.WriteHabit][dev.armaan.pd.app.Effect.WriteHabit]: insert when `id == 0`, else update. */
    suspend fun write(habit: Habit) = dao.upsert(habit.toEntity())

    suspend fun delete(habitId: Long) = dao.delete(habitId)

    suspend fun restore(habit: Habit, completions: Set<LocalDate>) =
        dao.restore(habit.toEntity(), completions.map { CompletionEntity(habit.id, it) })

    suspend fun setCompletion(habitId: Long, date: LocalDate, done: Boolean) =
        if (done) dao.insertCompletions(listOf(CompletionEntity(habitId, date)))
        else dao.deleteCompletion(habitId, date)
}
