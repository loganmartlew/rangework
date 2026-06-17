# Rangework Redesign — Master Implementation Roadmap

A single migration plan that turns the redesign program (10 screen redesigns + 11 audits) into an ordered, dependency-aware sequence of implementable stages. This document does **not** implement anything; it sequences the work so each stage is independently shippable, minimally coupled to its neighbours, and testable on its own.

**Sources analyzed:** `RANGEWORK.md`; `06-findings/*` (systemic audit, cognitive walkthrough, consistency audit, Material 3 audit, planning/readiness review, five per-screen reviews, prioritized 60-item backlog); `07-redesigns/*` (login, overview, unit list/detail/edit, session list/detail/edit, settings + new Manage clubs — markdown specs and wireframe PNGs); current-state screenshots `01–05`; `08-implementation/redesign-audit.md`; and the codebase map in `CLAUDE.md`.

Backlog item IDs (B01–B60) reference `06-findings/prioritized-roadmap.md`.

---

## 0. How to read this document

- **Section 1** — the full change inventory across the six required dimensions (UI, components, navigation, layout, accessibility, architecture).
- **Section A** — dependency graph.
- **Section B** — implementation stages (the unit of work).
- **Section C** — stage ordering + parallelization.
- **Section D** — validation checkpoints per stage.
- **Section E** — risks.
- **Section F** — open decisions / explicitly deferred work.

**Architectural ground rule (from `CLAUDE.md`):** UI/Compose/navigation/ViewModel work lives in `androidApp`; domain models, validation, use cases, repository contracts, and Supabase mappings live in `shared`. The redesign program is ~85% `androidApp` UI work. Only five items reach into `shared`/`supabase` (see Stage 3). Display-string and label changes must **not** rename serialized model fields or DB columns.

---

## 1. Change Inventory

### 1.1 Every required UI change (by screen)

| Screen | Changes (backlog IDs) |
| --- | --- |
| **Login** | Single-column layout, drop both cards (B22); remove nested "What you'll do here" sub-card (B24); remove "Signed out." label (B52); remove "Google sign-in" chip (B53); one headline only (B54); centre+enlarge app icon ~72–96dp (B55); Google Identity-compliant sign-in button (B23); add Terms/Privacy legal line. |
| **Overview** | Small TopAppBar title-only (B34); trim welcome for returning users, drop New unit/New session buttons (B28); tappable stat cards → navigate (B25, B51); contextual Next-move card (B26); Recently-used `LazyRow` (B27); dedicated first-run empty state (B02-analogue). |
| **Units list** | Small TopAppBar title-only (B34); FAB → `primaryContainer` (B07); 96dp bottom content padding (B37); three-tier card (B20); differentiate club from instruction text (B20); tappable card body + demoted overflow (B04, B49); Duplicate in overflow (B29); empty state (B02); Extended FAB on sparse lists (B57). |
| **Sessions list** | Same structural twin as Units list (B34, B07, B37, B04, B02, B57); raise ball-count prominence in card (B13); Duplicate as first-class overflow item (B30); dependency-aware empty state routing to Units (B19). |
| **Unit detail** | Edit/Delete → app-bar trailing icons (B35); Delete → overflow + confirm dialog + undo (B06); Small TopAppBar title-only (B34); remove "Instruction N" labels → number badges (B21); raise ball-count prominence / right-aligned pills (B13, B42); tinted Focus card (B16, B60); inline empty placeholders. |
| **Session detail** | Twin of Unit detail (B35, B06, B34, B13, B16, B60); briefing summary strip (B14); estimated duration (B15); club shown only when it overrides unit default; Duplicate in overflow (B30); inline empty placeholder. |
| **Unit edit/create** | Save → green `FilledButton` (B09); save snackbar (B41); remove "Instruction N" → badges (B21); helper text Notes vs Focus (B32); drag-to-reorder (B01); 48dp delete target (B38); restore field rest border (B36); ball-count stepper (B05); "Add instruction" → full-width TextButton+icon (B50); standardize field spacing (B48); drop duplicate H1 (B34); progressive disclosure of optional fields (B40); docked Save bar. |
| **Session edit/create** | Twin of Unit edit (B09, B41, B01, B38, B36, B05, B40, B21, B34); reorder item form so repeat-count + live subtotal lead (B03); sticky live ball total at top (B12); estimated duration in total (B15); dependency-aware Add-item routing (B19); "More options" expander per item. |
| **Settings** | Reorder sections Preferences→Clubs→Account→About (B17, B58); Small TopAppBar title-only (B34); Dynamic-color supporting text (B43); Sign out → `ListItem` w/ logout icon + error label (B47); Club bag → single row w/ "12 of 30 enabled" summary + chevron (B10, B44); section/group headers → M3 `ListSubheader` (B46); replace card-wrapped groups with stock list groups; speed-units caption or defer (B45). |
| **Manage clubs (NEW screen)** | New pushed destination (B10); category `ListSubheader`s (B46); per-club `ListItem` + `Switch`; switch content descriptions (B39); search/filter (B59); overflow presets "Enable common bag"/"Disable all" (B59); live count caption. |
| **Cross-cutting** | Terminology lock — one term per concept: Unit, Session, Session item, Focus cue, scope-prefixed notes (systemic 8.1, consistency M4/M6); nav-bar active pill + 80dp (B08). |

