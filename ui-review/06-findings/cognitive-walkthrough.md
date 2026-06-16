I'll look at all the screenshots carefully before doing the walkthrough.Now let me look at the project spec file too.Good. I have a thorough picture of all screens. Here is the full cognitive walkthrough.

---

# Rangework — Cognitive Walkthrough

_Persona: 25 handicap amateur golfer. Comfortable with Android. No prior exposure to this app. Practices occasionally, used to just picking up a bucket and hitting._

---

## Sign-in screen (Screenshot 12)

**What I notice first**
The big headline "Plan sharper range sessions" and the app icon. Then the "Sign in with Google" button at the bottom of the screen. The two-card layout feels like an onboarding flow.

**What I think I can do**
Sign in. That's all that feels available.

**What is confusing**
The second card is titled "Pick up where you left off" and says "Signed out." That phrasing is for returning users, not someone opening the app for the first time. As a new user I haven't _left_ anywhere. It reads slightly off.

The first card has a "What you'll do here" sub-block that largely repeats what the headline already said in slightly different words. Two paragraphs saying essentially the same thing, neither of which explains what a "unit" or a "session" actually is in concrete terms. _"Shape reusable practice units, turn them into session templates"_ — I don't yet know what a unit or session template means.

**What is missing**
A single plain sentence like: _"First, create a drill. Then bundle drills into a session. The app counts your balls for you."_ That would orient me immediately.

**Hesitation moment**
None — there is only one action. I tap Sign in with Google.

---

## Overview screen (Screenshot 13)

**What I notice first**
"Welcome back" with my email address. Then two buttons: **New unit** and **New session**. Then two stat cards showing counts of 1 and 1. Then a "Next move" tip at the bottom.

**What I think I can do**
Create something. The two buttons make the primary actions clear.

**What is confusing**
"Practice Units" card says _"Saved building blocks ready to reuse"_ and the count is just a big number. The cards are tappable (presumably), but there's no visual affordance — no chevron, no underline, nothing to signal I can tap through to the list. I might not realise these are navigation shortcuts.

"SESSION TEMPLATES" — I haven't heard this term before. The app called them "Sessions" in the bottom nav but "Session templates" in this card. That's a small inconsistency that plants a seed of doubt about whether they're the same thing.

The "Next move" tip says _"Tighten the unit details first, then use sessions to string them into a repeatable practice block."_ As someone who just arrived, I don't know what "tighten" means in this context. The tip is directed at users who already have data, not newcomers.

**What is missing**
There's no empty-state guidance for a truly new user with zero units and zero sessions. What would this screen look like on day one? The current "Next move" section seems to be the only contextual nudge, but it assumes I already understand the model.

**Hesitation moment ①**
I see "New unit" and "New session" and I don't know which one to start with. Nothing on screen tells me I need to make a unit _before_ I can make a session. A 25 handicap player thinking about practice might instinctively tap "New session" first because a session sounds like _the thing I'm going to the range to do_.

---

## Task 1 — Create a practice unit

### Units list (Screenshot 1)

**What I notice first**
One existing card — "3 Shot Shaping Drill" — with its metadata. A floating **+** button in the bottom right. Bottom nav.

**What I think I can do**
Create a new unit (the + button), or tap the card to view it. The three-dot overflow on the card implies quick actions.

**What is confusing**
The card shows _"3 instructions • 3 balls"_ and then lists the instructions inline as a dot-separated run-on sentence. That works for short lists but would break down visually for a unit with 6–8 instructions.

The three-dot menu — I don't know yet whether it gives me Edit and Delete directly, or whether I have to go into the detail screen first. Some users will tap the three dots immediately; others will tap the card title.

**What is missing**
No empty-state message for when a user has no units yet. When there are zero units the list will just be a blank screen with a + button, giving no hint of what to do or why.

**Hesitation moment ②**
Where does tapping the card go versus tapping the three dots? I'm not sure, so I pause before deciding.

I tap **+** to create a new unit.

---

### Edit unit — top half (Screenshot 3)

**What I notice first**
"Edit unit" as both the top bar title and the large page heading — duplicated. Then four fields: Title, Notes, Focus, Default club.

**What I think I can do**
Fill in a title. The other fields are optional-feeling.

**What is confusing**

**"Focus"** — I have no idea what this means as a field label. Is it a category? A hashtag? A personal reminder? The placeholder text just says "Focus" with no hint. The detail view (Screenshot 2) calls it "Focus" with sample content "Club face control", which helps, but inside the edit form I have no example to anchor to. A 25 handicap golfer might think it's something technical and skip it, or type something random.

