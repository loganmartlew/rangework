# MCP & Data Clutter — Design Decisions

**Date:** 2026-07-13
**Method:** grilling session (question-by-question interview) with inline domain modeling
**Status:** agreed 2026-07-13 — awaiting owner sign-off on the epic plan (`epic-implementation-plan.md`)
**Vocabulary:** the terms below — **Archived**, **Inline Unit**, **Promotion** — are defined
canonically in [`apps/mobile/CONTEXT.md`](../../apps/mobile/CONTEXT.md) (added during this
session). This doc records the decisions and their reasoning; the glossary records the language.

## 1. Problem

Practice Sessions accumulate. The dominant flow is: plan a session in an AI conversation via
the MCP server, run it once, never touch it again — so the session list fills with one-offs
that were never meant to be templates, and the unit library fills with drills the AI minted
just to serve them. Hard delete exists but is the wrong tool three ways:

1. **Re-runnability** — a deleted session can never be reopened, inspected, or re-run.
2. **Reversibility** — delete is permanent, so neither the user nor the AI can tidy casually.
3. **History grouping** — `range_sessions.practice_session_id` is `on delete set null`
   (`supabase/migrations/20260618120000_range_sessions.sql`), so deleting a template silently
   degrades per-template Range Session history. (The Snapshot keeps history *content* safe
   regardless — that design pays off throughout this epic.)

Two features answer this, staged separately within one epic: **Practice Session archiving**
(tidy the session list) and **Inline Units** (stop the AI's one-off drills from entering the
library at all).

## 2. Archiving — single concept, not two kinds of session

**Archived is a lifecycle state on Practice Session, not a second concept.** Rejected: a
"one-off plan" type distinct from reusable templates, with ephemerality declared at creation.
Reasoning: you don't know at creation which sessions turn out reusable — a one-off the AI
planned might be worth repeating, which would force a "promote to template" conversion and a
permanent upfront classification burden on the planning conversation. One concept + state keeps
the glossary clean and the MCP flow light.

Terminology guard: the unarchived state deliberately has **no name** — never "Active", which
belongs to the Range Session lifecycle.

## 3. Archiving semantics

| Capability | Archived Practice Session |
| --- | --- |
| Appear in default session list | **No** — reachable via a separate Archived destination |
| View | Yes, fully |
| Duplicate | Yes, directly — produces a fresh unarchived, independent session |
| Start a Range Session | **No** — unarchive first (one tap) |
| Edit | **No** — unarchive first |
| Delete | Yes — archive sits *before* delete, it doesn't replace it |
| Range Session history | Unaffected; history keeps grouping under the archived template |

The strict start/edit rule was chosen over "archived is just a list filter" because
unarchiving is trivially cheap and the loose version makes the state mean nothing.

**Archiving with an Active Range Session in flight is allowed, unconditionally.** The Snapshot
makes the running session immune; blocking would add an illusory safety rule and break the
MCP tidy-up flow on forgotten Active sessions. No warnings, no special states.

## 4. Archiving triggers

