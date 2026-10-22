package dev.armaan.pd.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface TodoDao {
    @Query("SELECT * FROM todo")
    fun todos(): Flow<List<TodoEntity>>

    @Upsert
    suspend fun upsert(todo: TodoEntity)

    @Query("DELETE FROM todo WHERE id IN (:ids)")
    suspend fun delete(ids: List<Long>)
}