**"Default club"** — the dropdown already shows "7-Iron". That's likely carried over from an existing unit in the demo. For a brand-new unit, will this default to nothing, the most common club, or something else? If it defaults to a club the user didn't choose, their session ball counts could be silently wrong.

The layout puts Notes _before_ Focus and Default club. Most users will fill in the title then scroll past Notes to get to Focus and club. Notes feels like an afterthought field in real usage but sits in a prime position.

**Hesitation moment ③**
"Focus" label with no helper text. I either skip it (losing value) or guess at its purpose. A small hint like _"A mental cue for this drill — e.g. 'Slow backswing'"_ would remove this entirely.

---

### Edit unit — instructions section (Screenshots 3 & 4)

**What I notice first**
An "Instructions" heading, then cards for each instruction. Each card has an Instruction text field, a Ball count field, and three icons at the bottom: up arrow, down arrow, delete (trash).

**What I think I can do**
Type an instruction, set how many balls it uses, reorder with the arrows, delete with the trash.

**What is confusing**

**Ball count field** — the label is "Ball count" but it's a free-text field. I could type "lots" or "ten" and presumably the app would either crash or accept it silently. There's no numeric keyboard enforced (or at least none visible in the screenshot), no stepper, no + / − control. For a golf-specific number that drives the entire session total, this is surprisingly low-friction in the wrong direction. I might type a decimal or leave it blank and not realise the count is now broken.

**Up/down arrows for reordering** — on mobile, tap-to-reorder arrows are slow and error-prone when there are 4+ instructions. If I add 6 instructions and want to move the last one to first, that's 5 taps of the up arrow, with the screen scrolling each time and me losing track of where I am. There's no long-press drag affordance visible, which is the Android-native pattern for list reordering.

**"Instruction 1", "Instruction 2"** as section headers — these are positional labels, not descriptive ones. They don't tell me anything useful. Once I have 5 instructions labelled "Instruction 1" through "Instruction 5", the form becomes hard to scan.

**Hesitation moment ④**
I fill in my first instruction, see "Ball count" below it, and type "10". Then I look for a button to add the next instruction. It's not visible — I have to scroll all the way down past all existing instructions to find "Add instruction" (Screenshot 4). If I have 3 instructions already, that button is well below the fold. I might think there's no way to add more.

**Hesitation moment ⑤**
After filling in three instructions I scroll down and see "Add instruction" and then "Save unit" (Screenshot 4). The "Add instruction" button is styled with a green filled-pill style (primary action) while "Save unit" is styled as a lighter secondary-looking button. Visually, "Add instruction" is more prominent than "Save unit". A user might interpret Save as optional, or wonder if they need to tap "Add instruction" before saving.

The button hierarchy is inverted — Save should feel like the more dominant action at the end of the form.

---

## Task 2 — Create a session

### Sessions list (Screenshot 5)

**What I notice first**
One session card: "Test" — "1 item • 18 balls" — "3 Shot Shaping Drill". Floating **+** button. Same layout as Units.

**What I think I can do**
Create a new session with +, or tap the card to see it.

**What is confusing**
"1 item" — item of what? I know from context it means one practice unit, but "item" is abstract. A user reading this cold might wonder if it means one exercise, one club, one hole, one bucket. "1 unit" would be more precise and consistent with the app's own vocabulary.

**Hesitation moment ⑥**
Same affordance ambiguity as the Units list — three-dot vs card tap. No differentiation.

I tap **+**.

---

### Edit session — top section (Screenshot 7)

**What I notice first**
"Edit session" as both bar title and page heading (duplicated again). Name field, Session notes field, "Session items" label with an **Add item** button.

**What I think I can do**
Name the session, add notes, add items.

**What is confusing**
"Session items" — again, "item" is abstract. This is the first time I encounter this terminology in context. I haven't been told that "items" are practice units. The button says "Add item" not "Add unit". I might expect a generic checklist of things to bring to the range, not a structured drill selector.

**Hesitation moment ⑦**
I tap **Add item**. What happens? Based on the screenshot, a "Session item" block appears inline below — it doesn't open a picker dialog or a separate screen. This is good for speed but means I now have to figure out a dropdown to select a unit. If I haven't created any units yet, that dropdown will presumably be empty. There's no in-context message warning me of this. A new user who went to Sessions first would hit a dead end here and not know why.

---

### Edit session — item configuration (Screenshots 7 & 8)

**What I notice first**
Session item 1 has four fields: Practice unit (dropdown), Repeat count (number field), Session club (dropdown), Item notes. And then below that I see "Focus cue" and a summary line "18 balls • Club: 7-Iron".

**What I think I can do**
Pick a unit, set how many times I repeat it, pick a club for this session, add notes.

**What is confusing**

