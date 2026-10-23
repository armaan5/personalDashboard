package dev.armaan.pd.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import dev.armaan.pd.app.Event
import dev.armaan.pd.app.Settings
import java.io.File
import java.util.UUID
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** Plumbing: DataStore `settings` (SPEC §3.2) in, `:core` [Settings] out. */
class SettingsStore(private val store: DataStore<Preferences>) {
    val settings: Flow<Settings> = store.data
        .map { prefs -> Settings(calendarIds = prefs[CalendarIds], askedCalendar = prefs[AskedCalendar] ?: false) }
        .distinctUntilChanged()

    val loaded: Flow<Event.SettingsLoaded> = settings.map(Event::SettingsLoaded)

    suspend fun setAskedCalendar() {
        store.edit { it[AskedCalendar] = true }
    }

    suspend fun setCalendarIds(ids: Set<String>) {
        store.edit { it[CalendarIds] = ids }
    }

    companion object {
        private val CalendarIds = stringSetPreferencesKey("calendar_ids")
        private val AskedCalendar = booleanPreferencesKey("asked_calendar")

        fun build(context: Context, io: CoroutineDispatcher) =
            create(io) { context.preferencesDataStoreFile("settings") }

        /** For instrumented tests: a fresh file per container, since one file allows one DataStore. */
        fun isolated(context: Context, io: CoroutineDispatcher) =
            create(io) { File(context.cacheDir, "settings-${UUID.randomUUID()}.preferences_pb") }

        private fun create(io: CoroutineDispatcher, file: () -> File) = SettingsStore(
            PreferenceDataStoreFactory.create(scope = CoroutineScope(io + SupervisorJob()), produceFile = file),
        )
    }
}
