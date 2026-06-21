-- Helper called by the delete-account Edge Function (service_role only).
-- Scrubs PII from the audit log before the auth.users row is removed.
-- SECURITY DEFINER so the function can reach the audit schema, which is
-- not exposed via PostgREST to ordinary callers.

create or replace function public.scrub_user_audit_log(p_user_id uuid)
returns void
language plpgsql
security definer
set search_path = public, audit
as $$
begin
  delete from audit.events where actor_id = p_user_id;
end;
$$;

revoke execute on function public.scrub_user_audit_log(uuid) from public, authenticated, anon;
grant execute on function public.scrub_user_audit_log(uuid) to service_role;