**"Repeat count"** — I put in "6" and it says 18 balls. The unit has 3 balls × 6 repeats = 18. That maths is invisible. I have to trust the number at the bottom, and I don't yet understand where 18 came from. There's no breakdown like "3 balls × 6 repeats = 18". The first time I see a big number here I'll probably wonder if it's right.

**"Session club"** vs "Default club" on the unit — these two concepts are related but the relationship is never explained. On the unit I set a "Default club: 7-Iron". On the session item I see "Session club: 7-Iron" pre-populated. Is it the same club? Is one overriding the other? What happens if I set the session club to a different iron — does the unit's default get ignored? A 25 handicap player will not know, and might set both to the same thing twice thinking they're required, or override one accidentally.

**"Focus cue"** at the bottom of the item — this is a fourth field I didn't expect. The unit already has a "Focus" field. Now the session item also has a "Focus cue" field. I can now see three layers of focus: unit's focus, session item's notes, session item's focus cue. That's a lot of optional free-text fields with similar semantic meaning crowded into one item.

**Hesitation moment ⑧**
The ball count summary "18 balls • Club: 7-Iron" appears at the bottom of the item card (Screenshot 8), above the up/down/delete row. It's small and low-contrast. A user scrolling through a long form won't notice this is the live running total for that item. The connection between changing "Repeat count" and seeing the ball number update isn't obvious on first encounter.

**Hesitation moment ⑨**
The "Balls — 18 balls" card at the very bottom of the screen (Screenshot 8) is the session total. But it looks visually identical to the "Summary" card on other screens. Its position — between the last item card and the Save button — makes it easy to miss or confuse with an item's own sub-total. There's no label differentiating "this item's balls" from "session total balls".

I fill everything in and scroll down to find "Save session".

**Hesitation moment ⑩**
"Save session" — after all the scrolling to configure items, I'm not certain I've seen all the options. The form is very long and I might not trust that scrolling to the bottom and tapping Save is correct without some kind of progress summary at the top. A sticky total bar showing "18 balls — 1 unit" while I work would give ongoing reassurance.

---

## Task 3 — Modify a session

### Session detail (Screenshot 6)

**What I notice first**
Session name "Test", two buttons "Edit session" and "Delete session". A Summary card. A "Session items" card listing the unit with its repeat count and club.

**What I think I can do**
Edit or delete the session.

**What is confusing**
"Edit session" and "Delete session" are styled as outlined pill buttons side by side. Delete session uses the app's accent colour (coral/red), making it visually prominent — arguably as prominent as Edit. For a destructive action this is risky. A user looking for the edit button might tap Delete first, especially at speed.

There's no **Duplicate** option visible on this screen. The spec says duplicate is supported, but I can only find Edit and Delete. Duplicate is presumably hidden in the three-dot menu on the Sessions list card — which I would never discover unless I explored the overflow menu. For a feature that's described as a key workflow (use a session as a template), it's buried.

**Hesitation moment ⑪**
I want to change the repeat count on one unit. My mental model says: tap Edit session, find the item, change the number, save. That works — but it means re-entering the entire long form. There's no way to quick-edit a single item field from the detail view without going through the full edit screen. For a simple change like "change repeats from 6 to 8", that's a lot of steps.

---

## Task 4 — Estimate practice ball count

The ball count is shown in multiple places:

- Unit list card: _"3 balls"_
- Unit detail Summary card: _"3 balls"_
- Session list card: _"18 balls"_
- Session detail Summary: _"18 balls"_
- Edit session bottom card: _"Balls — 18 balls"_

**What is confusing**
I can see the totals, but I never understand the _maths_ behind them. There's no breakdown of: unit ball count × repeats = item total, summed across items = session total. The number just appears. For a 25 handicap player who bought a 100-ball bucket and wants to know if this session fits, they need confidence in the number — not just the number itself.

**Hesitation moment ⑫**
In the Edit session form (Screenshot 8), the session total "Balls — 18 balls" is a static card below all the item cards. If I'm mid-form and I've just changed a repeat count from 6 to 8, does that number update in real time? I can't tell from the static screenshots, but even if it does, its position below the fold means I have to scroll down to verify after every change.

**What is missing**
A persistent, sticky ball count at the top or bottom of the edit session screen that updates live as I change repeat counts. This is the app's headline value proposition — knowing exactly what you're signing up for — and it's rendered as a small card at the bottom of a long scrollable form.

---

## Task 5 — Configure clubs

### Settings → Appearance + Units (Screenshot 9)

**What I notice first**
Account info at top, then Theme, then Dynamic color toggle, then Distance/Speed unit selectors.

**What is confusing**
"Dynamic color" — a 25 handicap golfer has no idea what this means. It's a Material You system concept that has nothing to do with golf. If they turn it on expecting something useful and the UI colours shift, they might think something went wrong.

