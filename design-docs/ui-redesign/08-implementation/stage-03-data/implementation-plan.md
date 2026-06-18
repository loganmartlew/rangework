# Stage 03 — Data enablers (shared / supabase)

> Roadmap stage **S3**. Isolates all non-UI/data work so screen stages consume stable contracts. Backlog: B29, B15, B44, B27, B26.

## Objective

Build the five capabilities the redesigned screens need from the `shared` module (and possibly `supabase`), keeping `commonMain` platform-agnostic per `CLAUDE.md` layering:

1. **Duplicate-unit use case** (B29) — mirror the existing duplicate-session flow so Units gain a Duplicate action.
2. **Estimated session duration** (B15) — a pure helper deriving an approximate time from total ball count (constant per-ball seconds); no persistence.
3. **Club enabled-count** (B44) — a read-only aggregate ("12 of 30 enabled") derived from existing `user_preferences`.
4. **Recently-used signal** (B27) — an ordering of the most recently opened/edited units and sessions for the Overview recents strip. The one genuinely data-bearing feature; design for graceful degradation.
5. **Contextual next-move inputs** (B26) — expose the state the Overview ViewModel needs (counts, has-units, has-sessions, last-edited entity) to branch the Next-move card.

## Dependencies

- **Upstream:** S1 (sequenced after for repo stability; no structural dependency).
- **Downstream:** S5 (duplicate-unit), S6 (duration), S8 (club-count), S9 (recents + next-move).
- **Parallel:** runs alongside S2.

## Affected screens

None in this stage (no UI). Enables: Units list (Duplicate), Session detail (duration), Settings (club count), Overview (recents + next-move).

## Likely files

- `shared/src/commonMain/.../model/*` — duration helper (pure function on the session/ball-count model); a recents DTO/ordering type if needed.
- `shared/src/commonMain/.../data/DataFoundation.kt` — wire up new use cases.
- `shared/src/commonMain/.../data/Supabase*Repository.kt` — duplicate-unit insert mapping; club-count read; recents read/write if persisted.
- New use-case files alongside the existing duplicate-session use case (find via the data foundation).
- `shared/src/commonTest/.../*` — unit tests for each capability.
- `androidApp/.../ui/PracticePlannerViewModel.kt` — surface counts / last-edited / club-count to the UI (read paths).
- **Only if recents needs persistence:** `supabase/migrations/<timestamped>.sql` (new migration, RLS preserved) + matching repository row-mapping update in the same change.

## New components required

No UI components. New domain/data artifacts:

- `DuplicateUnitUseCase` (mirrors `DuplicateSessionUseCase`).
- `estimateSessionDuration(...)` pure helper (per-ball time constant; returns minutes).
- `EnabledClubCount` aggregate / accessor over `user_preferences`.
- Recently-used ordering source: **decide local vs remote** (see F-6 / R4) — either a local last-accessed store or a `last_opened_at`/`updated_at`-driven query.
- Next-move state model (sealed type: `NoUnits` / `UnitsNoSessions` / `Both` / `ResumeEditing(entity)`) consumed by the Overview ViewModel.

## Validation checklist

- [ ] `.\gradlew.bat :shared:testDebugUnitTest :androidApp:testDebugUnitTest :androidApp:assembleDebug` green.
- [ ] `.\gradlew.bat :shared:lintDebug :androidApp:lintDebug` clean.
- [ ] `commonTest`: duplicate-unit produces an independent copy (new id, deep-copied instructions, no shared references).
- [ ] `commonTest`: `estimateSessionDuration` table test across representative ball counts (0, small, large).
- [ ] `commonTest`: club enabled-count matches the enabled set in `user_preferences`.
- [ ] `commonTest`: recents ordering returns most-recent-first and is bounded (3–5 items).
- [ ] Next-move state resolves to the correct branch for each of: zero units, units-no-sessions, both, just-edited.
- [ ] If a migration is added: RLS test confirms ownership scoping via `auth.uid()`; repository mapping round-trips the new column; `updated_at` trigger pattern preserved.
- [ ] Graceful degradation: with the recents source unavailable, the accessor returns empty (so S9 can hide the strip) rather than throwing.
- [ ] No breaking change to existing serialized models / wire format.

## Accessibility requirements

- None directly (no UI). Ensure derived strings handed to the UI are plain and TalkBack-friendly: duration as "~15 min" not a raw float; club count as "12 of 30 clubs enabled" (full words, not "12/30").

## Regression risks

- **R4 (primary):** recents (B27) may force a Supabase migration touching `shared` mappings, RLS, and serialization. Treat recents as the one feature that may slip; design the accessor to degrade gracefully so S9 still ships without it. Use a new timestamped migration; never rewrite existing migration history (`CLAUDE.md`).
- Duplicate-unit must respect ownership/RLS exactly like duplicate-session — a copy must be scoped to `auth.uid()`.
- Duration constant is a product assumption — keep it a single named constant so it's tunable without code archaeology; flag the chosen seconds-per-ball for product sign-off.
- Club-count read must not introduce an extra round-trip on every Settings render — derive from already-loaded preferences where possible.
- Changing `PracticePlannerViewModel` read surface risks the auth-gated flow — preserve the existing `AuthState` reactivity and misconfigured-degrade behaviour.
