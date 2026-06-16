# Rangework — Consistency Audit

Screens reviewed: Sign-in · Overview · Units list · Unit detail · Edit unit · Sessions list · Session detail · Edit session · Settings (two scrolls) · Clubs list

---

## Critical (breaks trust or causes task failure)

### C1 — Edit action placement is inconsistent across detail screens

**Unit detail** (screen 2) exposes two full-width outline buttons — _Edit unit_ and _Delete unit_ — directly below the title, before any content.

**Session detail** (screen 6) does the same — _Edit session_ and _Delete session_ — in the same position and the same style.

That consistency is good. The problem is that **Units list** and **Sessions list** (screens 1, 5) place the same actions behind a three-dot overflow menu on the card. Two entry points exist for the same destructive action (delete), with no visual or interaction parity between them.

**Recommendation:** Make the overflow menu the single home for Edit and Delete on both list cards, and remove the side-by-side button row from detail screens. Replace it with an Edit icon or menu in the top-right of the app bar (standard M3 pattern). Delete stays in the overflow or a confirmation dialog, never as an equal-weight button next to Edit.

---

### C2 — "Delete" button uses destructive color on detail screens but not on edit screens

On Unit detail and Session detail, _Delete unit_ / _Delete session_ are rendered in the app's coral/salmon error color with an outline style. On Edit unit (screen 3–4), the per-instruction delete icon is a plain icon with no color emphasis. On Edit session (screen 8), the item delete icon is also plain.

The same action — deletion — carries a visual warning in one context and none in the other.

**Recommendation:** Destructive actions always use the error color token (the coral already in use). Apply it to all delete icons inside edit forms, not just to the top-level delete buttons on detail screens.

---

### C3 — "Save" button style is inconsistent between unit and session editors

**Edit unit** (screen 4): _Save unit_ is a full-width, filled, mid-gray button.  
**Edit session** (screen 8): _Save session_ is also full-width but visually lighter — appears more like a tonal or outlined button.

These are the same action at the same hierarchy level. If only one save button is on screen at a time, it should always be the primary filled button using the same surface and text color.

**Recommendation:** Use a single filled primary button style for all save/confirm actions. In M3 terms: `FilledButton` with the primary color. Apply it everywhere a form is committed.

---

## Serious (friction or inconsistent learnability)

### S1 — Card structure differs between Units list and Sessions list

**Units list card** (screen 1): Title → subtitle line (instruction count + ball count) → instruction preview as flowing inline dots.  
**Sessions list card** (screen 5): Title → subtitle line (item count + ball count) → unit name list.

The subtitle line is structurally equivalent (good) but the preview row differs: Units uses a flowing inline dot-separated string of instruction text; Sessions uses a plain unit name. These should follow the same template since they serve the same purpose (preview the content before opening).

**Recommendation:** Standardise the card template: Title (M3 `titleMedium`), one metadata line (count + ball count, dot-separated), one preview line (first 1–2 instruction or unit names, truncated with ellipsis). Both card types should use the same surface color and corner radius.

---

### S2 — Section header style is inconsistent across screens

**Settings** (screen 9): Section headers (ACCOUNT, APPEARANCE, UNITS, CLUBS, ABOUT) are uppercase, small, `labelSmall` or caption weight — an M3 "preference group" pattern.  
**Unit detail / Session detail** (screens 2, 6): Section headers (Summary, Focus, Instructions, Session items) are set in a larger, heavier style that looks like `titleMedium` inside a card.  
**Edit unit / Edit session** (screens 3, 7): "Instructions" and "Session items" are again set in a different weight — appears to be `titleSmall` or `bodyLarge` bold, without card containment.

Three visual treatments for the same concept (section heading).

**Recommendation:** Adopt two header roles only. (1) **Screen-level section labels** (used in Settings and in edit forms): `labelMedium`, uppercase, `onSurfaceVariant`, no card wrapping. (2) **Card-internal section headers** (Summary, Focus, Instructions in detail views): `titleMedium`, sentence case, inside the card surface. Never mix the two.

---

### S3 — Instruction numbering is inconsistent between detail and edit views

**Unit detail** (screen 2): Instructions labelled "Instruction 1", "Instruction 2", "Instruction 3" with dividers between them, inside one card.  
**Edit unit** (screens 3–4): Each instruction is its own card with its own "Instruction N" heading — so numbering and containment both change between view and edit mode.  
**Edit session** (screens 7–8): Items use "Session item 1" as a card heading, matching the edit-unit pattern.

