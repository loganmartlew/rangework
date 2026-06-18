## Rangework – Practice Sessions UX review

### How the current flow works

The path from nothing to a saved session is: **Sessions list → tap card → Session detail → Edit session → scroll through item form → Save**. That's three screens and a long scroll before anything is persisted. For a tool whose core job is quick pre-range planning, this friction accumulates fast.

---

### Findings, prioritised by ROI

---

#### 1. The session detail screen is a dead end (high ROI)

**What's happening.** When you tap a session card on the list, you land on a detail screen (screenshot 2) showing a Summary card and a Session items card. Both cards show almost identical information to what was already visible on the list card. The only new information is the repeat count and club for each item. The two action buttons — Edit session and Delete session — sit at the top, styled as equal-weight outline buttons.

**The problems.** This screen exists purely as a waypoint. A user who tapped a card to edit it now has to tap again. A user who tapped to review it sees almost nothing new. The detail screen adds a navigation step without adding meaningful value.

**The fix.** Remove the standalone detail screen. On tap, go directly into the edit/view form. If you want a lightweight review mode without accidental edits, a bottom sheet or modal showing the breakdown is enough. The list card itself could be expanded to show item detail on tap, avoiding a full navigation push. This saves one tap every time a user touches a session.

---

#### 2. The session item form buries the most important field (high ROI)

**What's happening.** In the Edit session form (screenshots 3 and 4), each Session item expands to show: Practice unit (dropdown), Repeat count (text field), Session club (dropdown), Item notes (text area), Focus cue (text area). The ball count summary appears at the very bottom of the item card, below the Focus cue field.

**The problems.** Repeat count is the most consequential field — it directly drives the ball total — but it sits after the unit picker and before two optional text fields. A user scanning to adjust how many times they'll run a drill has to hunt past the club dropdown to find it. Worse, the ball count feedback ("18 balls · Club: 7-Iron") is tucked at the bottom of the card, below the Focus cue field, making it hard to see the impact of changes without scrolling.

**The fix.** Reorder the item card: unit → repeat count → live ball count for this item → then club/notes/focus cue collapsed or in a secondary section. Surfacing the ball impact immediately after the repeat count creates a tight feedback loop. The optional fields (notes, focus cue) can live behind an "Add details" expand or simply deprioritised visually by reducing their prominence.

---

#### 3. The session list's empty and sparse states give no guidance (high ROI)

**What's happening.** The Sessions list with one item (screenshot 1) shows the card, a FAB in the bottom-right corner, and nothing else. There's no empty state shown (presumably a blank screen), and on the sparse screen there's no contextual hint about what sessions are for or what to do next.

**The problems.** A new user who lands here has no prompt. The FAB is the only creation affordance and it carries no label. For a user who hasn't yet grasped the unit → session hierarchy, there's no scaffolding here. The three-dot overflow menu on the card (which presumably holds Edit, Delete, Duplicate) is invisible until you already know to look for it.

**The fix.** Add a proper empty state with a brief description ("Combine your practice units into a full session plan") and a primary action button. On the sparse list, the three-dot menu's options — especially Duplicate — should be more discoverable. Duplicate is a high-value shortcut for users who want to vary an existing session, but it's hidden two taps deep.

---

#### 4. Session creation requires you to already have units built (medium ROI)

**What's happening.** When building a session item, the Practice unit field is a dropdown that lists existing units. If you haven't built any units yet, the dropdown is empty and you're stuck.

**The problems.** The dependency on pre-existing units is invisible until you're mid-creation. A first-time user who taps the FAB on Sessions, adds an item, and finds an empty unit dropdown will be confused about why the session can't be completed and what they need to do first.

**The fix.** If the unit dropdown is empty, surface a contextual prompt inline: "No practice units yet — create one first" with a link directly into the unit creation flow. Even better, if the user has zero units and taps the FAB on Sessions, intercept with a brief explanation and route them to Units first. The two-level hierarchy is the app's core concept and it needs to be explained at the moment it matters.

---

#### 5. The total ball count is not visible during item editing (medium ROI)

**What's happening.** The spec says the app should show a "live running ball count total while building the session." In the screenshots, the Balls summary card ("18 balls") appears at the very bottom of the Edit session screen (screenshot 4), below all the item cards.

**The problems.** On a session with multiple items, the ball total is off-screen while you're editing any item that isn't the last one. This defeats the purpose of the live count — users who are adjusting repeat counts to hit a target total (say, 100 balls) have to scroll to the bottom after each change to see the effect.

**The fix.** Pin the ball total to the top of the Edit session screen or make it sticky, perhaps in a compact chip or subtitle under the screen heading. As units are added or repeat counts change, the total updates in place without the user needing to scroll. This is especially impactful for the core planning use case.

---

#### 6. The Edit/Delete buttons on the detail screen use the wrong visual weight (medium ROI)

**What's happening.** On the Session detail screen (screenshot 2), Edit session and Delete session are rendered as equal-weight outline buttons side by side, with Delete using the error/destructive colour (salmon/red text).

**The problems.** Two equal-weight buttons for actions of very different consequence is a known usability hazard. Delete is visually prominent — the colour draws the eye — despite being the less-frequent and more dangerous action. On Android/Material 3, destructive actions should either require a second confirmation or be visually subordinate to the primary action.

**The fix.** If the detail screen is retained, make Edit session the primary filled button and Delete session a text button or icon in the top app bar with a confirmation dialog. If the detail screen is removed (per finding 1), this problem goes away.

---

#### 7. "Session item N" as a label adds no value (low ROI)

**What's happening.** Each item card in the Edit session form is headed "Session item 1", "Session item 2" and so on.

**The problem.** Once a unit is selected, the unit name is the meaningful identifier. "Session item 1" above a card already showing "3 Shot Shaping Drill" is redundant scaffolding.

**The fix.** Replace the generic label with the unit name once one is selected, or remove the label entirely and let the unit dropdown be the heading of each card. This reduces visual noise and makes multi-item sessions easier to scan.

---

#### 8. The reorder controls are low-contrast and hard to target (low ROI)

**What's happening.** At the bottom of each session item card (screenshot 4) there are up-arrow, down-arrow, and delete icon buttons.

**The problems.** These sit at the bottom of a tall card, after all the form fields — they're not where a user's eye naturally goes when thinking "I want to move this drill up." On Material 3, drag-to-reorder handles at the leading edge of each card would be more discoverable and faster to use than explicit up/down buttons.

**The fix.** Replace the up/down arrows with a drag handle (the standard `≡` grip icon) on the leading edge of each item card. This is the platform-native pattern for list reordering and requires far less precision than small icon buttons stacked vertically.

---

### Summary table

| Finding                                                    | Impact | Effort |
| ---------------------------------------------------------- | ------ | ------ |
| Remove the session detail screen                           | High   | Medium |
| Reorder item form: repeat count + live ball count up top   | High   | Low    |
| Empty/sparse state with guidance and discoverable overflow | High   | Low    |
| Surface unit dependency before creation starts             | Medium | Low    |
| Pin ball total to top of Edit session screen               | Medium | Low    |
| Fix Edit/Delete button hierarchy                           | Medium | Low    |
| Replace "Session item N" labels with unit names            | Low    | Low    |
| Replace up/down arrows with drag handles                   | Low    | Medium |

The top three alone remove one full navigation screen, surface the most important field, and give new users a starting point — all without touching the data model or the Material 3 design language.
