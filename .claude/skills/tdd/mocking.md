# When to Mock — personalDashboard

Short answer: almost never. The architecture (proposed in `CLAUDE.md` Load-bearing constraints)
puts every boundary behind an argument or an interface with a fake.

## Boundaries and how each is handled

| Boundary | Handled by | In tests |
|---|---|---|
| Time | `now: Instant` argument / injected `Clock` | pass a fixed instant |
| Coroutine scheduling | injected `CoroutineDispatcher` | `StandardTestDispatcher`, `advanceTimeBy` |
| Randomness (IDs) | injected generator | assert shape, not value |
| Network / data sources | repository interface | hand-written fake; parsers tested on fixture files |
| Database (Room / DataStore) | DAO | real in-memory DB, not a mock |
| Side effects (notifications, WorkManager, intents) | `effects` list returned by `reduce` | assert the list |
| Android framework | ViewModel / Activity plumbing only | not in JVM tests; instrumented if needed |

If a test needs a mocking library, the code under test is on the wrong side of the reducer
boundary. Move the logic into `reduce` or a pure function and test it there.

## Fakes over mocks

A fake is a small in-test implementation of an interface (`FakeWeatherRepo(var next: Result)`).
It asserts nothing itself; the test asserts outcomes. Write fakes in `src/test/.../fakes/`, one
per interface, shared across tests (stateless or reset per test).

## Don't mock

- Own pure code — domain, reducer, parsers. Fast and deterministic already.
- Room — use `Room.inMemoryDatabaseBuilder`.
- Kotlin stdlib / `java.time`.

## Do not add

No MockK, Mockito, or Turbine unless a decision row names it. `kotlinx-coroutines-test` is the
ceiling by default.

## Designing for testability (house rule)

- Inject time. The system clock is read in one wiring place only.
- Return effects instead of performing them. `reduce` never touches network, disk, or the OS.
- Keep ViewModels thin: collect event → `reduce` → publish state → hand effects to executors.
  Nothing to test there except that the plumbing is connected.
