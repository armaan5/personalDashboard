package dev.armaan.pd.todo

import java.time.Instant
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class BucketsTest {
    private val today = LocalDate.of(2026, 10, 22)
    private val t0 = Instant.parse("2026-10-01T09:00:00Z")

    private fun oct(day: Int) = LocalDate.of(2026, 10, day)

    /** Each to-do is created [id] hours after t0 unless told otherwise. */
    private fun todo(id: Long, due: LocalDate? = null, done: LocalDate? = null, createdHour: Long = id) =
        Todo(id, "Item $id", due, t0.plusSeconds(createdHour * 3600), done)

    private fun ids(list: List<Todo>) = list.map { it.id }

    @Test
    fun `buckets - each boundary at today plus or minus one`() {
        val out = buckets(
            listOf(
                todo(1, due = oct(21)),
                todo(2, due = oct(22)),
                todo(3, due = oct(23)),
                todo(4),
                todo(5, due = oct(23), done = oct(22)),
                todo(6, due = oct(20), done = oct(21)),
            ),
            today,
        )
        assertEquals(listOf(1L), ids(out.overdue))
        assertEquals(listOf(2L), ids(out.today))
        assertEquals(listOf(3L), ids(out.upcoming))
        assertEquals(listOf(4L), ids(out.undated))
        assertEquals(listOf(5L), ids(out.doneToday))
        assertEquals(listOf(6L), ids(out.history))
    }

    @Test
    fun `buckets - doneToday moves to history after midnight`() {
        val ticked = listOf(todo(1, done = oct(22)))
        assertEquals(listOf(1L), ids(buckets(ticked, oct(22)).doneToday))
        assertEquals(listOf(1L), ids(buckets(ticked, oct(23)).history))
        assertEquals(emptyList(), buckets(ticked, oct(23)).doneToday)
    }

    @Test
    fun `buckets - a completion dated after today counts as done today`() {
        // The zone moved west after the tick (SPEC §4.1: stored dates aren't rewritten).
        assertEquals(listOf(1L), ids(buckets(listOf(todo(1, done = oct(23))), today).doneToday))
    }

    @Test
    fun `order - overdue oldest due first`() {
        val out = buckets(listOf(todo(1, due = oct(20)), todo(2, due = oct(15)), todo(3, due = oct(21))), today)
        assertEquals(listOf(2L, 1L, 3L), ids(out.overdue))
    }

    @Test
    fun `order - undated and today oldest created first`() {
        val out = buckets(
            listOf(todo(1, createdHour = 5), todo(2, createdHour = 1), todo(3, due = oct(22), createdHour = 9), todo(4, due = oct(22), createdHour = 2)),
            today,
        )
        assertEquals(listOf(2L, 1L), ids(out.undated))
        assertEquals(listOf(4L, 3L), ids(out.today))
    }

    @Test
    fun `order - upcoming soonest first`() {
        val out = buckets(listOf(todo(1, due = oct(30)), todo(2, due = oct(23)), todo(3, due = oct(25))), today)
        assertEquals(listOf(2L, 3L, 1L), ids(out.upcoming))
    }

    @Test
    fun `order - history most recent first`() {
        val out = buckets(listOf(todo(1, done = oct(10)), todo(2, done = oct(21)), todo(3, done = oct(15))), today)
        assertEquals(listOf(2L, 3L, 1L), ids(out.history))
    }

    @Test
    fun `card - overdue, then today, then undated, then done today, without upcoming or history`() {
        val out = buckets(
            listOf(
                todo(1, done = oct(22)),
                todo(2),
                todo(3, due = oct(22)),
                todo(4, due = oct(21)),
                todo(5, due = oct(24)),
                todo(6, done = oct(20)),
            ),
            today,
        )
        assertEquals(listOf(4L, 3L, 2L, 1L), ids(out.card))
    }
}