### 1.2 Every reusable component change

These are built once and consumed by multiple screens — they are the backbone of "minimally coupled" stages.

| Component | Consumers | Source items |
| --- | --- | --- |
| `RangeworkTopAppBar` (Small, title-only, optional back + trailing actions) | every screen except Login | B34, B35 |
| `RangeworkNavBar` (active pill, 80dp) | all authenticated screens | B08 |
| `ListEntryCard` (three-tier: title / metadata row / preview, clickable, trailing overflow) | Units list, Sessions list, Overview recents | B04, B20, B13, B49 |
| `EmptyState` (icon + headline + body + CTA, dependency-aware variant) | Units list, Sessions list, Overview first-run, detail inline placeholders | B02, B19 |
| `OverflowMenu` (Edit / Duplicate / Delete dropdown) | list cards, detail app bars | B04, B29, B30, B35 |
| `DeleteConfirmDialog` + `UndoSnackbar` host | both details, both editors (instruction/item delete) | B06, B18, B41 |
| `StatBlock` (numeral + caption) and `BriefingRow` | Unit detail, Session detail, Overview stat cards | B13, B14, B15, B25, B51 |
| `NumberBadge` (circular index) | Unit detail, both editors | B21 |
| `BallCountPill` / club `AssistChip` | Units list, both details | B13, B20 |
| `CountStepper` (− value +) | Unit edit (ball count), Session edit (repeat count) | B05 |
| `ReorderableItemRow` (drag handle + content) | Unit edit, Session edit | B01, B38 |
| `DockedSaveBar` (`FilledButton`, pinned) | Unit edit, Session edit | B09 |
| `StickyTotalBar` (live total under app bar) | Session edit (and reuse pattern in unit edit total) | B12, B15 |
| `MoreOptionsExpander` (progressive disclosure) | Unit edit (optional fields), Session edit (per-item) | B40 |
| `FocusCard` (`secondaryContainer` tint) | Unit detail, Session detail | B16, B60 |
| Settings `SettingsListItem` / `ListSubheader` / error `SignOutItem` | Settings, Manage clubs | B46, B47 |
| `GoogleSignInButton` (Identity-compliant) | Login | B23 |
| Restyled `OutlinedTextField` defaults (rest border, supporting text, 16dp spacing) | both editors, Settings | B36, B32, B48 |
| `RangeworkFab` / `ExtendedFab` (`primaryContainer`) | Units list, Sessions list | B07, B57 |

### 1.3 Every navigation change

- Detail screens: Edit/Delete **move out of content into the app-bar trailing area** (B35) — changes the action surface, not the route graph.
- **New route:** `Settings → Manage clubs` pushed destination (B10).
- Overview stat cards become navigation entry points → Units / Sessions tabs (B25).
- Overview Recently-used cards → unit/session detail (B27).
- Overview contextual Next-move button → create-unit / create-session / open-most-recent depending on state (B26).
- Empty-state CTAs route forward; **dependency-aware routing**: an empty Sessions / Add-item flow with zero units routes the user to create a unit first (B19).
- Club dropdowns gain a "Manage clubs" deep-link affordance (systemic 6.2 — optional, ties to B10).
- **Decision-gated:** remove the standalone Session-detail waypoint screen (B11) — conflicts with the Session-detail redesign; see Section F.
- **Decision-gated:** tablet list-detail pane layout for Units/Sessions (systemic 5.3) — not specified in any redesign; see Section F.

### 1.4 Every layout change

- Login: two stacked cards → one full-height centred `Column` with weighted spacers.
- Overview: card-heavy dashboard → greeting strip + stat row + Next-move + recents `LazyRow`; separate first-run state.
- Lists: run-on card → three-tier card; 96dp bottom padding so FAB can't occlude last card (B37).
- Details: two co-equal pill buttons consuming the top band → app-bar actions + `BriefingRow`/`SummaryRow` stat strip + structured item rows.
- Editors: bottom-buried Save and total → sticky total bar at top + docked Save bar at bottom; nested card-within-card removed; reordered field order (B03); standardized 16/12/20dp spacing grid (B48).
- Settings: 30 inline toggles → single Club-bag row; card-wrapped groups → stock M3 list groups; collapses a 3-scroll screen to one page.
- Surface hierarchy: flat custom dark surfaces → M3 tonal-elevation tokens (`surfaceContainer*`) so cards read as cards (Material audit; B33, B60).

### 1.5 Every accessibility change

- Contrast/AA audit on field borders, labels, helper text, secondary card text — outdoor legibility is a first-class requirement (systemic 9.1; B36). Target 4.5:1 text / 3:1 components.
- Content descriptions on all icon-only controls: reorder, delete, overflow, club switches (systemic 9.2; B39).
- 48dp minimum touch targets on instruction/item icon buttons and card overflow (B38; consistency M5).
- Destructive meaning never colour-only — pair error colour with icon + label + confirmation (systemic 9.3; B06).
- Verify segmented-control selected/unselected contrast delta (systemic 9.4).
- Verify club rows use ≥48–56dp `ListItem` height and `toggleable` row semantics (Material audit).

