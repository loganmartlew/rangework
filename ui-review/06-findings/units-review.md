# Rangework — Practice Units UX Review

---

## Screen 1: Units List

**What works well**

The card surfaces the three most useful pieces of information — title, instruction count, ball count, and a preview of the instruction labels — in a compact footprint. The FAB placement is correct for Android and the bottom nav is clear.

**What is confusing**

The instruction preview line ("7-Iron • Hit a shot with fade spin • Hit a shot with draw spin • Hit a straight shot") blends the default club with the instruction text using identical separators and identical weight. The club is a different category of information from the instructions, but it reads as just another list item. A user skimming multiple cards won't be able to extract the club at a glance.

The three-dot overflow menu is the only visible action. There's no hint from the card itself that tapping the card body opens a detail view. That's a missing affordance — the card looks like it might just be information.

**What feels visually weak**

The "3 instructions • 3 balls" metadata line and the instruction preview text are nearly the same size and weight, so there's no clear hierarchy between the summary stats (which are the most scannable) and the raw instruction text (which is secondary detail). The card feels like a wall of similar-weight text.

**What creates unnecessary effort**

To edit a unit, the user must: tap the overflow → tap Edit, or tap the card → tap "Edit unit". That's always two taps minimum. A swipe-to-reveal Edit action would cut this to one gesture for the most common action on a list item.

To delete, the same two-tap minimum applies, with the destructive action sitting one tap away from the overflow with no visual differentiation from Edit.

**Mobile UX principles violated**

The overflow menu hides both Edit and Delete with no visual distinction between them. Material 3 guidance recommends that destructive actions like Delete be visually separated or confirmed, not presented identically to non-destructive actions in a flat list.

---

## Screen 2: Unit Detail

**What works well**

The card-per-section layout (Summary, Focus, Instructions) gives clear visual grouping. The summary card shows the most important metadata at a glance. Instructions are individually separated with dividers, which is correct when order matters.

**What is confusing**

"Edit unit" and "Delete unit" are rendered as two equal-weight outlined buttons, side by side, immediately below the title. Delete is a destructive, irreversible action; it should not be visually equivalent to Edit. The red text colour is the only distinction — but colour alone is not sufficient affordance, especially in bright light conditions.

The "Instruction 1 / Instruction 2 / Instruction 3" labels are redundant. The number is implied by position and the divider. These labels add visual noise without adding meaning.

"Balls: 1" is the most operationally important number in each instruction — it determines the user's physical preparation at the range. It is currently the smallest, lightest element inside each instruction row, making it the hardest thing to read at a glance. The hierarchy is inverted from what the user actually needs.

**What feels visually weak**

The Instructions card is a large, undifferentiated block. With three instructions, it's manageable. With eight or ten instructions, it will become extremely hard to scan. There's no visual chunking within the card to help a user locate instruction 5 of 9 quickly.

**What creates unnecessary effort**

A user who arrives at this screen to make a quick edit (e.g., change a ball count) must tap "Edit unit", scroll to find the instruction, update the value, scroll to "Save unit", and tap Save. There is no inline editing.

**Mobile UX principles violated**

Two-button destructive action placement violates the principle of error prevention. The standard Android pattern is to surface Delete in an overflow or require a confirmation dialog for destructive actions — not to give Delete equal prominence with Edit as a primary action button.

---

## Screen 3: Edit Unit (top half)

**What works well**

The form is logically ordered — metadata fields first, then instructions. The club dropdown is appropriately scoped to the user's configured bag. Having Notes and Focus as separate fields is correct; they serve different purposes.

**What is confusing**

"Notes" is an empty, multi-line field with no placeholder content, helper text, or example. For a first-time user, it's unclear what belongs here versus in "Focus". The distinction between Notes (contextual, longer-form) and Focus (a single mental cue) is meaningful but unexplained. A short helper text line under each field would resolve this.

The "Instructions" heading appears visually outside the card containing the instruction fields, but it's actually introducing what follows. It looks like a section header that belongs to the screen, not to the card — this is a layout ambiguity.

**What feels visually weak**

The "Instruction 1" label is bold but small, and the card containing it has the same weight/colour as the surrounding fields. The boundary between "this is a unit-level field" and "this is an instruction-level field" is visually weak. A user editing instruction 2 while instruction 1 is partially visible above might not immediately understand what they're looking at.

**What creates unnecessary effort**

Ball count is a free-text numeric field. Users editing a ball count must tap the field, clear the existing number, and type a new one. A stepper (+/−) would be significantly faster for single-digit counts, which appear to be the common case.

**Mobile UX principles violated**

Full-width multi-line text fields for Notes with no character guidance or minimum size cue create form fields that feel unbounded. On mobile, unbounded fields increase uncertainty about how much the user is expected to write.

---

## Screen 4: Edit Unit (bottom half — instruction cards)

**What works well**

The "Add instruction" button is correctly placed at the bottom of the instruction list, immediately after the last item, so it reads as "append to this list." The "Save unit" button is visually distinct (lighter/filled) from "Add instruction," which is correct — they're different action types.

