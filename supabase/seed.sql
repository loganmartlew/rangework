-- Phase 2 keeps auth seeding lightweight.
-- Create users through Supabase Auth, then this seed can backfill profile names
-- from auth.users without storing any credentials in source control.

update public.profiles as profiles
set display_name = coalesce(profiles.display_name, split_part(users.email, '@', 1))
from auth.users as users
where profiles.id = users.id
  and users.email is not null;