### 1.6 Every architectural dependency (`shared` / `supabase` / data)

| Capability | Layer | Item | Notes |
| --- | --- | --- | --- |
| Duplicate **unit** use case | `shared` (mirrors existing duplicate-session) | B29 | New use case + repository call; unit test in `commonTest`. |
| Estimated session **duration** helper | `shared` model/util (pure derivation from ball count) | B15 | Constant per-ball time; no persistence; pure-function test. |
| Club **enabled-count** ("12 of 30") | derivable from existing `user_preferences` | B44 | Read-only aggregate; likely no schema change. |
| **Recently-used** tracking | `shared` + possibly `supabase` migration **or** local store | B27 | Needs last-opened/last-edited ordering. Highest-risk data item — may add a timestamp/local persistence. Can degrade gracefully (hide strip) if unavailable. |
| Contextual **Next-move** state | `androidApp` ViewModel reading existing counts + last-edited | B26 | Mostly UI logic; depends on recents/last-edited signal. |
| **"Used in N sessions"** reverse link | `shared` reverse query | systemic 4.2 / delight | **Not in any redesign spec** — deferred (Section F). |

No redesign requires a breaking model/column rename. Terminology lock changes **display strings only**.

---

## A. Dependency Graph

```
                         ┌─────────────────────────────┐
                         │ S1  FOUNDATION              │
                         │ M3 tokens · nav pill/80dp   │
                         │ FAB colour · field borders  │
                         │ surface elevation · AA      │
                         │ terminology/string lock     │
                         └───────────────┬─────────────┘
                                         │ (everything below depends on S1)
                ┌────────────────────────┼─────────────────────────┐
                ▼                        ▼                          ▼
   ┌────────────────────────┐  ┌──────────────────────┐   ┌─────────────────────┐
   │ S2  COMPONENT LIBRARY  │  │ S3  DATA ENABLERS    │   │ S4  LOGIN           │
   │ cards · empty state    │  │ (shared/supabase)    │   │ (isolated, no data) │
   │ stat block · stepper   │  │ dup-unit · duration  │   │ needs Google btn↘   │
   │ drag row · overflow    │  │ club-count · recents │   └─────────────────────┘
   │ delete-confirm/snack   │  │ next-move state      │
   │ docked save · sticky   │  └─────────┬────────────┘
   │ more-options · list-   │            │
   │ item · Google button   │            │
   └───────────┬────────────┘            │
               │                         │
   ┌───────────┼─────────────────────────┼──────────────────────────┐
   ▼           ▼                         ▼                           ▼
┌────────────────┐  ┌────────────────┐  ┌────────────────┐  ┌────────────────────┐
│ S5  LISTS      │  │ S6  DETAILS    │  │ S7  EDITORS    │  │ S8  SETTINGS +     │
│ units+sessions │  │ unit+session   │  │ unit+session   │  │     MANAGE CLUBS   │
│ needs S2,      │  │ needs S2,      │  │ needs S2,      │  │ needs S2 (listitem)│
│ dup-unit(S3)   │  │ duration(S3)   │  │ dep-routing    │  │ club-count(S3)     │
└───────┬────────┘  └───────┬────────┘  └────────────────┘  └────────────────────┘
        │                   │
        └─────────┬─────────┘
                  ▼
        ┌──────────────────────────┐
        │ S9  OVERVIEW (integrator)│
        │ stat nav → S5            │
        │ recents → S6, recents(S3)│
        │ next-move(S3) · 1st-run  │
        └────────────┬─────────────┘
                     ▼
        ┌──────────────────────────┐        ┌───────────────────────────────┐
        │ S10 A11Y HARDENING SWEEP │        │ S11 APPROVED DEFERRED WORK    │
        │ (cross-cuts S4–S9)       │        │ drag · used-in · swipe · etc. │
        └────────────┬─────────────┘        └───────────────┬───────────────┘
                     └──────────────────────┬───────────────┘
                                            ▼
                              ┌──────────────────────────────┐
                              │ S12 LIST FIDELITY             │
                              │ FAB state · top bar · cards   │
                              └──────────────┬───────────────┘
                                             ▼
                    ┌────────────────────────┴────────────────────────┐
                    ▼                                                 ▼
       ┌──────────────────────────┐                     ┌──────────────────────────┐
       │ S13 DETAIL FIDELITY      │                     │ S14 EDITOR CONSISTENCY   │
       │ stats · focus cues       │                     │ shared row · terminology │
       └────────────┬─────────────┘                     └────────────┬─────────────┘
                    └────────────────────────┬───────────────────────┘
                                             ▼
                               ┌──────────────────────────┐
                               │ S15 OVERVIEW POLISH      │
                               │ recents · contrast · bars│
                               └──────────────────────────┘
```

