# Stage 11 — Approved deferred work: implementation plan

> Roadmap stage **S11**. This plan covers **only** the deferred items the product owner approved for implementation (see the "Implement" column of `deferred-changes.md`). It is written to be followed step-by-step without needing to re-derive context.

## Scope

**In scope (build these — ordered by user impact, highest first):**

| Order | Feature | Source ID | Section |
|---|---|---|---|
| 1 | Drag-to-reorder gesture in both editors | B01 | [S11-1](#s11-1--drag-to-reorder-gesture-b01) |
| 2 | "Used in N sessions" reverse link + delete safety | F-3 | [S11-2](#s11-2--used-in-n-sessions-reverse-link-f-3) |
| 3 | Session item notes shown on session detail | — | [S11-3](#s11-3--session-item-notes-on-session-detail) |
| 4 | Swipe-to-edit/delete on list cards | B31 | [S11-4](#s11-4--swipe-to-editdelete-on-list-cards-b31) |
| 5 | Collapsing TopAppBar on detail screens | B56 | [S11-5](#s11-5--collapsing-topappbar-on-detail-screens-b56) |
| 6 | Login icon decorative TalkBack description | — | [S11-6](#s11-6--login-icon-decorative-talkback-description) |
| 7 | `ClickableText` → `LinkAnnotation` migration | — | [S11-7](#s11-7--clickabletext--linkannotation-migration) |

**Explicitly OUT of scope (do NOT implement — marked "No" by the product owner):**

- **Run / "follow at the range" mode** (F-4) — large feature, needs its own plan.
- **Remove the Session-detail waypoint** (B11 / F-1) — the screen stays.
- **Undo for instruction/item delete inside editors** (B18 partial) — large feature, needs its own plan.
- **Tablet list-detail pane** (F-2) — deferred to a dedicated tablet UI pass.
- **Terms & Privacy URL wiring** (login `LegalLine`) — links stay as no-ops. Note S11-7 keeps them non-functional on purpose.

## Ground rules (apply to every section)

- UI/Compose/ViewModel code lives in `androidApp`; domain models, validation, and pure derivations live in `shared` (`CLAUDE.md`).
- Do **not** change SDK, Java, Kotlin, or Compose BOM versions except where S11-1 explicitly adds one library.
- Follow the typography rules in `CLAUDE.md`: numeric/metric text uses `RangeworkMono.*`; everything else uses `MaterialTheme.typography.*`. No anonymous inline `TextStyle`s. No hardcoded colours — use `MaterialTheme.colorScheme.*`.
- After each section, run the validation commands at the [end of this file](#global-validation). Run tests **before** lint.
- Each section is independent and can be shipped on its own. Recommended order is the table above.

---

## S11-1 — Drag-to-reorder gesture (B01)

### Goal
Make the drag-handle icon on instruction rows (Unit editor) and session-item rows (Session editor) actually reorder by dragging. Keep the existing ↑/↓ chevron buttons as the accessible fallback — **do not remove them**.

### Background (already in place)
- `UnitEditorScreen.kt` renders instruction rows with the `ReorderableItemRow` component, which already shows a static `DragHandle` icon and ↑/↓ chevrons. Reorder is driven by `onMoveInstructionUp(index)` / `onMoveInstructionDown(index)`.
- `SessionEditorScreen.kt` renders `SessionItemEditorCard`s with an inline `DragHandle` icon and inline ↑/↓ chevrons. Reorder is driven by `onMoveSessionItem(fromIndex, toIndex)`.
- Both screens use a `LazyColumn` with `itemsIndexed(..., key = { _, x -> x.order })`.

### Step 1 — Add the reorder library to the version catalog
The deferral reason was toolchain risk, so do the compatibility check first.

In `gradle/libs.versions.toml`:

1. Under `[versions]` add:
   ```toml
   reorderable = "2.4.3"
   ```
2. Under `[libraries]` add:
   ```toml
   reorderable = { module = "sh.calvin.reorderable:reorderable", version.ref = "reorderable" }
   ```

In `androidApp/build.gradle.kts`, inside the `dependencies { }` block, add:
```kotlin
implementation(libs.reorderable)
```

**Compatibility spike (do this before writing any UI code):** run
`.\gradlew.bat :androidApp:assembleDebug`.
- If it resolves and builds, continue.
- If the version fails to resolve, change `reorderable = "2.4.3"` to the latest published `2.x` version of `sh.calvin.reorderable:reorderable` and rebuild. Do **not** bump any other version to make it fit. If no `2.x` version builds against Compose BOM `2024.10.01`, stop and report — do not force a BOM change.

This library is chosen because it targets modern Compose `LazyColumn` and does not require a toolchain bump.

### Step 2 — Wire drag in the Unit editor (`UnitEditorScreen.kt`)
The `ReorderableItemRow` component takes a `content` slot. The library wraps each `LazyColumn` item in a `ReorderableItem` scope that exposes a drag `Modifier` to attach to the handle.

1. Add imports:
   ```kotlin
   import androidx.compose.foundation.lazy.rememberLazyListState
   import sh.calvin.reorderable.ReorderableItem
   import sh.calvin.reorderable.rememberReorderableLazyListState
   ```
2. Inside `UnitEditorScreen`, before the `Scaffold`, create the list state and reorder state:
   ```kotlin
   val lazyListState = rememberLazyListState()
   val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
       // Both lists share the same LazyColumn; convert absolute indices to
       // instruction indices by subtracting the count of non-instruction items
       // that appear before the instruction block (title, notes/focus, club,
       // "INSTRUCTIONS" header = 4 items).
       val headerCount = 4
       onMoveInstruction(from.index - headerCount, to.index - headerCount)
   }
   ```
   > Note: the current screen exposes `onMoveInstructionUp`/`onMoveInstructionDown` (single-step), not a from/to move. Add a from/to mover — see Step 4.
3. Pass `state = lazyListState` to the `LazyColumn`.
4. Replace the `itemsIndexed(...)` block for instructions so each row is wrapped in `ReorderableItem`:
   ```kotlin
   itemsIndexed(
       items = editor.instructions,
       key = { _, instruction -> instruction.order },
   ) { index, instruction ->
       ReorderableItem(reorderableState, key = instruction.order) { isDragging ->
           InstructionEditorRow(
               instruction = instruction,
               number = index + 1,
               isWorking = isWorking,
               dragHandleModifier = Modifier.draggableHandle(),
               onUpdateText = { onUpdateInstructionText(index, it) },
               onUpdateBallCount = { onUpdateInstructionBallCount(index, it) },
               onMoveUp = { onMoveInstructionUp(index) },
               onMoveDown = { onMoveInstructionDown(index) },
               onRemove = { onRemoveInstruction(index) },
               canMoveUp = index > 0,
               canMoveDown = index < editor.instructions.lastIndex,
           )
       }
   }
   ```
   `Modifier.draggableHandle()` is an extension available inside the `ReorderableItem` scope.
5. `InstructionEditorRow` must forward a drag-handle modifier down to `ReorderableItemRow`. Add a `dragHandleModifier: Modifier = Modifier` parameter to `InstructionEditorRow` and pass it through.

### Step 3 — Attach the handle modifier in `ReorderableItemRow.kt`
1. Add a parameter: `dragHandleModifier: Modifier = Modifier`.
2. Apply it to the existing `DragHandle` `Icon`:
   ```kotlin
   Icon(
       imageVector = Icons.Default.DragHandle,
       contentDescription = "Drag to reorder",
       tint = MaterialTheme.colorScheme.onSurfaceVariant,
       modifier = dragHandleModifier.size(24.dp),
   )
   ```
   Leave everything else (chevrons, delete) unchanged.

### Step 4 — Add a from/to instruction mover (ViewModel + actions)
The Session editor already has `moveSessionItem(from, to)`. The Unit editor only has up/down. Add the symmetric API.

1. In `PracticePlannerViewModel.kt`, find `moveInstructionUp` / `moveInstructionDown`. Add:
   ```kotlin
   fun moveInstruction(fromIndex: Int, toIndex: Int) {
       // Reuse the same list-reordering logic the up/down movers use.
       // Guard indices to the current instruction range before moving.
   }
   ```
   Implement it by moving the instruction at `fromIndex` to `toIndex` in the editor instruction list and renormalising `order` exactly the way `moveInstructionUp/Down` already do (copy that body, generalise the swap to an arbitrary target). Keep the existing up/down methods — the chevrons still call them.
2. In `PlannerActions.kt`, add to `UnitEditorActions`:
   ```kotlin
   val onMoveInstruction: (Int, Int) -> Unit,
   ```
3. In `RangeworkApp.kt`, in the `UnitEditorActions(...)` constructor, add:
   ```kotlin
   onMoveInstruction = plannerViewModel::moveInstruction,
   ```
4. Thread `onMoveInstruction` into `UnitEditorScreen` as a new parameter and use it in the `rememberReorderableLazyListState` callback (Step 2.2). Pass it from both the `UnitCreate` and `UnitEdit` composable call sites in `RangeworkApp.kt`.

### Step 5 — Wire drag in the Session editor (`SessionEditorScreen.kt`)
The session editor has a `stickyHeader`, a details `item`, an optional empty-state `item`, then the `itemsIndexed` item block, then the "Add item" `item`. Because indices include the sticky header and other items, compute the offset.

1. Add the same imports as Step 2.1 plus keep `ExperimentalFoundationApi` (already opted-in).
2. Create `lazyListState` + `rememberReorderableLazyListState`. In its callback, translate `from.index`/`to.index` into session-item indices by subtracting the number of `LazyColumn` items before the item block. Determine the offset by counting the leading slots present at runtime:
   - `stickyHeader` (1) + details `item` (1) = 2 always.
   - the empty-state `item` only renders when `editor.items.isEmpty()`, in which case there are no draggable items anyway, so it does not affect dragging.
   - Therefore `val headerCount = 2` and call `onMoveSessionItem(from.index - headerCount, to.index - headerCount)`.
3. Pass `state = lazyListState` to the `LazyColumn`.
4. Wrap the `SessionItemEditorCard` in `ReorderableItem(reorderableState, key = item.order) { ... }`.
5. In `SessionItemEditorCard`, add a `dragHandleModifier: Modifier = Modifier` parameter and apply it to the inline `DragHandle` `Icon` (the one with `contentDescription = "Drag to reorder item $number"`). Pass `Modifier.draggableHandle()` from the `ReorderableItem` scope. Leave the inline chevrons unchanged.

### Tests
- `androidApp/src/test/.../PracticePlannerViewModelTest.kt`: add `moveInstructionReordersToArbitraryIndex` — create a unit with 3 instructions, call `moveInstruction(0, 2)`, assert the new order and that `order` fields are renormalised. Mirror the existing session-item move test if one exists.

### Validation
- Build + lint green (drag library resolves).
- Dragging an instruction handle reorders the list; dropping persists the new order through Save.
- Dragging a session-item handle reorders; subtotal/total stay correct.
- ↑/↓ chevrons still work (fallback intact).
- TalkBack: chevrons still announce "Move … up/down".

### Risks
- **Index-offset bug:** the `headerCount` offset is the most error-prone part. Verify by dragging the first and last rows specifically.
- If the library cannot build against the pinned BOM, this section is blocked — report rather than bump the toolchain.

---

## S11-2 — "Used in N sessions" reverse link (F-3)

### Goal
On the Unit detail screen, show which sessions use the unit, each tappable to open that session. Warn in the unit delete dialog when the unit is still used by sessions.

### Design decision (important)
The original F-3 note proposed a Supabase reverse query. **Do not add a query or migration.** All sessions are already loaded into `plannerUiState.sessions` (RLS-scoped at load time), so this is a **pure in-memory derivation**, consistent with how S3 implemented `recentItems` and `enabledClubCount`. This is simpler and lower-risk.

### Step 1 — Add the derivation (`shared`)
In `shared/src/commonMain/.../model/PracticePlanningMetrics.kt`, add:
```kotlin
fun sessionsUsingUnit(
    unitId: String,
    sessions: List<PracticeSession>,
): List<PracticeSession> = sessions.filter { session ->
    session.items.any { it.practiceUnitId == unitId }
}
```

### Step 2 — Test the derivation (`shared`)
In `shared/src/commonTest/.../usecase/Stage03DataEnablerTest.kt` (or a new `Stage11Test.kt` in the same package), add:
- a unit referenced by 2 of 3 sessions → returns those 2 sessions;
- a unit referenced by none → returns empty;
- a session referencing the unit in multiple items → the session appears once (the `any {}` guarantees this).

Use the existing `makeUnit` / `makeSession` helpers in that test file.

### Step 3 — Surface on Unit detail (`UnitDetailScreen.kt`)
1. Add a parameter `onViewSession: (String) -> Unit` to `UnitDetailScreen`.
2. Add imports:
   ```kotlin
   import com.loganmartlew.rangework.shared.model.sessionsUsingUnit
   import com.loganmartlew.rangework.android.ui.components.ListEntryCard // or a lighter row — see below
   ```
3. After the Instructions `Card` (inside `ScrollableScreen`, still using `unit`), compute and render:
   ```kotlin
   val usedInSessions = sessionsUsingUnit(unit.id, plannerUiState.sessions)
   if (usedInSessions.isNotEmpty()) {
       Card(modifier = Modifier.fillMaxWidth()) {
           Column(
               modifier = Modifier.fillMaxWidth().padding(16.dp),
               verticalArrangement = Arrangement.spacedBy(12.dp),
           ) {
               Text(
                   text = "Used in ${usedInSessions.size} session${if (usedInSessions.size == 1) "" else "s"}",
                   style = MaterialTheme.typography.titleMedium,
               )
               usedInSessions.forEach { session ->
                   // Simple tappable row: session name + chevron.
                   // Reuse a clickable Row with Role.Button; navigate on tap.
               }
           }
       }
   }
   ```
   Keep the row simple — a clickable `Row` (full width, `Role.Button`, `MaterialTheme.typography.bodyLarge` name + a trailing `Icons.AutoMirrored.Filled.KeyboardArrowRight` icon) that calls `onViewSession(session.id)`. Do not use `RangeworkMono` here (it is a name, not a metric). The count in the header is descriptive text, also not mono.

### Step 4 — Wire navigation (`RangeworkApp.kt`)
In the `composable(RangeworkRoutes.UnitDetail)` block, pass:
```kotlin
onViewSession = { sessionId ->
    shellNavController.navigate(RangeworkRoutes.sessionDetail(sessionId))
},
```

### Step 5 — Deletion-safety warning
Make the unit delete dialog warn when the unit is used.

1. In `DeleteConfirmationDialog.kt`, add an optional parameter:
   ```kotlin
   warning: String? = null,
   ```
   In the `text = { ... }`, render the base sentence and, when `warning != null`, a second line below it in `MaterialTheme.colorScheme.error`. Keep the existing copy when `warning` is null (backward-compatible — all current callers pass nothing).
2. In `RangeworkApp.kt`, the unit delete dialog is the `if (showUnitDeleteDialog) { DeleteConfirmationDialog(...) }` block. Compute the warning from the pending unit:
   ```kotlin
   val usedCount = pendingDeleteUnit?.let {
       sessionsUsingUnit(it.id, plannerUiState.sessions).size
   } ?: 0
   DeleteConfirmationDialog(
       itemName = pendingDeleteUnit?.title ?: "unit",
       warning = if (usedCount > 0)
           "This unit is used in $usedCount session${if (usedCount == 1) "" else "s"}. Those sessions will lose this unit."
           else null,
       onConfirm = { /* unchanged */ },
       onDismiss = { /* unchanged */ },
   )
   ```
   Add `import com.loganmartlew.rangework.shared.model.sessionsUsingUnit` to `RangeworkApp.kt`.
   Leave the **session** delete dialog unchanged (no `warning`).

### Validation
- Unit detail shows a "Used in N sessions" card listing the right sessions; tapping a row opens that session's detail.
- The card is absent for a unit no session uses.
- Deleting a used unit shows the error-coloured warning line; deleting an unused unit shows the original copy only.
- `shared` tests pass.

### Risks
- Deleting a unit referenced by a session does not cascade-delete the session item here — that is existing backend behaviour and out of scope. The warning is informational only.

---

## S11-3 — Session item notes on session detail

### Goal
Show per-item notes (entered in the session editor's "More options") on the Session detail screen.

### Step — `SessionDetailScreen.kt`, `SessionItemDetailRow`
`PracticeSessionItem` has a nullable `notes: String?`. The focus-cue line is already rendered at the bottom of the row at `padding(start = 40.dp)`. Add the notes line directly **below** the focus-cue block, using the same indentation:

```kotlin
item.notes?.takeIf(String::isNotBlank)?.let { notes ->
    Text(
        text = notes,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 40.dp),
    )
}
```

Place it as the last child of the row's outer `Column`, after the focus-cue `let` block. No new imports, no signature changes — `item` already carries `notes`.

> Optional polish (only if it reads ambiguously next to the focus cue): prefix with a quiet label, e.g. two adjacent `Text`s where "Notes" is `labelSmall`/`onSurfaceVariant`. Not required.

### Validation
- A session item with notes shows them under its row; an item without notes shows nothing extra.
- Focus cue and notes both render and are visually distinct from the unit title.

### Risks
- None significant — additive, no data or signature changes.

---

## S11-4 — Swipe-to-edit/delete on list cards (B31)

### Goal
Let users swipe a Units or Sessions list card: swipe one direction reveals/triggers **Edit**, the other reveals/triggers **Delete**. Tapping the card (open detail) and the overflow menu must keep working.

### Approach
Wrap each `ListEntryCard` at the call site (in `UnitListScreen.kt` and `SessionListScreen.kt`) in a Material 3 `SwipeToDismissBox`. The screens already hold the edit and delete callbacks and the delete-confirmation dialog state, so no ViewModel changes are needed.

### Step 1 — Units list (`UnitListScreen.kt`)
Inside the `else` branch where `plannerUiState.units.forEach { unit -> ListEntryCard(...) }`:

1. Add imports:
   ```kotlin
   import androidx.compose.material3.SwipeToDismissBox
   import androidx.compose.material3.SwipeToDismissBoxValue
   import androidx.compose.material3.rememberSwipeToDismissBoxState
   import androidx.compose.foundation.layout.Box
   import androidx.compose.foundation.layout.fillMaxSize
   import androidx.compose.ui.Alignment
   ```
2. For each `unit`, create a per-item dismiss state keyed by `unit.id` and wrap the card:
   ```kotlin
   val dismissState = rememberSwipeToDismissBoxState(
       confirmValueChange = { value ->
           when (value) {
               SwipeToDismissBoxValue.EndToStart -> { pendingDeleteUnit = unit; false } // swipe left → delete (confirm dialog); don't actually dismiss
               SwipeToDismissBoxValue.StartToEnd -> { onEditUnit(unit.id); false }       // swipe right → edit
               SwipeToDismissBoxValue.Settled -> false
           }
       },
   )
   SwipeToDismissBox(
       state = dismissState,
       backgroundContent = {
           // Coloured background that reveals an Edit icon on the start side
           // and a Delete icon (error container) on the end side, depending on
           // dismissState.dismissDirection. Use Box + Alignment.CenterStart /
           // CenterEnd with the matching icon.
       },
   ) {
       ListEntryCard( /* unchanged existing card */ )
   }
   ```
   Returning `false` from `confirmValueChange` means the row springs back after triggering the action — important so the card is never actually removed by the swipe itself (delete is gated behind the existing confirmation dialog; edit navigates away).
3. The existing `DeleteConfirmationDialog` driven by `pendingDeleteUnit` already handles the confirm/cancel. No change there.

### Step 2 — Sessions list (`SessionListScreen.kt`)
Apply the identical pattern in the `plannerUiState.sessions.forEach { session -> ListEntryCard(...) }` block, using `pendingDeleteSession = session` for EndToStart and `onEditSession(session.id)` for StartToEnd.

### Step 3 — Background content
Implement `backgroundContent` once as a small private composable in each screen (or a shared helper in `components/`), showing:
- Start→End (swiping right): `Icons.Default.Edit`, aligned `CenterStart`, on `MaterialTheme.colorScheme.secondaryContainer`.
- End→Start (swiping left): `Icons.Default.Delete` tinted `onErrorContainer`, aligned `CenterEnd`, on `MaterialTheme.colorScheme.errorContainer`.
Choose the icon/colour from `dismissState.dismissDirection`.

### Validation
- Swipe left on a card → delete confirmation dialog appears; Cancel restores the card; Confirm deletes.
- Swipe right on a card → opens the editor.
- Tap (not swipe) still opens detail; overflow menu still works.
- Card is never silently removed by the swipe alone.
- Works on both Units and Sessions lists; phone and tablet.

### Risks
- **Gesture conflict with vertical scroll:** `SwipeToDismissBox` handles horizontal-only swipes, so vertical list scroll is preserved. Verify scrolling still feels normal.
- **Accidental triggers:** returning `false` from `confirmValueChange` (spring-back) prevents destructive surprises. Do not return `true`.

---

## S11-5 — Collapsing TopAppBar on detail screens (B56)

### Goal
On Unit detail and Session detail, use a `MediumTopAppBar` that collapses as the user scrolls. All other routes keep the current pinned `CenterAlignedTopAppBar` behaviour.

### Background
`AuthenticatedAppShell` in `RangeworkApp.kt` has a single `Scaffold` whose `topBar` renders one `CenterAlignedTopAppBar` for every route. Detail screens use `ScrollableScreen` (a vertical-scroll `Column`), whose scroll propagates to a `nestedScroll` connection.

### Step 1 — Detect detail routes and pick a scroll behavior
Inside `AuthenticatedAppShell`, after `currentRoute` is computed, add:
```kotlin
val isDetailRoute = (currentUnitId != null) || (currentSessionId != null)
val scrollBehavior = if (isDetailRoute) {
    TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
} else {
    TopAppBarDefaults.pinnedScrollBehavior()
}
```
Add imports:
```kotlin
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.input.nestedscroll.nestedScroll
```
`currentUnitId` / `currentSessionId` are already derived just above in the function — reuse them (they are non-null only on detail, not edit, routes).

### Step 2 — Attach the connection to the Scaffold
On the existing `Scaffold(...)`, add to its `modifier`:
```kotlin
modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
```
(The Scaffold currently has no `modifier` argument — add one.)

### Step 3 — Swap the app bar per route
In the `topBar = { ... }` lambda, branch on `isDetailRoute`:
- When `isDetailRoute` is true, render a `MediumTopAppBar` with the **same** `title`, `navigationIcon`, and `actions` blocks currently used, plus `scrollBehavior = scrollBehavior`.
- Otherwise render the existing `CenterAlignedTopAppBar` unchanged, but also pass `scrollBehavior = scrollBehavior` (a pinned behavior is a no-op visually but keeps the connection consistent).

Extract the shared `title` / `navigationIcon` / `actions` lambdas into local `val`s (or a small private composable) so both bar types use identical content and you don't duplicate the Edit/overflow logic.

### Step 4 — Keep `@OptIn`
`MediumTopAppBar` and the scroll-behavior APIs are `ExperimentalMaterial3Api`. The function already has `@OptIn(ExperimentalMaterial3Api::class)` — no change needed.

### Validation
- On a detail screen, scrolling down collapses the large title into a standard-height bar; scrolling to top expands it.
- Edit icon + overflow menu still render and work in the collapsed and expanded states.
- Back button still works.
- All non-detail routes (lists, editors, settings, overview, manage clubs) look and behave exactly as before.
- Phone and tablet both correct.

### Risks
- **Shared Scaffold:** because the app bar is global, mistakes here affect every screen. Verify a non-detail route (e.g. Units list) is visually unchanged.
- **Editor routes:** ensure editor routes are NOT treated as detail (they aren't — `currentUnitId`/`currentSessionId` are null on `/edit` routes by their existing guards). Confirm the discard-changes flow still works.

---

## S11-6 — Login icon decorative TalkBack description

### Goal
Stop TalkBack from announcing the decorative Rangework mark on the login screen, without affecting other uses of `BrandMarkContainer`.

### Step 1 — `BrandMarkContainer.kt`
Add an optional parameter and forward it to the `Image`:
```kotlin
@Composable
internal fun BrandMarkContainer(
    size: Dp,
    markSize: Dp,
    twoColor: Boolean,
    contentDescription: String? = "Rangework mark",
) {
    ...
    Image(
        painter = painterResource(...),
        contentDescription = contentDescription,
        modifier = Modifier.size(markSize),
    )
}
```
The default keeps every existing call site (e.g. `BrandWordmark`) behaving exactly as today.

### Step 2 — Login call site (`RangeworkApp.kt`)
In `UnauthenticatedEntryScreen`, change the login mark to be decorative:
```kotlin
BrandMarkContainer(size = 84.dp, markSize = 60.dp, twoColor = true, contentDescription = null)
```
A `null` content description removes the focus stop. Do not change any other `BrandMarkContainer` call.

### Validation
- TalkBack on login: focus moves to the headline / sign-in button without announcing "Rangework mark".
- The wordmark in the app bar (`BrandWordmark`) is unchanged.

### Risks
- None — additive parameter with a safe default.

---

## S11-7 — `ClickableText` → `LinkAnnotation` migration

### Goal
Remove the soft-deprecated `ClickableText` in `LegalLine` (login) by using `Text` with `LinkAnnotation`. **Links remain non-functional** (URL wiring is out of scope per the product owner) — the migration only removes the deprecation and improves link semantics for TalkBack.

### Step — `RangeworkApp.kt`, `LegalLine`
1. Remove the import `androidx.compose.foundation.text.ClickableText`.
2. Add imports:
   ```kotlin
   import androidx.compose.ui.text.LinkAnnotation
   import androidx.compose.ui.text.withLink
   ```
   (`SpanStyle`, `buildAnnotatedString`, `withStyle`, `TextDecoration`, `TextAlign` are already imported.)
3. Rewrite the body to build the string with `withLink` and render a plain `Text`:
   ```kotlin
   @Composable
   private fun LegalLine() {
       val linkColor = MaterialTheme.colorScheme.primary
       val textColor = MaterialTheme.colorScheme.onSurfaceVariant
       val linkStyle = SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline)
       val noop = LinkInteractionListener { /* URLs wired when policies are published */ }

       val text = buildAnnotatedString {
           withStyle(SpanStyle(color = textColor)) { append("By continuing you agree to the ") }
           withLink(LinkAnnotation.Clickable(tag = "TERMS", linkInteractionListener = noop)) {
               withStyle(linkStyle) { append("Terms") }
           }
           withStyle(SpanStyle(color = textColor)) { append(" & ") }
           withLink(LinkAnnotation.Clickable(tag = "PRIVACY", linkInteractionListener = noop)) {
               withStyle(linkStyle) { append("Privacy Policy") }
           }
       }

       Text(
           text = text,
           style = MaterialTheme.typography.bodySmall.copy(textAlign = TextAlign.Center),
       )
   }
   ```
   Add `import androidx.compose.ui.text.LinkInteractionListener`.
   The `linkInteractionListener` is intentionally a no-op so behaviour matches today (tapping does nothing) while the deprecation is gone.

### Validation
- Build emits no `ClickableText` deprecation warning.
- "Terms" and "Privacy Policy" still render underlined in the primary colour, centred.
- Tapping them does nothing (unchanged) and does not crash.
- TalkBack exposes the two spans as links.

### Risks
- None significant — `LinkAnnotation` is the supported replacement and is available in the pinned Compose BOM.

---

## Global validation

Run after **each** section (tests first, then lint):

```powershell
.\gradlew.bat :shared:testDebugUnitTest :androidApp:testDebugUnitTest :androidApp:assembleDebug
.\gradlew.bat :shared:lintDebug :androidApp:lintDebug
```

Both must be green before moving to the next section. Extend the nearest existing test rather than adding broad new suites (`CLAUDE.md`). Verify each changed screen on **both** phone (bottom bar) and tablet (navigation rail) layouts, and in **light and dark** themes.

## Accessibility requirements (apply throughout)

- Keep the ↑/↓ chevron reorder fallback (S11-1) — drag is an enhancement, not a replacement.
- All new interactive controls keep ≥48dp targets and meaningful content descriptions.
- Swipe actions (S11-4) must not be the *only* way to edit/delete — the overflow menu remains.
- Destructive paths stay non-colour-only (icon + label + confirmation), per S10.

## Suggested commit boundaries

One commit per section keeps each independently reviewable and revertible:
1. `S11-1 drag-to-reorder in editors`
2. `S11-2 used-in-N-sessions reverse link + delete warning`
3. `S11-3 session item notes on detail`
4. `S11-4 swipe actions on list cards`
5. `S11-5 collapsing app bar on detail`
6. `S11-6 decorative login mark for TalkBack`
7. `S11-7 ClickableText → LinkAnnotation`
