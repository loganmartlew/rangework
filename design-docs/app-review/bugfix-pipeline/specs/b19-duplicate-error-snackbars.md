# B19 — Identical consecutive error snackbars don't re-show

Batch: android-ui
Source: ../../potential-bugs.md#b19 (full finding text copied below — this spec is self-contained)

## Finding (verbatim)

> `RangeSessionScreen.kt:134-138`
>
> The snackbar effect keys on the message string, so two back-to-back failures with the same
> copy ("Couldn't record ball. Please try again.") show only once, masking repeated transient
> errors. Key on a monotonic event id instead.

## Confirmation method

The defect lives in a `LaunchedEffect` key, which `:androidApp` unit tests cannot observe
(there is no Compose UI test harness in this project, and D7 forecloses adding one). Confirm
at the **ViewModel** layer instead, which is also where the fix lands:

New test case appended to
`apps/mobile/androidApp/src/test/java/com/loganmartlew/rangework/android/ui/RangeSessionViewModelTest.kt`
(**additions only** — that file's `FakeRangeSessionRepository` near line ~1350 already has a
`shouldFail`-style toggle to force errors):

Drive the same failing operation twice (e.g. record-ball failing twice), consuming the
notification in between via `onConsumeNotification`, and assert the two notifications are
**distinguishable** — i.e. the state carries something that changes between them. Today
`notification` is a bare `String?` (`RangeSessionViewModel.kt:56`), so two identical failures
produce an identical value and the test fails.

State plainly in the verdict that this confirms the *cause* (identical state → identical
`LaunchedEffect` key → no re-show) rather than the on-screen symptom, and that the symptom
itself is verified only by the reviewer reading `RangeSessionScreen.kt:134-138`.

## Definition of done

- New test passes
- `RangeSessionScreen.kt:134-138`'s `LaunchedEffect` keys on a monotonically increasing event
  id, not on the message string, and the snackbar shows once per event
- All 13 existing `notification = "..."` assignment sites in `RangeSessionViewModel.kt`
  (lines 172, 251, 372, 409, 468, 506, 558, 589, 648, 786, 828) go through the new mechanism —
  a fix that converts only the record-ball path leaves the bug live everywhere else
- `onConsumeNotification` (`RangeSessionViewModel.kt:513`) still clears correctly and cannot
  clear a *newer* notification that arrived between show and consume
- `.\gradlew.bat :androidApp:testDebugUnitTest :androidApp:testReleaseUnitTest :shared:testDebugUnitTest`
  green; `.\gradlew.bat :androidApp:lintDebug` green
- Scope boundary: `RangeSessionViewModel.kt`, `RangeSessionScreen.kt`,
  `RangeSessionViewModelTest.kt`. No shared-module changes. **Do not** convert other screens'
  snackbar handling in this PR, even though they likely share the pattern — note them in the
  PR body for a follow-up issue

## Notes for the fixer

- Current shape: `val notification: String? = null` in the UI state
  (`RangeSessionViewModel.kt:56`), set by `_uiState.value.copy(notification = "...")` at the
  13 sites above, cleared by `onConsumeNotification` (`:513`), and consumed in Compose by
  `LaunchedEffect(uiState.notification) { ... showSnackbar(msg); onConsumeNotification() }`
  (`RangeSessionScreen.kt:134-138`).
- The minimal honest fix is a small value type — e.g. `data class Notification(val id: Long, val message: String)`
  with a VM-held counter — and `LaunchedEffect(uiState.notification?.id)`. Keying the effect
  on the whole object works only if the id makes it unequal, so the id must be the key.
- Watch the consume race: `onConsumeNotification` currently nulls the field unconditionally.
  If a new notification lands while the snackbar is showing, an unconditional clear drops it.
  Consuming *by id* (clear only if the current id matches the one shown) avoids this and is
  worth the few extra lines — the whole point of the bug is not losing error signals.
- `showSnackbar` suspends until the snackbar is dismissed, so back-to-back events queue rather
  than interleave. That's fine and expected; don't add a queue of your own.
- `RangeSessionViewModelTest.kt` is large (1300+ lines) with established fakes — read the
  existing failure-path tests before adding yours; several already drive `shouldFail` toggles
  and assert on `notification`. **Those existing assertions will break** when the field's type
  changes. The additions-only guard permits the mechanical update needed to keep them
  compiling and passing, but do not weaken or delete an existing assertion — if one can't be
  updated mechanically, stop and flag it.
