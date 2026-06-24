# .agents — harness-agnostic AI context

This directory is the single source of truth for AI agent instructions in this repository. The goal is to write conventions once here and have every agent harness load them through thin, zero-duplication adapters.

## Layout

```
.agents/
  instructions/         on-demand detail docs (load only when relevant)
    android-app.md      Compose UI, ViewModels, navigation, screens, components
    typography.md       DM Sans / DM Mono rules, colour pairings, prohibited patterns
    shared.md           KMP shared module — models, use cases, repositories
    supabase.md         SQL migrations, RLS, schema conventions
    build-system.md     Gradle, libs.versions.toml, CI, pnpm/Turbo
    mcp.md              apps/mcp — Cloudflare Worker, MCP tools, TypeScript
    site.md             apps/site — Astro marketing site
  skills/               installable agent skills (Matt Pocock skills format)
  README.md             this file
```

The always-on core lives in `AGENTS.md` at the repo root. It is intentionally small: overview, build commands, secrets policy, module map, and a routing table that points here.

## Harness wiring

| Harness | Entry file | Mechanism |
| --- | --- | --- |
| Claude Code | `CLAUDE.md` | `@AGENTS.md` native import — Claude Code inlines it automatically |
| GitHub Copilot (global) | `.github/copilot-instructions.md` | Pointer to `AGENTS.md`; loaded for every workspace file |
| GitHub Copilot (path-scoped) | `.github/instructions/*.instructions.md` | `applyTo` frontmatter triggers load; body points to `.agents/instructions/<x>.md` |
| Zed / opencode / Cursor / Codex / any other | `AGENTS.md` | Most harnesses auto-load `AGENTS.md` at repo root |

### Adding a new harness

1. Find out what filename the harness auto-loads (e.g. `CURSOR.md`, `.opencode/instructions.md`).
2. Create that file as a one-line pointer: "Read AGENTS.md for the full conventions."
3. If the harness supports path-scoped instructions, mirror the `.github/instructions/` pattern.
4. Do not copy content — the pointer is enough.

### Adding a generator (if drift becomes a problem)

If harnesses start ignoring pointers and drift returns, add a `scripts/sync-agents.ts` that reads `.agents/instructions/*.md` and emits full native content for each harness. Add a CI step that fails if the generated files are stale. The pointer approach was chosen first to avoid tooling overhead; the generator slots in here without changing the `.agents/` structure.

## Skills

Skills in `.agents/skills/` use the Matt Pocock skills format (`SKILL.md` + `skills-lock.json` at root). They are also symlinked into `.claude/skills/` so Claude Code can discover them.

To install additional skills:

```powershell
# install from mattpocock/skills registry
npx mpc install <skill-name>
```
