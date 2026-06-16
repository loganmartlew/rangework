## Login Screen Review

### What's Working

The dark theme renders cleanly, the Google sign-in button is clearly identifiable, and the two-card structure loosely separates "what this app is" from "how to get in." The app logo and headline set a confident tone.

---

### Issues and Recommendations

#### 1. Two cards, duplicated purpose — collapse into one

The current layout splits the screen into a "Welcome" card and a "Google sign-in" card, but they both serve the same moment: a first-time or returned user deciding whether to sign in. The two-card structure implies these are separate decisions, but they're not.

**Recommendation:** Merge into a single card. Lead with the value proposition, follow immediately with the sign-in button. The visual separation creates unnecessary scrolling and makes the CTA feel secondary.

---

#### 2. Redundant copy — "What you'll do here" block

The nested sub-card inside the Welcome card repeats almost verbatim what the paragraph above it already says. "Shape reusable practice units, turn them into session templates" is a restatement of "Create repeatable practice units, build session templates from them." It adds length without adding information.

**Recommendation:** Remove the nested sub-card entirely. The headline + one paragraph is sufficient. If you want three scannable points, use a compact bullet or icon-row treatment — not a second card.

---

#### 3. "Signed out." is low-signal and slightly awkward

The label beneath "Pick up where you left off" reads "Signed out." — this is a state description, not user-oriented information. A first-time user doesn't understand what they're being told. A returning user doesn't need to be told they're signed out; they already know.

**Recommendation:** Remove "Signed out." entirely, or replace it with something action-oriented if state context matters — e.g. "Your sessions are waiting." (for returning users) or nothing at all (for new users).

---

#### 4. Sign-in button isn't a filled button — it should be

The Google sign-in button uses an outlined style (border, no fill). In Material 3, the **filled button** is the highest-emphasis action on a screen. On a login screen, signing in _is_ the primary — and only — action. An outlined button signals secondary importance.

**Recommendation:** Use `ButtonDefaults.filledButton` (or `Button` with `FilledButton` composable) for the sign-in action. This is also consistent with Material 3's guidance for auth screens where there's a single prominent action.

---

#### 5. "Google sign-in" chip label is redundant

The teal chip at the top of the second card reads "Google sign-in" — but the button directly below already says "Sign in with Google" and carries the Google logo. The chip adds no new information.

**Recommendation:** Remove the chip. If you need a section label on a merged card, a simple `titleMedium` text style for the heading is cleaner. Alternatively, drop the heading and let the button speak for itself.

---

#### 6. Headline hierarchy: "Pick up where you left off" competes with "Plan sharper range sessions"

Both cards use a large display-style headline. Two competing headlines at the same visual weight fragment attention rather than directing it. The user's eye doesn't know which to prioritise.

**Recommendation:** On the merged card, use one strong headline. Demote the sign-in section to a supporting role with a smaller `titleMedium` or `bodyLarge` label if any label is needed at all.

---

#### 7. App icon placement and size

The icon is large and positioned top-left inside the card, which gives it visual weight typically reserved for primary content. On a login screen, the icon functions as a brand mark — it should be centred (or at least more compact) to feel intentional rather than dropped in.

**Recommendation:** Centre the app icon above the headline, reduce to ~56–64dp, and let it anchor the card visually rather than compete with the text.

---

### Priority Order

| Priority | Issue                                       |
| -------- | ------------------------------------------- |
| High     | Merge the two cards                         |
| High     | Make the sign-in button filled              |
| High     | Remove the nested sub-card (redundant copy) |
| Medium   | Remove "Signed out." label                  |
| Medium   | Remove "Google sign-in" chip                |
| Low      | Resolve dual-headline competition           |
| Low      | Reposition/resize app icon                  |

---

### Net Effect

The current screen asks users to read more than they need to and buries the only action they can take. The changes above would reduce the screen to: icon → headline → one-line value prop → filled sign-in button. That's the entire job of a login screen done in one pass.
