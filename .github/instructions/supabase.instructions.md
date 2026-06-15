---
applyTo: "supabase/**/*.sql,supabase/**/*.toml"
---

# Supabase instructions

- Treat `supabase` as the source of truth for backend schema and local project configuration.
- Prefer new timestamped migration files for schema evolution instead of silently rewriting existing migration history, unless the task explicitly calls for editing an existing migration.
- Keep row-level security and ownership rules intact. This app is private-per-user and the current schema consistently scopes access through `auth.uid()`, `profiles`, and owner/user foreign keys.
- Follow the existing migration patterns: explicit constraints, `updated_at` triggers via `public.set_updated_at()`, and indexes on ownership/sort-order access paths.
- Planning schema changes usually require matching updates in `shared/src/commonMain/.../data/Supabase*Repository.kt` and related model/use-case code. Do not change SQL in isolation if Kotlin mappings depend on it.
- `supabase/config.toml` can contain project metadata and disabled provider placeholders, but never commit real provider secrets or service-role credentials.