**Edge summary (X depends on Y):**
- S2, S3, S4 → S1
- S5 → S1, S2, S3(dup-unit)
- S6 → S1, S2, S3(duration)
- S7 → S1, S2 ; soft → S3(dependency-aware routing uses existing units only)
- S8 → S1, S2(list items), S3(club-count)
- S9 → S1, S2, S3(recents, next-move), and destinations S5/S6
- S10 → all screen stages (verification + gap-fill)
- S11 → approved deferred work from `stage-11-deferred/deferred-changes.md`
- S12 → S10, S11 (needs final list state after swipe/FAB/a11y work)
- S13 → S11 (detail top-bar/session-note changes should land first)
- S14 → S11 (drag-to-reorder should land before consolidating editor rows)
- S15 → S12, S13, S14 (final pass after card, detail/top-bar, and terminology decisions)

S3 and S4 can begin in parallel with S2 the moment S1 lands. S4 (Login) only needs the Google button from S2; it can ship as soon as that single component exists.

---

## B. Implementation Stages

Each stage lists: **scope · depends-on · backlog IDs · why it's a coherent unit · how it's tested.**

### S1 — Foundation: M3 tokens, chrome, terminology
**Scope:** Apply M3 tonal-elevation surface tokens (`surfaceContainer*`) replacing flat custom dark surfaces; FAB container → `primaryContainer` (B07); nav bar active-indicator pill + 80dp height (B08); restore `OutlinedTextField` rest border + standardize 16dp field spacing defaults (B36, B48); confirm primary/error/secondary colour tokens for buttons; segmented-control contrast (9.4); contrast/AA audit baseline (9.1). Plus the **terminology & string lock** (Unit / Session / Session item / Focus cue / scope-prefixed notes) as display-string changes only — no serialized field renames.
**Depends on:** nothing.
**Why one unit:** all cross-cutting, theme-level, touch `ui/theme/*` + global wrappers; doing them together avoids re-touching every screen later.
**Backlog:** B07, B08, B36, B48, B43-token, systemic 7.x/8.1/9.1/9.4, Material surface-hierarchy.
**Tested by:** visual regression on all existing screens (nothing should break); contrast meter on fields/secondary text; nav-bar renders pill at 80dp; existing `androidApp` ViewModel tests still green.

### S2 — Shared UI component library
**Scope:** Build every reusable composable in §1.2 as standalone, previewable components: `RangeworkTopAppBar`, `ListEntryCard`, `EmptyState`, `OverflowMenu`, `DeleteConfirmDialog` + `UndoSnackbar` host, `StatBlock`/`BriefingRow`, `NumberBadge`, `BallCountPill`/club chip, `CountStepper`, `ReorderableItemRow` (drag handle), `DockedSaveBar`, `StickyTotalBar`, `MoreOptionsExpander`, `FocusCard`, settings `ListItem`/`ListSubheader`/`SignOutItem`, `GoogleSignInButton`, `RangeworkFab`/`ExtendedFab`.
**Depends on:** S1.
**Why one unit:** these are consumed by ≥2 screens each; building once is the core of keeping screen stages decoupled. No screen wiring here.
**Backlog:** B04, B20, B13, B02, B21, B05, B01, B38, B09, B12, B40, B16, B60, B46, B47, B23, B57, B06, B18, B41.
**Tested by:** Compose `@Preview`s + screenshot tests per component; unit tests for `CountStepper` bounds and `ReorderableItemRow` reorder callback; drag-reorder library spike validated against the Compose/toolchain versions before adoption (risk R3).

### S3 — Data enablers (`shared` / `supabase`)
**Scope:** Duplicate-unit use case (B29); estimated-duration pure helper (B15); club enabled-count aggregate (B44); recently-used ordering signal (B27); contextual next-move state inputs (B26).
**Depends on:** S1 (none structurally; sequenced after S1 for repo stability). Can run **parallel to S2**.
**Why one unit:** isolates all non-UI/data work so screen stages consume stable contracts; keeps `shared` changes reviewable together per `CLAUDE.md`.
**Backlog:** B29, B15, B44, B27, B26.
**Tested by:** `shared/commonTest` use-case/helper unit tests; duplicate-unit round-trip; duration pure-function table; recents ordering test. If recents needs a migration, add Supabase migration + RLS test + repository-mapping update in the same change.

### S4 — Login redesign
**Scope:** Single-column centred layout; remove both cards/sub-card/labels/chip (B22, B24, B52, B53, B54); centre+resize icon (B55); Google Identity-compliant button (B23); Terms/Privacy line.
**Depends on:** S1, S2 (`GoogleSignInButton`). No data deps.
**Why one unit / early:** fully isolated pre-auth screen; fastest end-to-end proof that foundation + a real component work. Validates Credential-Manager/auth path early (risk R5).
**Backlog:** B22, B23, B24, B52, B53, B54, B55.
**Tested by:** sign-in still authenticates (manual + `AuthViewModel` tests); button matches Google branding spec; no-scroll CTA on common device heights; misconfigured-auth still degrades to friendly messaging (`CLAUDE.md` constraint).

