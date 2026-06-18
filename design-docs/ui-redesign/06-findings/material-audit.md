# Rangework — Material 3 Design Review

13 screens reviewed across all 4 tabs + sign-in. Visual identity preserved throughout.

**Legend:** 🔴 Violation · 🟢 Opportunity · 🟡 Accessibility · 🟣 Consistency

---

## Navigation bar

🔴 **Active indicator pill is missing.** M3 Navigation Bar requires a 64×32dp pill-shaped indicator behind the active icon. The current selected state is communicated only through label weight and colour — the icon has no container. Fix: add a `NavigationBar` composable with default token colours; the pill renders automatically.

🔴 **Bar height is under-spec.** M3 specifies 80dp for Navigation Bar. The current bar appears closer to 64dp, compressing the icon/label padding. The 16dp clearances above icon and below label are required to hit the 48×48dp touch target per destination.

🟣 **Label capitalisation.** Minor — the existing sentence-case approach is fine for M3, but ensure it's applied uniformly across all destinations including any future additions.

---

## Top app bar

🔴 **No M3 Top App Bar variant is matched.** List screens show "Rangework · Units" as a single line with logo. This is a custom hybrid — it doesn't correspond to Small (56dp), Medium (112dp), or Large (152dp) variants. Fix: use `TopAppBar` (Small) with a `navigationIcon` slot for the back arrow on drill-in screens, and the screen title only (drop the app name from the bar — that's what the nav bar destinations are for).

🔴 **Edit and Delete actions are full-width buttons below the title.** M3 Small Top App Bar provides a trailing area for up to 3 icon buttons. Edit (pencil) and Delete (trash) belong there, not as large capsule buttons consuming ~80dp of vertical space before any content appears.

🟢 **Medium/Large App Bar opportunity.** Unit and Session detail screens have display-size titles that are good candidates for `MediumTopAppBar` with `exitUntilCollapsed` scroll behaviour. The title would scroll into the bar, recovering ~60dp of working space.

---

## FAB

🔴 **FAB shape doesn't match M3 CornerLarge (16dp).** The current FAB appears to use a custom squircle or very tight radius. M3 standard FAB uses `ShapeKeyTokens.CornerLarge` = 16dp, producing a noticeably rounded square. Fix: use `FloatingActionButton {}` with default shape — no override needed.

🔴 **FAB colour is wrong.** The FAB container is a dark grey surface, not `primaryContainer`. M3 mandates: FAB background = `primaryContainer`, icon = `onPrimaryContainer`. The app's green palette should be the FAB's fill, making it the visual anchor of each list screen.

🟢 **Extended FAB on sparse lists.** With 1 item, the primary task is clearly to create another. An `ExtendedFloatingActionButton("New unit")` surfaces the action explicitly. Collapse to a standard FAB via `listState.firstVisibleItemIndex > 0`.

🟡 **FAB occludes list items.** When a list grows, the last card's overflow menu (⋮) will sit behind the FAB with no way to tap it. Fix: `contentPadding = PaddingValues(bottom = 96.dp)` on the `LazyColumn`.

---

## Cards

🔴 **Instruction preview text has identical visual weight to the subtitle.** On unit list cards, three lines (title · "3 instructions · 3 balls" · inline instruction text) are rendered at near-equal prominence. Fix: instruction text to `bodySmall` (12sp) in `onSurfaceVariant`.

🔴 **Card container colour is a custom dark surface, not an M3 surface token.** Use `CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)` for filled cards, or switch to `OutlinedCard` for a lighter, more differentiated surface.

🟢 **Overview stat sections lack card structure.** "PRACTICE UNITS / 1" and "SESSION TEMPLATES / 1" are plain text. Wrapping each in a `Card` with an icon, a large numeric display, and subtitle would make the overview scannable and follow M3's information-density conventions.

🟣 **Section containment strategy is inconsistent.** Detail screens use card-per-section. The Overview screen uses raw text sections. Decide one pattern and apply it everywhere — the card-per-section approach is the stronger choice.

---

## Forms (Edit unit / Edit session)

🔴 **Text field rest state has no visible border.** M3 `OutlinedTextField` requires a 1dp outline using `OutlineVariant` at rest. The current fields show only a container fill with floating label. Either restore the outline or switch consistently to the filled `TextField` variant (underline indicator only).

🔴 **Ball count is a free-form text field.** This should be a number input at minimum (`keyboardType = KeyboardType.Number`) and ideally a `+/−` stepper row — icon button left, count display centre, icon button right. Steppers are range-safe and M3-idiomatic for bounded integers.

🔴 **Repeat count has the same problem.** Same stepper treatment, minimum value 1.

🔴 **Instruction reorder uses ↑/↓ chevron buttons, not drag-to-reorder.** M3 and Android convention for list reordering is a drag handle (≡ grip) activating a drag gesture. Chevron buttons require O(n) taps for long lists. Fix: `ReorderableColumn` with a `DragHandle` icon. Keep chevrons as an accessibility fallback.

