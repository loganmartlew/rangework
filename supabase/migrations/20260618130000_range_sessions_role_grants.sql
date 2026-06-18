-- Grant table-level permissions on range session tables to the authenticated role.
-- The start_range_session RPC is security invoker, so the authenticated role needs
-- DML rights in addition to the already-granted EXECUTE on the function.

grant select, insert, update, delete
on table public.range_sessions
to authenticated;

grant select, insert, update, delete
on table public.range_session_time_entries
to authenticated;