### S5 — Units list + Sessions list redesign
**Scope:** Three-tier `ListEntryCard`, tappable body, demoted overflow, club/ball differentiation, empty states, FAB recolour already in S1, Extended FAB, 96dp padding, Duplicate in overflow (unit via S3 use case; session existing), dependency-aware Sessions empty state (B19).
**Depends on:** S1, S2, S3(dup-unit).
**Why one unit:** the two screens are structural twins — same card/empty/FAB components; building together guarantees they "scan identically."
**Backlog:** B04, B07, B13, B19, B20, B29, B30, B34, B37, B49, B57, B02.
**Tested by:** UI tests for card tap → detail, overflow actions, empty→CTA routing, dependency-aware route; FAB no longer occludes last card; existing list ViewModel tests extended.

### S6 — Unit detail + Session detail redesign
**Scope:** Edit/Delete → app-bar (B35); Delete → overflow + confirm + undo (B06); number badges (B21); ball-count prominence/pills (B13, B42); Focus card (B16, B60); Session `BriefingRow` with duration (B14, B15); club-override-only display; Duplicate in overflow (B30); inline empty placeholders.
**Depends on:** S1, S2, S3(duration). **Decision gate F-1** (B11) should be resolved before/at this stage.
**Why one unit:** twins sharing app-bar action pattern, stat strip, item rows, delete-confirm.
**Backlog:** B06, B13, B14, B15, B16, B21, B30, B34, B35, B42, B60.
**Tested by:** app-bar Edit→editor, Delete→dialog→undo; Focus card renders only when cue exists; duration math matches helper; override badge shows only on deviation; ViewModel delete/undo tests.

### S7 — Unit edit + Session edit redesign
**Scope:** Green docked `FilledButton` Save + snackbar (B09, B41); drag-to-reorder + 48dp delete (B01, B38); steppers (B05); restored borders + helper text (B36, B32); "Add instruction/item" full-width TextButton (B50); number badges (B21); progressive disclosure (B40); Session: sticky live total (B12), reordered item fields (B03), duration in total (B15), dependency-aware Add-item (B19).
**Depends on:** S1, S2. Soft dep S3 (dependency-aware routing uses existing units).
**Why one unit:** highest interaction debt; both editors share stepper, drag row, docked save, more-options, snackbar. Most complex layout (nested scroll + IME + docked bar + sticky bar).
**Backlog:** B01, B03, B05, B09, B12, B15, B19, B21, B32, B34, B36, B38, B40, B41, B48, B50.
**Tested by:** reorder persists order; stepper min/max + invalid-input prevention; live total updates on every edit; Save persists + snackbar; back-press unsaved-changes guard; IME doesn't hide docked Save; ViewModel save/validation tests extended.

### S8 — Settings redesign + new Manage clubs screen
**Scope:** Section reorder + Preferences grouping (B17, B58); Dynamic-color caption (B43); Sign-out list item (B47); Club-bag row w/ count (B10, B44); `ListSubheader`s (B46); speed caption/defer (B45); new **Manage clubs** pushed screen with subheaders, per-club switches + content descriptions (B39), search (B59), presets (B59).
**Depends on:** S1, S2(list items), S3(club-count). Adds one route.
**Why one unit:** the club-bag extraction and the new screen are inseparable; the rest of Settings is the same list-item refactor.
**Backlog:** B10, B17, B39, B43, B44, B45, B46, B47, B58, B59.
**Tested by:** club toggles persist via existing prefs path; count summary matches; search filters; presets bulk-update; TalkBack reads "Enable Driver"; theme/unit selectors unchanged behaviourally; back nav from Manage clubs.

### S9 — Overview redesign (integrator)
**Scope:** Title-only app bar (B34); trimmed greeting, drop create buttons (B28); tappable stat cards → navigate (B25, B51); contextual Next-move (B26); Recently-used `LazyRow` (B27); first-run empty state.
**Depends on:** S1, S2, S3(recents, next-move), and live destinations S5/S6.
**Why last screen:** it integrates everything — reuses stat blocks and recent cards, navigates into the redesigned lists/details, and is the most data-coupled (recents + next-move). Doing it last means it consumes finalized components and routes.
**Backlog:** B25, B26, B27, B28, B34, B51.
**Tested by:** stat tap routes to tab; recent tap routes to detail; next-move branches across all states (no units / units-no-sessions / both / just-edited); first-run shows single CTA; recents degrade gracefully if S3 signal unavailable.

### S10 — Accessibility hardening sweep
**Scope:** Cross-cutting verification + gap-fill after screens land: full content-description audit, 48dp target audit, contrast re-measure under outdoor/bright conditions, colour-not-alone for all destructive paths, segmented-control deltas.
**Depends on:** S4–S9.
**Why separate:** much a11y is baked into S2 components, but a final sweep catches integration gaps and is the right place to run TalkBack end-to-end.
**Backlog:** B38, B39, systemic 9.1–9.4.
**Tested by:** TalkBack full-app pass; automated contrast/target lint; manual sunlight legibility check.

