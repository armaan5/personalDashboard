package dev.armaan.pd.todo

import java.time.LocalDate

/** SPEC §4.3 buckets relative to `today`, each already in display order. */
data class Buckets(
    val overdue: List<Todo>,
    val today: List<Todo>,
    val undated: List<Todo>,
    val upcoming: List<Todo>,
    val doneToday: List<Todo>,
    val history: List<Todo>,
) {
    /** The dashboard card: overdue, today, undated, then done today (struck through). */
    val card: List<Todo> get() = overdue + today + undated + doneToday
}

fun buckets(todos: List<Todo>, today: LocalDate): Buckets {
    val (done, open) = todos.partition { it.completedOn != null }
    // A completion dated after today (the zone moved west) still counts as done today.
    val (history, doneToday) = done.partition { it.completedOn!! < today }
    val (dated, undated) = open.partition { it.dueDate != null }
    val byDue = compareBy<Todo>({ it.dueDate }, { it.createdAt }, { it.id })
    val byCreated = compareBy<Todo>({ it.createdAt }, { it.id })
    return Buckets(
        overdue = dated.filter { it.dueDate!! < today }.sortedWith(byDue),
        today = dated.filter { it.dueDate == today }.sortedWith(byCreated),
        undated = undated.sortedWith(byCreated),
        upcoming = dated.filter { it.dueDate!! > today }.sortedWith(byDue),
        doneToday = doneToday.sortedWith(byCreated),
        history = history.sortedWith(compareByDescending<Todo> { it.completedOn }.thenByDescending { it.createdAt }),
    )
}
