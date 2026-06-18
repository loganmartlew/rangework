-- The original grant (select, insert, delete) omitted UPDATE.
-- SupabaseClubRepository.setClubEnabled uses upsert (INSERT ... ON CONFLICT DO UPDATE),
-- which requires UPDATE privilege even when no conflict occurs.
-- Without it every enable-club call fails with a permission error.

grant update on table public.user_enabled_clubs to authenticated;
