-- user_preferences.user_id had no default, so inserting a preferences row required
-- the client to supply the id explicitly. The repository insert omits it (mirroring
-- the practice_units.owner_id pattern), which meant first-time saves violated the
-- NOT NULL primary key and the RLS `with check (auth.uid() = user_id)` policy, so
-- measurement preferences never persisted. Default to auth.uid() like other tables.
alter table public.user_preferences
  alter column user_id set default auth.uid();