The Distance unit toggle has "Yards" and "Meters" but I've selected "Meters" and the Speed still shows "mph" — a unit mismatch that an average user won't notice but that will silently produce confusing data if they ever use speed-related features.

---

### Settings → Clubs (Screenshots 10 & 11)

**What I notice first**
A long list of club names grouped by category (Woods, Hybrids…) each with a toggle.

**What I think I can do**
Toggle clubs on and off to control what appears in the dropdowns.

**What is confusing**

**No context at the top of the list.** The CLUBS heading just appears as I scroll Settings. There's no explanatory sentence like _"Enable the clubs in your bag — these are the only clubs that appear in unit and session dropdowns."_ A 25 handicap golfer seeing this for the first time might think they're entering their handicap bag declaration for a competition, or that toggling something off deletes data.

**The club names assume knowledge.** "3-Wood," "4-Wood," "5-Wood," "7-Wood," "9-Wood," "11-Wood" — most recreational golfers don't carry 7, 9, or 11 woods and have never heard of them. The list will feel overwhelming and they won't know what's relevant to their bag.

**Pitching Wedge (PW), Approach Wedge (AW), Gap Wedge (GW)** — the abbreviations in parentheses are helpful for experienced players but AW/GW overlap in naming conventions. Different manufacturers use "AW" and "GW" interchangeably. A 25 handicapper may not know the difference and could enable or disable the wrong club.

**No "Select all" or "Clear all"** — if I want to start from scratch and only enable my 14 clubs, I have to scroll through 30 items toggling individually. There's also no "start with a typical amateur bag" preset.

**Hesitation moment ⑬**
I don't know if toggling off a club that's _already assigned to a unit_ will break that unit or just remove the club from future dropdowns. There's no warning or explanation. A user might be afraid to change their setup after they've already built content.

**Hesitation moment ⑭**
The list ends with "Putter" under its own "Putter" sub-header (Screenshot 11) and then the ABOUT section starts. The transition from club toggles to app metadata is abrupt — no spacing or divider makes the two sections feel meaningfully separate.

---

## Summary of all hesitation/error moments

| #   | Screen           | Issue                                                                                         |
| --- | ---------------- | --------------------------------------------------------------------------------------------- |
| ①   | Overview         | User taps "New session" before creating any units                                             |
| ②   | Units list       | Unclear: tap card vs tap three-dot menu                                                       |
| ③   | Edit unit        | "Focus" field has no explanation                                                              |
| ④   | Edit unit        | "Add instruction" is below the fold; hard to find                                             |
| ⑤   | Edit unit        | Save button looks less prominent than Add instruction                                         |
| ⑥   | Sessions list    | Same tap ambiguity as units                                                                   |
| ⑦   | Edit session     | "Add item" reveals a unit picker, but term "item" is opaque; fails silently if no units exist |
| ⑧   | Edit session     | Ball sub-total is small and easy to miss; maths is invisible                                  |
| ⑨   | Edit session     | Session total looks identical to item sub-total; position is below fold                       |
| ⑩   | Edit session     | No persistent progress summary while building                                                 |
| ⑪   | Session detail   | Duplicate is hidden; Edit/Delete button parity gives Delete equal visual weight               |
| ⑫   | Edit session     | Ball count update requires scrolling to bottom to verify                                      |
| ⑬   | Settings / Clubs | No warning about toggling a club already in use                                               |
| ⑭   | Settings / Clubs | No "typical bag" preset; 30 individual toggles is exhausting                                  |

---

## Top five issues by severity

**1. No units → session creation dead end** — The app lets a user navigate to Sessions and tap "New session" with zero units in the library. When they try to pick a unit in the item dropdown, there's nothing there. There's no inline empty-state message explaining why, and no link to go create a unit. Task completion fails silently.

**2. Ball count total is buried** — The entire value of the app is "know your ball count before you leave the house." That number lives at the bottom of a long scrollable form with no sticky display. It should be impossible to miss.

**3. "Focus" has no explanation anywhere in the form** — A field with no placeholder, no helper text, no example. Recreational golfers will either skip it entirely or use it incorrectly. The feature is probably the most strategically valuable field in the app for improving practice quality.

**4. Destructive action (Delete) has equal visual weight to Edit** — On both the Unit detail and Session detail screens, Delete is styled as a coral/red outlined button at the same size as Edit. A user moving quickly will occasionally tap the wrong one, with no undo.

**5. Terminology inconsistency** — "Sessions" vs "Session templates", "items" vs "units", "Session club" vs "Default club" — the app uses different words for the same concepts in different screens. A 25 handicapper building a mental model of how the app works will accumulate small confusions that compound into a general sense that they might be doing something wrong.