**What is confusing**

Each instruction card has three icon buttons: up arrow, down arrow, delete. The up/down arrows are the only way to reorder instructions. On a form with many instructions, this is slow and error-prone — moving instruction 1 to position 5 requires four taps. There's no drag-to-reorder handle despite the spec explicitly calling out that instructions are ordered.

The delete icon in each instruction card is the same icon as the one that appears in the app bar area (trash). In this context it's permanently visible with no confirmation — one tap deletes the instruction immediately. Given that instructions contain the user's written text, this is a meaningful loss with no undo path visible.

**What feels visually weak**

The three icon buttons (up / down / delete) are placed at the bottom-right of each instruction card and are close together. The delete icon is visually similar in size and weight to the reorder arrows. There's no visual grouping that separates "reorder" (non-destructive) from "delete" (destructive).

**What creates unnecessary effort**

Reordering via tap-arrows is the highest-friction interaction in the entire form. A drag handle on each card (which is the universal Android pattern for reorderable lists) would reduce reordering from N taps to one drag.

The ball count field being a full text-entry field is repeated here — same issue as noted above.

**Mobile UX principles violated**

Reorder via arrows instead of drag handles is an anti-pattern in Material 3. The guidelines explicitly provide a drag handle component for reorderable list items. The app is using a custom workaround for something the design system already solves.

---

## Flow-level evaluation

**Extra taps**

The Edit path from the list requires 2 taps (overflow → Edit, or card → Edit unit button). A contextual swipe action would save a tap on the most common list-level action. Every ball count edit requires 3+ gestures (tap field, clear, type). Instruction reorder costs 2 taps per position shift. None of these are blocking, but they compound over repeated use.

**Missing affordances**

The unit list card does not signal that it's tappable — no ripple hint, no chevron, no visual tap target indication. The instructions in the edit form have no drag handle despite being explicitly ordered. The Notes field has no affordance explaining its purpose or expected length.

**Missing feedback**

When "Save unit" is tapped, there is no visible save confirmation in the screenshots (a snackbar "Unit saved" or similar). If saving fails silently, the user has no indication. The delete action on an instruction has no confirmation or undo — if a user taps it accidentally, the text is gone.

**Missing hierarchy**

Ball counts are the most operationally important data point when following a unit at the range, yet they're the smallest element in both the detail view and the edit form. The instruction text and the ball count should be visually inverted from the current treatment — or at minimum given equal weight. In the list card, the club name and instruction text have equal weight despite serving different roles.

**Missing shortcuts**

There is no duplicate action for units (Sessions have duplicate; Units do not, per the spec). If a user wants to create a "50-yard pitch — 5 balls" variant of an existing "50-yard pitch — 10 balls" unit, they must create it from scratch. A duplicate-and-edit shortcut would be valuable given that practice drills often come in families.

---

## Prioritised improvements by impact

**High — fix these first**

1. **Add drag-to-reorder handles on instruction cards.** Reordering is a core edit action and the current arrow approach is significantly slower and less natural than what Android users expect. This is the single biggest usability gap in the feature.

2. **Visually separate Delete from Edit in the detail view.** Move "Delete unit" to the overflow menu or an icon button in the app bar, not a co-equal button next to Edit. This is an error-prevention issue with real data loss risk.

3. **Raise ball count visual prominence.** In the detail view, increase the ball count to at least bodyLarge, or add a pill/chip treatment so it reads as a distinct data element, not a footnote. This is the number the user needs to read at the range.

4. **Replace ball count text field with a stepper.** +/− stepper for single-digit counts (the likely default case) is faster, prevents non-numeric input, and fits one-handed use. Keep free-text entry as a fallback for larger counts or expose it on long-press.

**Medium — meaningful but not blocking**

5. **Add a drag handle and confirmation for instruction delete.** A simple "Undo" snackbar for accidental instruction deletion prevents data loss without adding a confirmation dialog to the happy path.

6. **Add swipe-to-edit (or at least swipe-to-delete) on the unit list.** This cuts the edit path from 2 taps to 1 gesture and makes the list feel interactive rather than passive.

7. **Add helper text to Notes and Focus fields.** A single line of helper text ("A longer description or context" / "A single mental cue to focus on during the drill") resolves the ambiguity between the two fields for new users.

8. **Add "Duplicate unit" to the unit overflow.** Mirrors the existing Sessions behaviour and reduces friction for users building drill families.

**Lower — polish and consistency**

9. **Differentiate club from instruction text in the list card.** Render the club with a chip or icon prefix so it reads as a different category from the instruction preview text.

10. **Remove "Instruction N" labels in the detail view.** Position is sufficient. Removing the labels tightens the layout and lets ball counts breathe.

11. **Add a tappable visual cue to unit list cards.** A subtle end-of-card chevron or ensuring the ripple is visible on tap makes the list feel interactive and sets the correct expectation that tapping opens detail.
