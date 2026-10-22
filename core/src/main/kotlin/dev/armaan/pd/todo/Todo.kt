package dev.armaan.pd.todo

import java.time.Instant
import java.time.LocalDate

/** SPEC §3.1 / §4.3. `id == 0` means not yet stored. `completedOn == null` means open. */
data class Todo(
    val id: Long,
    val title: String,
    val dueDate: LocalDate?,
    val createdAt: Instant,
    val completedOn: LocalDate?,
)

const val TODO_TITLE_MAX = 120

/** Trimmed title, or null when blank or longer than [TODO_TITLE_MAX] (SPEC §4.3). */
fun cleanTodoTitle(raw: String): String? =
    raw.trim().takeIf { it.isNotEmpty() && it.length <= TODO_TITLE_MAX }