### S11 — Approved deferred work
**Scope:** Implements the product-owner-approved subset of `stage-11-deferred/deferred-changes.md`: drag-to-reorder gestures, "Used in N sessions" reverse link/delete warning, session item notes on detail, swipe-to-edit/delete on list cards, collapsing top app bar on detail, decorative login mark a11y fix, and `ClickableText` migration.
**Depends on:** S1-S10.
**Why one unit:** closes explicitly approved deferred work before the delivery-audit fidelity pass begins.
**Backlog:** B01, B31, B56, F-3, plus deferred a11y/technical-debt items.
**Tested by:** section-level validation in `stage-11-deferred/implementation-plan.md`; full regression across editor drag, list swipe, detail top bars, delete warning, and login a11y.

### S12 — List shell and card fidelity
**Scope:** Fix empty-list/sparse-list FAB rules; hide FAB when empty, show Extended FAB for 1-2 items, compact FAB for 3+; normalize list top bars to left-aligned M3 small bars; restore list cards as clickable `OutlinedCard`s with `titleMedium` titles and a single metadata row combining club/count information.
**Depends on:** S10, S11.
**Why one unit:** all findings affect the Units/Sessions list entry surface and shared list card/FAB behavior; keeping both lists together preserves twin parity.
**Audit findings:** redesign-audit #1, #7 (list portion), #8.
**Tested by:** empty/sparse/populated list states on both Units and Sessions; card tap/overflow/swipe; top-bar alignment; phone and tablet.

### S13 — Detail briefing and focus fidelity
**Scope:** Add stat prominence so ball count is visually dominant; restore the default-club block to Unit detail summary; label/tint session-item focus cues; add the missing icon to `FocusCard`; verify any remaining detail top-bar truncation/alignment leftovers.
**Depends on:** S11; can run after or alongside S12 if shared card work is stable.
**Why one unit:** all work sharpens detail-screen scan hierarchy and focus-cue comprehension.
**Audit findings:** redesign-audit #2, #3, #6, #7 (detail remainder if needed), #9.
**Tested by:** Unit/Session detail visual scan, long-title truncation, focus cue plus notes rendering, large font scale, phone and tablet.

### S14 — Editor consistency and terminology repair
**Scope:** Refactor Session editor rows to reuse `ReorderableItemRow`; restore scope-prefixed notes labels across editors/details; remove unused local editor `SnackbarHostState` wiring unless a real producer exists.
**Depends on:** S11 drag-to-reorder.
**Why one unit:** it fixes the editor twin-drift risk and the copy lock regression without mixing in new product behavior.
**Audit findings:** redesign-audit #4, #5, #12.
**Tested by:** editor drag and chevron reordering, delete controls, subtotal/live total updates, save snackbar still from shell, terminology grep.

### S15 — Overview fidelity and final polish
**Scope:** Remove the remaining low-contrast alpha from the Overview Next-move eyebrow; restore recents metadata/type chips; include entity names in "Resume editing" copy; finish top-bar normalization for Overview, Settings, Manage clubs, and editors if any centered bars remain.
**Depends on:** S12, S13, S14.
**Why one unit:** Overview consumes the final card, terminology, and top-bar decisions; it is the right final pass for integration polish.
**Audit findings:** redesign-audit #7 (overview/settings/editor remainder), #10, #11.
**Tested by:** Overview next-move branches, recents metadata for units/sessions, contrast in light/dark, all remaining non-detail top bars, phone and tablet.

---

## C. Stage Ordering

**Critical path:** S1 → S2 → {S5, S6, S7, S8} → S9 → S10 → S11 → S12 → {S13, S14} → S15.

**Recommended sequence (with rationale):**

1. **S1 Foundation** — unblocks everything; lowest-risk-first only after this, because tokens change app-wide.
2. **S2 Components** *and* **S3 Data enablers** in **parallel** — independent of each other; both only need S1.
3. **S4 Login** — as soon as `GoogleSignInButton` exists in S2. Isolated, no data deps, fastest end-to-end validation of foundation + auth. Confidence builder.
4. **S5 Lists** — high user-felt value (empty states, tappable cards, FAB), twins built once. Needs dup-unit from S3.
5. **S6 Details** — depends on duration (S3) and reuses the app-bar action pattern; resolve F-1 (B11) at this gate.
6. **S7 Editors** — heaviest, but isolated to two screens; sequence after Details so the app-bar/overflow/delete-confirm patterns are settled and reused.
7. **S8 Settings + Manage clubs** — independent of S5–S7; can slot in parallel with S6/S7 if a second implementer is available (only shares S2 list items + S3 club-count).
8. **S9 Overview** — last screen; integrates recents/next-move and navigates into finished S5/S6.
9. **S10 A11y sweep** — after all screens.
10. **S11 Approved deferred work** — close the items already approved out of the deferred backlog before measuring final delivery fidelity.
11. **S12 List fidelity** — fix the highest-impact delivery-audit issue first: duplicate empty-state affordances and missing sparse-list Extended FAB behavior.
12. **S13 Detail fidelity** — restore ball-count hierarchy, summary parity, and focus cue treatment.
13. **S14 Editor consistency** — consolidate the twin editor row structure and repair terminology drift.
14. **S15 Overview polish** — finish the integrator screen and any remaining small-top-bar normalization once list/card/terminology decisions are stable.

