# App Review — 2026-07-15

A full usability and feature review of Rangework across all modules: the Android/KMP app
(`apps/mobile`), the MCP server (`apps/mcp`), the marketing site (`apps/site`), the Supabase
backend (`supabase/`), and the build/CI system. Conducted by five parallel review agents (two
Opus on the mobile surfaces, three Sonnet on MCP/backend/site+build), synthesized and
cross-checked against `main` at commit `82447f2`.

**Method note:** findings are from static code review, not runtime testing. Every concrete claim
carries a `file:line` reference and was reported against the current `main` — several
previously-tracked issues (the OAuth consent race, the broken MCP regression script) were
verified as *already fixed* and are deliberately absent from these documents.

## Files

| File | Contents |
| --- | --- |
| [potential-bugs.md](potential-bugs.md) | Defects and latent defects, ranked High/Medium/Low, across app, shared, MCP, DB, site |
| [usability.md](usability.md) | Friction points and UX gaps — range execution flow first, then planning surfaces and the site |
| [high-value-features.md](high-value-features.md) | Feature opportunities ranked by impact vs effort, grounded in what the data model already supports |
| [integration-opportunities.md](integration-opportunities.md) | Cross-module seams: MCP tool-surface gaps, missing SQL aggregation layer, offline, the triplicated domain-rules problem |
| [tech-debt.md](tech-debt.md) | Structural debt: localization, test-coverage holes, CI scope, doc staleness, schema hygiene |
| [bugfix-pipeline/](bugfix-pipeline/README.md) | Execution plan for burning down potential-bugs.md via autonomous agent runs: strategy, to-dos, batches |

## Top priorities across all categories

The ten items most worth acting on, regardless of category:

1. **Guard the range-session lifecycle transitions** — `finishSession`/`abandonSession` are
   unconditional updates; a stray abandon on a completed session silently destroys history.
   ([potential-bugs.md](potential-bugs.md) B1)
2. **Fix the double-tap duplicate range-session start** on the session detail screen — the
   fix pattern already exists on the picker path. (B2)
3. **Cross-session, per-club analytics** — three independent reviews converged on this as the
   headline payoff of the ball-granular recording investment; the data is fully captured, only
   the aggregation layer is missing. ([high-value-features.md](high-value-features.md) F1, and
   the flattened SQL view in [integration-opportunities.md](integration-opportunities.md) I2)
4. **Cap the audit-log growth on step completion** — whole-column JSONB diffing makes ball-tap
   auditing O(N²) per session. (B4)
5. **Harden `closeTimeEntry` and reconcile the two duration computations** — silent time-record
   loss via timestamp-string equality matching. (B3)
6. **Widen CI to cover `apps/site` and `apps/mcp`** — two of five modules currently merge with
   zero automated checks. ([tech-debt.md](tech-debt.md) D2)
7. **Add MCP `update_unit`/`update_session`/`duplicate_session` tools** — the DB RPCs already
   support all three; "tweak this drill" currently forces recreation. (I1)
8. **Externalize UI strings** — zero `stringResource` usage today; the single biggest
   pre-launch debt. (D1)
9. **Wire the sign-in Terms & Privacy links and fix the Play Store CTA** — two dead-end
   affordances on the app's and site's most important consent/conversion surfaces.
   ([usability.md](usability.md) U8, [potential-bugs.md](potential-bugs.md) B12)
10. **Decide the offline story** — every ball tap is a network round-trip; the ball-granular
    step model is already the right shape for a local outbox. (I4)

## What is in good shape

Worth stating so the findings read in proportion: the range-execution concurrency handling
(mutex-serialized completion writes, optimistic revert, ordered per-block writers), snapshot
forward/backward compatibility, accessibility semantics in the execution UI, MCP tool
descriptions and the coaching guide, MCP test coverage (22 files, ~4,900 lines), and the
RLS/index alignment on the range-session query paths were all independently called out as
strengths.