The label pattern ("Instruction N" vs "Session item N") is at least parallel. The inconsistency is in how instructions are grouped: one card in view mode, N cards in edit mode.

**Recommendation:** In view mode, keep all instructions in one card with dividers (current approach is fine). In edit mode, keep each instruction as its own card but visually distinguish the card from the top-level form surface more clearly (slightly elevated or with a distinct `surfaceVariant` color). The "Instruction N" label on each card should match the label weight used in the view screen.

---

### S4 — "Add instruction" button style is inconsistent with "Add item" in session editor

**Edit unit** (screen 4): _Add instruction_ is a small tonal/filled button, left-aligned, at the bottom of the instructions section.  
**Edit session** (screen 7): _Add item_ is a small filled button placed inline with the "Session items" section header, right-aligned.

Two buttons that create new child records use different alignment, different positional anchoring, and possibly different visual weight.

**Recommendation:** Place all "add child record" buttons consistently: inline with the section header, right-aligned, using a `FilledTonalButton` (M3). This matches Android conventions and is already done on the session editor — standardise the unit editor to match.

---

### S5 — Ball count summary surface differs between edit screens

**Edit session** (screen 8): "Balls — 18 balls" appears in its own card, clearly separated, at the bottom.  
**Edit unit** (screen 3–4): No equivalent running total exists during unit editing. Ball counts are only shown inside each instruction's "Ball count" field.

While units don't have the same compositing logic as sessions, showing a running unit total during editing would be consistent with the established pattern and would reduce cognitive load.

**Recommendation (minor scope add):** Show a small total beneath the instructions list in the unit editor — same card style as the session editor's Balls card. Label: "Total balls: N". Not blocking, but a natural extension of the existing pattern.

---

## Moderate (visual roughness, not functionally broken)

### M1 — Top app bar breadcrumb pattern is applied inconsistently

**Units list, Sessions list, Settings, Overview** (screens 1, 5, 9, 13): Top bar shows `Rangework · [Screen name]` with the app logo to the left — a breadcrumb/label pair.  
**Unit detail, Edit unit, Session detail, Edit session** (screens 2, 3, 6, 7): Top bar shows back arrow + `[Screen name]` only — no app name, no logo.

These are two different top-bar patterns used side by side in the same navigation stack. The logo-and-breadcrumb style appears on top-level destinations; the back-arrow style on secondary screens. That is actually a reasonable pattern (M3 distinguishes top-level from secondary screens), but the transition feels jarring because the logo/wordmark disappears entirely on secondary screens.

**Recommendation:** Keep the back arrow on all secondary screens (correct behavior). Remove the logo from the top-level screens' top bar and let the bottom navigation carry the current-tab identity. The large heading ("Units", "Sessions" etc.) already establishes where the user is — the repeated logo adds clutter.

---

### M2 — "Repeat count" control is a free-text field; should be a stepper

**Edit session** (screen 7): Repeat count is a plain text input field containing "6". This is a small integer (likely 1–10) that users will adjust frequently. A numeric keyboard is acceptable but a stepper (`+` / `–` with the value displayed between) or a number picker would reduce input friction and prevent invalid values (letters, negatives, fractions).

**Ball count** in Edit unit is the same problem — a text field where a numeric stepper would be more appropriate.

**Recommendation:** Replace both free-text numeric fields with M3-compatible steppers or, if the range is small, a segmented chip group (1× 2× 3× … 6×). At minimum, add `inputType = number` and a defined min/max.

---

### M3 — Reorder controls (up/down chevrons) are visually identical to navigation chevrons in dropdowns

**Edit unit, Edit session** (screens 3, 4, 8): Instructions and session items have up-arrow / down-arrow icon buttons for reordering.  
Throughout: Dropdown menus (Practice unit, Session club, Default club) use a downward chevron/caret.

The up/down reorder chevrons are visually similar to the dropdown-open affordance. On a small phone screen they can be misread.

**Recommendation:** Replace reorder up/down chevrons with a drag-handle icon (`drag_indicator` in the M3 icon set, three horizontal lines). This is the standard M3/Android reorder affordance. Use a long-press to enter reorder mode, or a persistent handle — either way, disambiguate from dropdown controls.

---

### M4 — Focus/Focus cue field label naming is inconsistent

**Unit detail** (screen 2): Section label is "Focus".  
**Edit unit** (screen 3): Field label is "Focus".  
**Edit session** (screen 8): Field label is "Focus cue".

These reference the same concept across the two editors, yet the label changes.

**Recommendation:** Pick one name and apply it everywhere. "Focus cue" is more descriptive and self-explanatory for a new user. Use it on the unit editor field, the unit detail card header, and the session item field.

