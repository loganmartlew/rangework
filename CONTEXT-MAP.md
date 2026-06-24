# Context Map

## Contexts

- [Planning & Execution](./apps/mobile/CONTEXT.md) — core golf practice planning model; the authoritative vocabulary for Practice Units, Practice Sessions, Range Sessions, Clubs, and execution state
- [Coaching](./apps/mcp/CONTEXT.md) — AI coaching conversation protocol; terms specific to the MCP server and planning dialogue
- Site (`apps/site/`) — marketing and support website; introduces no new domain terms; all planning vocabulary must match Planning & Execution exactly

## Relationships

- **Coaching → Planning & Execution**: The Coaching context creates and reads Practice Units and Practice Sessions via the same Supabase schema. All planning-layer terms (Practice Unit, Practice Session, Session Item, Club Code, etc.) carry the same meaning in both contexts — defined once in Planning & Execution, used without re-definition in Coaching.
- **Site → Planning & Execution**: Marketing copy refers to planning concepts by name. No synonyms or shorthand — copy must use the canonical terms from Planning & Execution (e.g. "practice session" not "session template", "session item" not "slot", "range session" not "live session").
