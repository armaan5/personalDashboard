# Good and Bad Tests — personalDashboard examples

Runner is JUnit + `kotlin.test`. Examples are **illustrative**: the domain (`Tile`, `DashState`,
`refresh`) is a placeholder until `SPEC.md` names the real one. Replace these with real cases
once the first wave ships; keep the good/bad pairs.

## Good

**Reducer test, expected value from the spec.**

```kotlin
import kotlin.test.Test
import kotlin.test.assertEquals
import java.time.Instant

class ReduceRefreshTest {
    private val t0 = Instant.parse("2026-10-14T09:00:00Z")
    // base() is this file's factory returning a DashState with one cached tile.
    private fun base() = DashState(tiles = listOf(Tile(id = "weather", value = "14°C", fetchedAt = t0)))

    @Test
    fun `refresh - cache older than 30 min is marked stale`() {
        val now = t0.plusSeconds(31 * 60)
        val out = reduce(base(), Event.Tick, now)
        assertEquals(true, out.state.tiles.single().stale) // SPEC §x: stale after 30 min
    }
}
```

Why it is good: drives the public seam, asserts state the UI renders, expected value is a spec
literal, `now` is explicit, name says WHAT.

**Order of effects is the assertion.**

```kotlin
@Test
fun `refresh - failure keeps cached value then schedules retry`() {
    val out = reduce(base(), Event.FetchFailed("weather"), t0)
    assertEquals(
        listOf(Effect.ShowError("weather"), Effect.ScheduleRetry("weather", t0.plusSeconds(300))),
        out.effects,
    ) // SPEC §x: error first, retry in 5 min
    assertEquals("14°C", out.state.tiles.single().value)
}
```

**Parser against a checked-in fixture.**

```kotlin
@Test
fun `weather parser - reads current temperature`() {
    val json = javaClass.getResource("/weather_ok.json")!!.readText()
    assertEquals(14.2, parseWeather(json).tempC)
}
```

## Bad

**Implementation-coupled.**

```kotlin
// BAD: verifies a call instead of an outcome
@Test fun `refresh calls repository`() {
    vm.onRefresh()
    verify(exactly = 1) { repo.fetch(any()) }
}
// GOOD: assert the effect reduce returned, or the state the fake repo produced
```

**Tautological.**

```kotlin
// BAD: expected value computed the way the code computes it
assertEquals(formatTemp(14.2), render(tile).text)
// GOOD: a literal you can check by eye
assertEquals("14°C", render(tile).text)
```

**Wall clock.**

```kotlin
// BAD: flaky, depends on when the test runs
val out = reduce(base(), Event.Tick, Instant.now())
// GOOD: fixed instant
val out = reduce(base(), Event.Tick, t0.plusSeconds(1860))
```

**Whole-object snapshot for a one-field change.**

```kotlin
// BAD
assertEquals(base().copy(tiles = listOf(tile.copy(stale = true))), out.state)
// GOOD
assertEquals(true, out.state.tiles.single().stale)
```