---

### M5 — Overflow menu icon size appears inconsistent across card types

**Units list card** (screen 1): Three-dot icon is visually medium-weight.  
**Sessions list card** (screen 5): Three-dot icon appears similar but sits at the same vertical midpoint of a taller card — making it feel more loosely anchored.

**Recommendation:** Pin the overflow icon to `top = 12dp` on all cards regardless of card height, and use the same `IconButton` size (48×48dp touch target) consistently.

---

### M6 — "Item notes" and "Session notes" label wording is inconsistent

**Edit session top** (screen 7): Field label is "Session notes" (for the session-level notes).  
**Edit session item** (screen 8): Field label is "Item notes".

This is fine as a distinction, but within the unit editor:  
**Edit unit** (screen 3): Field label is simply "Notes".

All three fields do the same job (free-text annotation) at different scopes.

**Recommendation:** Apply the scope as a prefix consistently: "Unit notes", "Session notes", "Item notes". This makes the hierarchy legible at a glance and aligns with the naming logic already used for "Session club" vs "Default club".

---

## Low (polish / minor alignment)

### L1 — Overview card bottom padding is tighter than Units/Sessions list cards

**Overview** (screen 13): The "Next move" card and the stat cards have tighter bottom padding inside the card than the cards on the Units and Sessions list screens.

**Recommendation:** Standardise inner card padding to `16dp` all sides, or `16dp` horizontal / `12dp` vertical throughout. Pick one and apply globally.

---

### L2 — "Dynamic color" toggle in Settings is not present in the spec

The Rangework spec lists "Theme" (Light/Dark/System) as a setting but does not mention dynamic color. The toggle exists in the app (screen 9) but has no spec entry. If it's intentional, it should be documented and its position in the hierarchy confirmed (it currently sits below the Theme segmented button without a sub-label explaining what it does).

**Recommendation:** Add a one-line supporting text beneath the "Dynamic color" toggle: "Use colors from your wallpaper when supported."

---

### L3 — Sign-in screen has no bottom navigation but uses top-of-screen layout inconsistently with all other screens

The sign-in screen (screen 12) has no bottom navigation bar (correct — user is not authenticated) but uses a scrollable card layout with a top-of-screen wordmark that differs from the top bar style used everywhere else once authenticated.

This is intentional (onboarding vs. in-app) but the app name "Rangework" in the sign-in screen is set in a small, plain `bodyMedium` style, while inside the app it appears in the top bar as a `titleSmall` breadcrumb. The logo icon also changes size between the two contexts.

**Recommendation:** Not a functional issue. If brand consistency matters, ensure the app icon and wordmark lockup use the same proportions on the sign-in screen as inside the app.

---

## Summary: Recommended Canonical Patterns

| Category                            | Recommended pattern                                                                                              |
| ----------------------------------- | ---------------------------------------------------------------------------------------------------------------- |
| **Buttons — primary action**        | M3 `FilledButton`, primary color, full width at bottom of form                                                   |
| **Buttons — destructive**           | Error color token (coral), applied to all delete actions consistently, outline or tonal style                    |
| **Buttons — secondary/add child**   | `FilledTonalButton`, right-aligned, inline with section header                                                   |
| **Cards**                           | `surfaceVariant` fill, 12dp corner radius, 16dp internal padding all sides, consistent across Units and Sessions |
| **Card overflow menu**              | Always top-right, icon pinned to `top=12dp`, 48×48dp touch target                                                |
| **Section headers — screen level**  | `labelMedium`, uppercase, `onSurfaceVariant`, no card wrapping                                                   |
| **Section headers — card internal** | `titleMedium`, sentence case, inside card surface                                                                |
| **Typography — card title**         | `titleMedium`                                                                                                    |
| **Typography — card metadata**      | `bodySmall`, `onSurfaceVariant`, dot-separated                                                                   |
| **Edit vs. view actions**           | Edit: icon button in app bar top-right. Delete: overflow menu only                                               |
| **Reorder controls**                | `drag_indicator` handle icon, not up/down chevrons                                                               |
| **Numeric inputs**                  | Stepper control (not free-text) for small integers like ball count and repeat count                              |
| **Field naming**                    | Scope-prefixed: "Unit notes", "Session notes", "Item notes", "Focus cue" everywhere                              |
| **Navigation bar**                  | Bottom nav on all authenticated screens; no change needed                                                        |
| **Top app bar**                     | Back arrow on secondary screens, no logo; bottom nav carries tab identity on top-level screens                   |