- **Manual action** on the session (app and MCP).
- **A quiet affordance on the Range Session finish screen** — secondary, not jumping out,
  matching the established passive-affordance stance (Block Result notes are "never prompted
  or required"). It archives the *source template*, and self-hides if the template is already
  archived. Rejected: auto-archive on completion (fights genuinely reusable templates) and
  manual-only (the feature would exist but not help).

## 5. Archiving surfaces

- **App:** a separate **Archived destination** (quiet entry point off the session list — e.g.
  overflow item or footer row), not a filter toggle on the main list. The two populations have
  different capability rules, so separate screens keep every screen's actions uniformly valid.
- **MCP:** two explicit tools, `archive_session(id)` and `unarchive_session(id)` — matching
  the existing explicit-verb surface (`create_session`, `create_tag`); explicit names are
  self-documenting for the model, and unarchive is a distinct conversational intent. Rejected:
  a `set_session_archived(id, bool)` flag tool, and a general `update_session` (scope creep —
  general mutation invented to smuggle in one state change). `list_sessions` gains
  `include_archived` (default `false`); archived results carry a visible archived marker.
  Motivating flow: a user running their own knowledge base outside Rangework tells the AI a
  program is finished; the AI tidies the related sessions.

## 6. Inline Units — owned, not hidden

An **Inline Unit** is a Practice Unit owned by exactly one Practice Session, minted during AI
planning to fill a slot in that session. This does not contradict §2's rejection of upfront
classification: a session's reusability is unknowable at creation, but a unit minted
mid-conversation purely to fill a slot in the session being planned *is* known one-off by its
creator at creation.

**Ownership, not a visibility flag.** The unit lives in the normal `practice_units` table with
a nullable owning-session reference; but conceptually it is session content (like Session
Items), not a library citizen:

- Never appears in the unit library; can never be referenced by another session.
- **Lifecycle follows the owner:** dormant when the session is Archived; deleted (cascade)
  when the session is deleted; deep-copied when the session is duplicated.
- Rejected: a "hidden from library" flag with independent lifecycle — it quietly recreates the
  clutter invisibly, with orphaned units accumulating forever and no cleanup story. Also
  rejected: storing inline units outside the units table (too far — loses all shared machinery).

**Duplication is deep copy.** Each inline unit is copied and owned by the new session; both
sessions are fully independent. Rejected: sharing (breaks the single-owner invariant, needs
reference counting) and auto-promote-on-duplicate (promotion must be deliberate curation, not
a side effect). Consequence accepted: duplicating a one-off five times yields five invisible
near-identical inline units, each dying with its session — that's fine (nobody worries about
duplicated Session Items), and repeated duplication of a drill is precisely the signal to
promote it. Pre-committed answer to "shouldn't these be deduplicated?": **no**.

## 7. Promotion — the one-way escape hatch

An Inline Unit can be **promoted** to an ordinary library unit: ownership is detached, content
and identity unchanged, the session keeps referencing the same unit. Promotion is what makes
the hard lifecycle coupling (cascade delete) safe to commit to — anything worth keeping has a
way out first.

- **One-way.** No demotion (a library unit may be referenced by other sessions; no real itch).
- **Always user-initiated, never silent.** Two surfaces: a button on the inline unit within
  its session in the app; and an MCP `promote_unit(id)` tool the AI invokes when the user asks
  conversationally ("I loved that drill from Tuesday — use it today"), finding the unit
  through the session it lives in.

## 8. Inline Unit creation and editing

- **MCP-only creation for now.** The itch is AI-minted clutter; hand-planning already has the
  deliberate library→compose flow. The model deliberately doesn't care who created the unit,
  so app-side inline creation later is UI work only.
- **Necessarily embedded in `create_session`:** each item in the payload takes a `unit_id`
  *or* an inline unit definition (same shape as `create_unit`); the server creates session,
  inline units, and items atomically in one RPC (matching the atomic-planning-save pattern).
  The two-step alternative has no valid ID ordering: the session doesn't exist to scope to,
  and the session's items need unit IDs that don't exist — and inventing an
  add-item-to-session mutation to break the cycle is worse.
- **`list_units` stays pure library** — no `include_inline` flag. Inline units are reached
  through their session's detail. The API shape mirrors the ownership model.
- **App editing reuses the existing unit editor**, navigated from the session editing screen;
  saves key off the unit's ID, so the editor needn't know it's editing an inline unit. No new
  editing surface.

## 9. Storage recommendations (confirm in stage plans)

- Archived state: nullable **`archived_at timestamptz`** on `practice_sessions` (not a
  boolean — "when" comes free).
- Inline ownership: nullable **`scoped_to_session_id uuid`** on `practice_units`,
  `references practice_sessions (id) on delete cascade`. Promotion = set null.
- The ownership + cascade decision gets an **ADR** (hard to reverse, surprising to a future
  reader, real trade-off). Plain archiving does not (reversible, unsurprising).

## 10. Rejected and deferred

**Rejected for unit decluttering** (explored before landing on Inline Units):
derived visibility / "orphan shelf" (units referenced only by archived sessions sink),
recency-weighted library ordering, and tag-based curation (`scratch` tag — smuggles the
two-kinds classification back in). None felt right to the owner; Inline Units prevent the
clutter at the source instead of managing it after the fact.

**Deferred, not dead:**

- Unit archiving / decluttering of *existing* library units — archiving isn't the right model
  for units (no "done" moment); revisit if library clutter still hurts after Inline Units ship.
- App-side inline unit creation (create a unit inline while building a session by hand).
- Retro-scoping: Inline Units only prevent future clutter; AI-minted units that predate this
  epic stay in the library (bulk-delete by hand is acceptable at single-user scale).
- Demotion (library unit → inline).
