package dev.armaan.pd.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import java.time.LocalDate
import kotlin.coroutines.CoroutineContext

/**
 * SPEC §3.1. Every later version ships a `Migration` and a `MigrationTestHelper` test in the same
 * ticket. Destructive migration is never enabled.
 */
@Database(
    entities = [HabitEntity::class, CompletionEntity::class, TodoEntity::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class Db : RoomDatabase() {
    abstract fun habitDao(): HabitDao

    abstract fun todoDao(): TodoDao

    companion object {
        const val NAME = "personal_dashboard.db"

        fun build(context: Context, queryContext: CoroutineContext): Db =
            Room.databaseBuilder(context, Db::class.java, NAME)
                .setQueryCoroutineContext(queryContext)
                .build()
    }
}

class Converters {
    @TypeConverter
    fun fromDate(date: LocalDate?): String? = date?.toString()

    @TypeConverter
    fun toDate(text: String?): LocalDate? = text?.let(LocalDate::parse)
}