**Parallelization opportunities:** S2∥S3; once S2 lands, S5/S6/S7 and S8 are mutually independent and can be split across implementers (each touches a disjoint screen set; shared surface is the S2 library, which is frozen). S9 must wait for S5+S6. After S12, S13 and S14 can run in parallel because they touch details vs editors; S15 should wait for both so Overview consumes the final card, top-bar, and terminology patterns.

**Why low-ROI-formula items aren't all front-loaded:** the backlog's top formula rows (B09, B07, B37, token swaps) are absorbed into S1/S2 as foundation rather than shipped as isolated one-liners, because they're cheaper to do once at the component/theme layer than to retrofit per screen. The genuinely high-user-value items the reviews converge on (B01 drag-reorder, B02 empty states, B06 delete safety, B10 club extraction) land in their natural screen stages on the critical path.

---

## D. Validation Checkpoints

**Per-stage gate (every stage must pass before the next consumes it):**
- `.\gradlew.bat :shared:testDebugUnitTest :androidApp:testDebugUnitTest :androidApp:assembleDebug` green (the `CLAUDE.md` primary validation command).
- `.\gradlew.bat :shared:lintDebug :androidApp:lintDebug` clean (tests first, then lint).
- Extend the nearest existing test rather than adding broad new suites (`CLAUDE.md` convention).

| Stage | Specific checkpoints |
| --- | --- |
| **S1** | Visual regression: every existing screen still renders coherently; nav pill at 80dp; field borders visible at rest; contrast meter ≥ AA on fields + secondary text; auth-misconfigured path still shows friendly setup messaging. |
| **S2** | Each component has a `@Preview` + screenshot baseline; `CountStepper`/`ReorderableItemRow` unit tests; drag-reorder library compatibility spike documented. |
| **S3** | `commonTest` for dup-unit, duration, club-count, recents ordering; if a migration is added — RLS test + repository row-mapping updated in the same change; graceful-degradation path for recents verified. |
| **S4** | Real Google sign-in completes; button matches branding spec; session-restore still works; no horizontal scroll / CTA above fold on baseline devices. |
| **S5** | Card tap → detail; overflow Edit/Duplicate/Delete; empty-state CTA + dependency-aware route; FAB clears last card; both lists scan identically. |
| **S6** | App-bar Edit/Delete; delete confirm + undo restores; Focus card conditional; duration matches helper; override badge only on deviation. |
| **S7** | Reorder persists; stepper bounds + invalid-input blocked; live total/sticky bar updates every edit; Save persists + snackbar; unsaved-changes back guard; IME vs docked Save bar; tablet rail layout intact. |
| **S8** | Toggles persist; count summary correct; search + presets; TalkBack labels; Manage-clubs back nav; theme/units unchanged. |
| **S9** | Stat nav; recents nav; next-move all four states; first-run single CTA; recents degrade gracefully. |
| **S10** | Full TalkBack pass; 48dp + contrast lint; outdoor legibility manual check. |
| **S11** | Drag works with chevron fallback; used-in sessions card/delete warning; session item notes; list swipe actions; detail collapsing app bar; login mark a11y; `ClickableText` warning gone. |
| **S12** | Empty lists show one CTA and no FAB; 1-2 item lists show Extended FAB; 3+ lists show compact FAB; list top bars left-align; list cards are outlined with one metadata row; tap/overflow/swipe intact. |
| **S13** | Ball totals dominate detail summary strips; Unit detail summary includes default club; session item focus cues are labelled/tinted; `FocusCard` has icon; long detail titles truncate. |
| **S14** | Session editor uses shared reorder row; drag/chevrons/delete/subtotals still work; notes labels are scope-prefixed everywhere; unused editor snackbar hosts removed. |
| **S15** | Overview eyebrow contrast passes; recents include type + metadata; "Resume editing" includes entity name; all remaining non-detail top bars use left-aligned small bars. |

**Program-level checkpoints:**
- Phone (compact) **and** tablet (expanded rail + two-column overview) verified each screen stage — `CLAUDE.md` requires preserving the responsive nav pattern.
- Terminology audit after S1, re-checked after S9, and repaired/verified again in S14 (no concept uses two words anywhere).
- Auth-gated flow intact end-to-end after S4 and after S9.
- Delivery-audit coverage check after S15: every finding in `08-implementation/redesign-audit.md` is either implemented in S12-S15 or explicitly covered by S11.

---

## E. Risks

