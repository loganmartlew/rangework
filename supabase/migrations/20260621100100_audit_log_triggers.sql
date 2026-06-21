-- Attach audit.log_change() to every user-mutable table.
-- TG_ARGV[0] is supplied when the PK column is not named 'id'.

drop trigger if exists audit_profiles on public.profiles;
create trigger audit_profiles
after insert or update or delete on public.profiles
for each row execute function audit.log_change();

drop trigger if exists audit_user_preferences on public.user_preferences;
create trigger audit_user_preferences
after insert or update or delete on public.user_preferences
for each row execute function audit.log_change('user_id');

drop trigger if exists audit_practice_units on public.practice_units;
create trigger audit_practice_units
after insert or update or delete on public.practice_units
for each row execute function audit.log_change();

drop trigger if exists audit_practice_unit_instructions on public.practice_unit_instructions;
create trigger audit_practice_unit_instructions
after insert or update or delete on public.practice_unit_instructions
for each row execute function audit.log_change();

drop trigger if exists audit_practice_sessions on public.practice_sessions;
create trigger audit_practice_sessions
after insert or update or delete on public.practice_sessions
for each row execute function audit.log_change();

drop trigger if exists audit_practice_session_items on public.practice_session_items;
create trigger audit_practice_session_items
after insert or update or delete on public.practice_session_items
for each row execute function audit.log_change();

drop trigger if exists audit_range_sessions on public.range_sessions;
create trigger audit_range_sessions
after insert or update or delete on public.range_sessions
for each row execute function audit.log_change();

drop trigger if exists audit_range_session_time_entries on public.range_session_time_entries;
create trigger audit_range_session_time_entries
after insert or update or delete on public.range_session_time_entries
for each row execute function audit.log_change();

drop trigger if exists audit_user_enabled_clubs on public.user_enabled_clubs;
create trigger audit_user_enabled_clubs
after insert or update or delete on public.user_enabled_clubs
for each row execute function audit.log_change('user_id,club_code');
