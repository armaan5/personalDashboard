package dev.armaan.pd.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import dev.armaan.pd.habit.Habit
import dev.armaan.pd.habit.Weekdays
import dev.armaan.pd.todo.Todo
import java.time.Instant
import java.time.LocalDate

// SPEC §3.1, schema version 1. Dates are ISO `LocalDate` text via [Converters].

@Entity(tableName = "habit")
data class HabitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long,
    val name: String,
    val weekdays: Int,
    @ColumnInfo(name = "created_on") val createdOn: LocalDate,
    @ColumnInfo(name = "sort_order") val sortOrder: Int,
)

@Entity(
    tableName = "habit_completion",
    primaryKeys = ["habit_id", "date"],
    foreignKeys = [
        ForeignKey(
            entity = HabitEntity::class,
            parentColumns = ["id"],
            childColumns = ["habit_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class CompletionEntity(
    @ColumnInfo(name = "habit_id") val habitId: Long,
    val date: LocalDate,
)

@Entity(tableName = "todo")
data class TodoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long,
    val title: String,
    @ColumnInfo(name = "due_date") val dueDate: LocalDate?,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "completed_on") val completedOn: LocalDate?,
)

fun HabitEntity.toDomain() = Habit(id, name, Weekdays.fromMask(weekdays), createdOn, sortOrder)

fun Habit.toEntity() = HabitEntity(id, name, Weekdays.toMask(weekdays), createdOn, sortOrder)

fun TodoEntity.toDomain() = Todo(id, title, dueDate, Instant.ofEpochMilli(createdAt), completedOn)

fun Todo.toEntity() = TodoEntity(id, title, dueDate, createdAt.toEpochMilli(), completedOn)