| # | Risk | Likelihood | Impact | Mitigation |
| --- | --- | --- | --- | --- |
| **R1** | S1 app-wide token swap causes visual regressions on screens not yet redesigned. | Med | Med | Land S1 behind thorough visual regression on all current screens; change tokens, not per-screen overrides; keep palette/green accent stable. |
| **R2** | Sticky total bar + docked Save + nested `LazyColumn` + IME interactions misbehave (overlap, scroll jank, hidden Save). | Med | Med | Prototype the S7 scaffold early (during S2); test with keyboard open on small screens and tablet. |
| **R3** | Drag-to-reorder (B01) needs a library or custom gesture that conflicts with the pinned Compose/toolchain versions (Java 17 target, SDK 35). | Med | Med | Compatibility spike in S2 before committing; keep ↑/↓ chevrons as an accessibility fallback per Material audit. Do **not** bump toolchain casually (`CLAUDE.md`). |
| **R4** | Recently-used (B27) forces a Supabase migration / new persistence — touches `shared` mappings, RLS, and model serialization. | Med | High | Treat recents as the one genuinely data-bearing feature; design it to degrade gracefully (hide strip) so S9 ships even if recents slips. New timestamped migration + RLS intact (`CLAUDE.md`). |
| **R5** | Google Identity-compliant button (B23) requires Credential Manager / `GetSignInWithGoogleOption` rewiring and can break sign-in. | Med | High | Do S4 early as an isolated end-to-end test; keep `AndroidAppAuthConfig`/`BuildConfig` wiring, no hardcoded secrets; verify misconfigured-degrade path. |
| **R6** | B11 (remove session-detail waypoint) conflicts with the S6 Session-detail redesign — building both wastes work. | High (if undecided) | Med | **Decision gate F-1 before S6.** If removing, migrate briefing strip + item rows into Edit/expandable card instead of S6 detail. |
| **R7** | Terminology/string lock accidentally renames serialized model fields or DB columns. | Low | High | Constrain S1 to user-facing display strings only; never touch `kotlinx.serialization` keys or `supabase` columns. |
| **R8** | Twin screens drift (Units vs Sessions, both details, both editors) if implemented separately. | Med | Low | Enforce the S2 shared components as the single source; review twins together in one stage. |
| **R9** | Progressive disclosure / "More options" expander (B40) hides fields users expect, causing confusion or lost edits. | Low | Med | Only collapse genuinely-optional fields; auto-expand when a hidden field has a value; usability check in S7. |
| **R10** | Tablet/responsive layout (compact ↔ expanded) regresses as screens are restructured. | Med | Med | Validate both width classes at every screen stage; the redesigns are phone-first and must not break the rail/two-column pattern. |
| **R11** | Scope creep from delight items (briefing reframe, run-mode, tablet panes) pulled into screen stages. | Med | Med | Keep S11 and S12-S15 bounded to their plan docs; larger product features need their own stage. |
| **R12** | List FAB rules regress because the decision is made in the global shell instead of each list screen. | Med | Med | In S12, derive FAB state from the active route and that route's actual item count; test empty, 1, 2, and 3+ items for both lists. |
| **R13** | Ball-count prominence crowds detail summary rows at large font scale. | Med | Low | Give `StatBlock` a prominence variant and test narrow/large-font layouts; wrap or rebalance row weights instead of shrinking the primary metric away. |
| **R14** | Refactoring Session editor onto `ReorderableItemRow` drops session-specific subtotal/repeat/club behavior. | Med | Med | Inventory row behavior before S14 and verify every callback/state after the refactor. |
| **R15** | Top-bar normalization conflicts with S11 collapsing detail app bars. | Med | Med | Encode route policy explicitly: detail routes use collapsing bars; all authenticated non-detail routes use pinned small bars. |

---

## F. Open Decisions & Deferred Work

These must be resolved by the product owner or handled by the already-approved deferred work. They are **not** part of the original S1-S10 redesign stages.

- **F-1 — Session-detail waypoint (B11 vs Session-detail redesign).** The redesign spec improves the detail screen *as it stands*; the backlog separately proposes deleting it as a navigational dead-end. These conflict. **Decide before S6.** If removed: relocate the briefing strip + structured item rows into the Edit read-state or an expandable list card (per the redesign author's own caveat).
- **F-2 — Tablet list-detail pane.** Systemic 5.3 flags the half-adapted tablet layout; no redesign specifies a canonical list-detail pane. Decide whether to commit to it (separate design needed) or keep the current rail + two-column overview only.
- **F-3 — "Used in N sessions" reverse link** (systemic 4.2 / delight). Resolved by the approved S11 implementation plan as an in-memory derivation plus Unit detail affordance/delete warning.
- **F-4 — Run / "follow at the range" mode** (systemic 3.5, planning review). The app stops at planning; the stated promise is "follow on the day." Leave an IA seam now; build later.
- **F-5 — Speed units** (B45). Decide: add a clarifying caption ("used when importing launch-monitor data") or defer the control until a feature consumes it.
- **F-6 — Recents persistence model** (R4). Local store vs Supabase timestamp migration — pick before S3 finalizes the recents contract.

---

## Summary

The original redesign program is sequenced as **two foundation stages (tokens, then a shared component library) + one data-enabler stage**, feeding **five screen stages** (Login → Lists → Details → Editors → Settings), **Overview as the integrator**, and an **accessibility sweep**. S11 then closes the approved deferred backlog items. The updated post-delivery audit continues with **S12-S15**: list fidelity, detail fidelity, editor consistency, and Overview polish. Together those stages cover every remaining finding in `redesign-audit.md` while keeping work grouped by user-facing surface and preserving the twin-screen strategy that keeps Units/Sessions and both editors from drifting.
