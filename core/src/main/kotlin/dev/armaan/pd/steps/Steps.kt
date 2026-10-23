package dev.armaan.pd.steps

import dev.armaan.pd.app.Access
import java.time.LocalDate
import java.util.Locale

/** `HealthConnectClient.getSdkStatus`, as SPEC §5.3 reads it. */
enum class HealthSdk { Unavailable, NeedsInstall, Available }

data class DaySteps(val date: LocalDate, val count: Long)

/** SPEC §5.3 card states. */
sealed interface StepsCard {
    data object Unavailable : StepsCard

    data object NeedsInstall : StepsCard

    /** A permission state from §5.1 other than Granted. */
    data class Blocked(val access: Access) : StepsCard

    data object NoData : StepsCard

    data class Steps(val today: Long, val last7: List<DaySteps>) : StepsCard
}

/**
 * SPEC §5.3, in check order: Unavailable → NeedsInstall → permission states → NoData → Steps.
 * Null while a check it depends on hasn't been read yet, so nothing shows off a default.
 */
fun stepsCard(sdk: HealthSdk?, access: Access?, today: Long?, last7: List<DaySteps>?): StepsCard? = when (sdk) {
    null -> null
    HealthSdk.Unavailable -> StepsCard.Unavailable
    HealthSdk.NeedsInstall -> StepsCard.NeedsInstall
    HealthSdk.Available -> when {
        access == null -> null
        access != Access.Granted -> StepsCard.Blocked(access)
        today == null || last7 == null -> null
        last7.sumOf { it.count } == 0L -> StepsCard.NoData
        else -> StepsCard.Steps(today, last7)
    }
}

/** The ring shows today against the goal, capped at a full turn (SPEC §5.3). */
fun ringFraction(today: Long, goal: Int): Float = (today.toFloat() / goal).coerceIn(0f, 1f)

/** The count as shown, uncapped, with en-GB thousands separators: `8,000`. */
fun stepsLabel(count: Long): String = String.format(Locale.UK, "%,d", count)

/**
 * The bars: the last 7 local days, today last (SPEC §5.3). [counts] is per local date, holding only
 * days that have records; the rest read 0.
 */
fun lastSevenDays(counts: Map<LocalDate, Long>, today: LocalDate): List<DaySteps> =
    (6L downTo 0L).map { back -> today.minusDays(back).let { DaySteps(it, counts[it] ?: 0L) } }

const val GoalMin = 1_000
const val GoalMax = 50_000
const val GoalStep = 500
const val GoalDefault = 8_000

/** Settings → Step goal (SPEC §3.2, §5.3): 1,000–50,000, on the 500 step. */
fun clampGoal(goal: Int): Int =
    (Math.round(goal.coerceIn(GoalMin, GoalMax) / GoalStep.toDouble()) * GoalStep).toInt()

/** Steps left to the goal; 0 once it's met. */
fun toGo(today: Long, goal: Int): Long = (goal - today).coerceAtLeast(0L)

/** The bars' full height: the best day, or the goal when no day reached it, so the goal line shows. */
fun barScale(week: List<DaySteps>, goal: Int): Long = maxOf(week.maxOfOrNull { it.count } ?: 0L, goal.toLong())
