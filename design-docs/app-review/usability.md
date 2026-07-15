# Usability

Friction points and UX gaps, ordered by surface: the range-execution flow first (highest
stakes — used one-handed, outdoors, mid-practice), then planning, auth/settings, and the
website. Overlaps with [potential-bugs.md](potential-bugs.md) are cross-referenced rather than
repeated.

A general note first: accessibility hygiene in the execution UI is genuinely good — merged
semantics with human-readable counts on the counter (`ExecutionBlockPage.kt:317-323`), block
overview rows (`BlockOverviewContent.kt:125-139`), `clearAndSetSemantics` to hide redundant
denominators (`ObservationCaptureSection.kt:336`), a skip-to-content link and reduced-motion
handling on the site (`Layout.astro:43-48`). The findings below are edges, not a pattern of
neglect.

---

## Range execution flow

### U1 — Final observation chip auto-commits with a 300ms input lock and no undo window — HIGH

`RangeSessionViewModel.kt:284-297` (`scheduleAutoCommit`, `autoCommitDelayMillis = 300` at :87)

When the last enabled observation type is staged, the ball auto-commits after 300ms, and
during the arm window `stageObservation`, `commitBall`, and `decrementBlock` all early-return
(:227, :269, :301). A misclick on the final chip commits a wrong ball that can only be fixed
afterward via the edit sheet. Outdoors, one-handed, gloved, mid-routine — this is exactly
where a mistap happens. Consider a longer or cancellable arm window, or a brief "tap to undo"
affordance on the commit pulse.

### U2 — Double-tap on Start creates duplicate sessions — HIGH

See [potential-bugs.md](potential-bugs.md) B2. Usability face of the same defect: the user
ends up with a phantom in-progress session in the Active carousel and no idea how it got
there.

### U3 — Club-swap chip is below the 48dp touch-target minimum — MEDIUM

`ExecutionBlockPage.kt:553-583`

The in-block club chip is a `Row` of short-label text + a 16dp `SwapHoriz` icon with
`spacedBy(4.dp)` and no minimum size — yet it's a primary in-execution action. Contrast
`CountStepper`, which correctly enforces `Modifier.size(48.dp)` (`CountStepper.kt:66-68`), and
the 56/64dp counter buttons. Same outdoor/gloved context as U1.

### U4 — No back-press handling during an active session — LOW

`RangeworkApp.kt:383`

Android back = `popBackStack()` with no confirmation; an accidental back mid-block silently
drops the user out of the session they're running. Progress is persisted so nothing is lost,
and the finish-summary screen *does* guard back (`RangeSessionScreen.kt:162-164`) — a
`BackHandler` routing to the overview (or a light confirm) would make the execution screen
consistent with it.

### U5 — Bare-spinner loading state on the execution screen — LOW

`RangeSessionScreen.kt:522-526`

Only a centered `CircularProgressIndicator`, despite a `SkeletonList` component existing and
the empty/error states here being well done (`EntryHighlightCard`, :536-557). Consistency gap.

### U6 — Screen-on flag persists through the finish summary — LOW

`RangeSessionScreen.kt:121-127, 204`

`FLAG_KEEP_SCREEN_ON` is tied to a `DisposableEffect(Unit)` that stays composed during the
finish-summary early-return, so the screen stays awake on a static summary. Minor battery
edge.

---

## Planning and library

### U7 — No free-text search on unit/session lists — MEDIUM

Tag filtering exists (`TagFilterBar`, `toggleUnitTagFilter`/`toggleSessionTagFilter`) but
there's no search box. Past a few dozen units this becomes the main library friction — and the
MCP integration makes libraries grow faster than hand-authoring would. See
[high-value-features.md](high-value-features.md) F4.

---

## Auth, legal, settings

### U8 — Sign-in Terms & Privacy links are dead — MEDIUM (close before launch)

`RangeworkApp.kt:484`

`LegalLine` wires both links to a no-op: `LinkInteractionListener { /* URLs wired when
policies are published */ }`. Every new user sees "By continuing you agree to the Terms &
Privacy Policy" with links that do nothing — while `WebViewScreen` + `RangeworkRoutes.legalPage`
already exist and are wired from Settings. A consent affordance that silently fails is a
trust and store-review problem.

---

## Website (visitor-facing)

### U9 — Marketing copy describes the retired step-per-screen wizard — HIGH

- `HeroSection.astro:36-37` — "Execute step by step at the range"
- `FeaturesSection.astro:17-19` — "only the current instruction is shown. Tap to complete and
  auto-advance"
- `HowItWorksSection.astro:22-24` — "walks you through one step at a time … Resumes mid-step"

The shipped product is block-first with a ball counter, swipe navigation, and per-block
progress (`RANGEWORK.md:39-43`). This is a factual mismatch, and it violates
`CONTEXT-MAP.md`'s own rule that site copy "must match Planning & Execution exactly."

### U10 — Conversion funnel dead-ends — HIGH

Two compounding issues, both in [potential-bugs.md](potential-bugs.md): the Play Store CTA
goes to the store homepage (B12) and shared links have no working preview image (B11). The
site can't currently convert a visitor even in principle.

### U11 — No support/contact surface — MEDIUM

`AGENTS.md` describes the site as "the marketing and support website," but the only support
channel is a `mailto:` buried in `delete-account.astro:85-91`. No FAQ, no help page, no
Contact link in `Nav.astro` or `Footer.astro`.

### U12 — `/ai-planning` is not in primary navigation — LOW

`Nav.astro:32-51` links only `#how-it-works` and `#sample-session`; the AI-planning page — a
distinct value proposition — is reachable only via one link inside `AiPlansSection.astro:60`.

### U13 — No sitemap.xml or robots.txt — LOW

Nothing blocks crawling, but there's no sitemap helping engines discover `/ai-planning`,
`/delete-account`, and the legal pages.
