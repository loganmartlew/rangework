grant usage on schema public to authenticated;

grant select, insert, update, delete
on table public.user_preferences
to authenticated;

grant select, insert, update, delete
on table public.practice_units
to authenticated;

grant select, insert, update, delete
on table public.practice_unit_instructions
to authenticated;

grant select, insert, update, delete
on table public.practice_sessions
to authenticated;

grant select, insert, update, delete
on table public.practice_session_items
to authenticated;

grant usage, select
on all sequences in schema public
to authenticated;