🔴 **Save button is muted grey — not a primary action colour.** M3 `FilledButton` (primary container) is reserved for the single most important action on a screen. "Save unit" and "Save session" should use `Button(onClick = onSave)` with the app's green as the container. Currently these buttons are visually indistinguishable from neutral surfaces.

🟢 **"Add instruction" button.** Replace the tonal small button with a full-width `TextButton` with a leading `+` icon — the M3 inline list-item-addition pattern (used in Google Tasks, Keep).

🟡 **No confirmation on Delete.** No confirmation dialog or undo snackbar is visible. Destructive, unrecoverable actions require either an `AlertDialog` (title: "Delete unit?" · Cancel + Delete buttons) or a `Snackbar` with "Undo". The current design allows instant data loss with a single tap.

🟣 **Delete button pattern is custom but internally consistent.** `OutlinedButton` with error-coloured text and border is the correct M3 approach for destructive secondary actions — just ensure `contentColor = colorScheme.error` and `borderColor = colorScheme.error` are used via tokens, not hardcoded values.

---

## Lists

🔴 **⋮ overflow menu is the only interactive element on list cards.** Cards should be tappable (→ detail screen); the ⋮ menu should be a shortcut for Edit/Delete, not the only path to them. The current design hides all actions and makes the card's purpose unclear.

🔴 **Club list section headers ("Woods", "Hybrids") are not M3 List Subheaders.** They render as body text. M3 list subheader: `Text(text = "Woods", style = typography.labelLarge, color = colorScheme.primary, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))`.

🟡 **Switch touch targets may be under 48dp.** Each club row has no visible padding between it and adjacent rows. M3 `ListItem` defaults to 56dp height for one-line rows, which satisfies the requirement — confirm this is being used and not overridden.

🟡 **Switches have no content descriptions.** TalkBack will read only the switch state, not the club name. Set `Modifier.semantics { contentDescription = "$clubName, ${if(checked) "enabled" else "disabled"}" }` on the switch, or use `Modifier.toggleable` on the full row.

---

## Empty states

🔴 **No empty state exists.** When a new user first opens Units or Sessions, the screen is blank except for the FAB. M3 specifies: icon/illustration + headline + supporting text + primary CTA button. This is also the primary onboarding moment.

---

## Surface hierarchy

🔴 **Elevation tones are unused — all surfaces are effectively flat.** M3 uses additive surface tints (`surfaceContainer`, `surfaceContainerLow`, `surfaceContainerHigh`) to express depth. Currently the app uses near-identical custom dark values throughout, making layering relationships ambiguous. Let the M3 token system do this work automatically.

🟣 **Detail section cards (Summary, Focus, Instructions) have insufficient contrast from the page background.** They appear as inset blocks but the tonal delta is minimal. Use `surfaceContainerHigh` for section cards against a `surfaceContainer` page background.

---

## Spacing & touch targets

🟡 **↑/↓/🗑 icon buttons on instruction rows have undersized touch targets.** Icon buttons appear at ~24dp with minimal surrounding padding. Fix: `IconButton(modifier = Modifier.size(48.dp))` or `Modifier.minimumInteractiveComponentSize()` on each.

🔴 **Form field vertical spacing is inconsistent.** Gaps between fields vary between ~8dp and ~16dp with no apparent grid alignment. Fix: `Column(verticalArrangement = Arrangement.spacedBy(16.dp))` for top-level fields; 12dp between sub-fields within one instruction block; 20dp between instruction blocks.

---

## Sign-in screen

🟢 **CTA may require scrolling.** The marketing card is tall and the sign-in button is in a second card below. Fix: shorten the hero block (icon + headline + two-line tagline) and anchor the sign-in card to the bottom of the screen using `Modifier.weight(1f)` so the action is always visible without scrolling.

🔴 **"Sign in with Google" button doesn't follow Google's Identity Branding Guidelines.** The button must conform to Google's spec: white fill, Google logo, dark text. Custom-styled outline buttons are not permitted by Google's branding requirements. Use the official Credential Manager `GetSignInWithGoogleOption` UI or construct the button exactly per Google's identity guidelines.

---

## Summary

| Category             | Count |
| -------------------- | ----- |
| M3 violations        | 14    |
| M3 opportunities     | 7     |
| Accessibility issues | 6     |
| Consistency issues   | 5     |

**Highest-priority fixes:**

1. FAB colour → `primaryContainer` (green fill)
2. Navigation bar → active indicator pill
3. Save button → `FilledButton` (primary)
4. Numeric fields → stepper controls
5. FAB list padding (`contentPadding = PaddingValues(bottom = 96.dp)`)
6. Empty states for Units and Sessions
7. Cards tappable → detail screen
8. Delete confirmation dialog
