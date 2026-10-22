package dev.armaan.pd.data

import dev.armaan.pd.app.Event
import dev.armaan.pd.todo.Todo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Plumbing: Room in, `:core` snapshots out; performs the to-do effects (SPEC §4.4). */
class TodoRepository(private val dao: TodoDao) {
    val loaded: Flow<Event.TodosLoaded> =
        dao.todos().map { rows -> Event.TodosLoaded(rows.map { it.toDomain() }) }

    /** [Effect.WriteTodo][dev.armaan.pd.app.Effect.WriteTodo]: insert when `id == 0`, else upsert by id. */
    suspend fun write(todo: Todo) = dao.upsert(todo.toEntity())

    suspend fun delete(ids: List<Long>) = dao.delete(ids)
}
