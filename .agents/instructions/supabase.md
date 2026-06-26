# Supabase instructions

`supabase/` is the source of truth for backend schema and local project configuration.

## Schema overview

Planning data centers on: `practice_units`, `practice_unit_instructions`, `practice_sessions`, `practice_session_items`, `user_preferences`, `clubs` (catalog), and `user_enabled_clubs`. Tagging adds an owner-nullable `tags` table (NULL owner = Default Tag) plus `practice_unit_tags` / `practice_session_tags` join tables, the `slugify_tag` / `create_or_get_tag` / `count_tag_attachments` functions, and a `p_tag_ids uuid[]` parameter on the save RPCs. Includes atomic save RPCs (`save_practice_unit`, `save_practice_session`).

## Conventions

- Prefer new timestamped migration files for schema evolution; do not silently rewrite existing migration history unless the task explicitly calls for it.
- Keep row-level security and ownership rules intact. The schema scopes access through `auth.uid()`, `profiles`, and owner/user foreign keys.
- Follow existing migration patterns: explicit constraints, `updated_at` triggers via `public.set_updated_at()`, indexes on ownership/sort-order access paths.
- Planning schema changes require matching updates in `apps/mobile/shared/src/commonMain/.../data/Supabase*Repository.kt` and related model/use-case code. Do not change SQL in isolation if Kotlin mappings depend on it.
- `supabase/config.toml` can contain project metadata and disabled provider placeholders, but never commit real provider secrets or service-role credentials.
