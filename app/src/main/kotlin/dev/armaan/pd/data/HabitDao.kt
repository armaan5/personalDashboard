package dev.armaan.pd.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface HabitDao {
    @Query("SELECT * FROM habit ORDER BY sort_order")
    fun habits(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habit_completion")
    fun completions(): Flow<List<CompletionEntity>>

    @Upsert
    suspend fun upsert(habit: HabitEntity)

    @Query("DELETE FROM habit WHERE id = :id")
    suspend fun delete(id: Long)

    /** The (habit, date) primary key makes a repeated tick a no-op. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCompletions(completions: List<CompletionEntity>)

    @Query("DELETE FROM habit_completion WHERE habit_id = :habitId AND date = :date")
    suspend fun deleteCompletion(habitId: Long, date: LocalDate)

    @Transaction
    suspend fun restore(habit: HabitEntity, completions: List<CompletionEntity>) {
        upsert(habit)
        insertCompletions(completions)
    }
}
