-- Audit log: debugging-only record of row mutations across user-owned tables.
-- Schema lives outside `public` so PostgREST does not expose it. Reads are
-- service-role only (RLS enabled, no policies). Trigger function is shared by
-- every audited table and is wired up in the follow-up triggers migration.

create schema if not exists audit;

create table if not exists audit.events (
  id           bigint generated always as identity primary key,
  occurred_at  timestamptz not null default timezone('utc', now()),
  actor_id     uuid,
  table_name   text not null,
  action       text not null check (action in ('INSERT', 'UPDATE', 'DELETE')),
  row_id       uuid,
  row_pk       jsonb not null,
  old_values   jsonb,
  new_values   jsonb
);

create index if not exists events_table_row_idx
  on audit.events (table_name, row_id, occurred_at desc);

create index if not exists events_actor_idx
  on audit.events (actor_id, occurred_at desc);

create index if not exists events_occurred_idx
  on audit.events (occurred_at);

alter table audit.events enable row level security;

-- Generic trigger function.
--
-- PK resolution: TG_ARGV[0] may be a comma-separated list of PK column names
-- (e.g. 'user_id,club_code' for composite keys). Defaults to 'id'.
--
-- Diff behavior on UPDATE: skips rows where the only changes are to
-- created_at/updated_at, and stores only the changed columns in old/new_values.
create or replace function audit.log_change()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_pk_keys    text[];
  v_pk_key     text;
  v_old_row    jsonb;
  v_new_row    jsonb;
  v_source_row jsonb;
  v_row_pk     jsonb := '{}'::jsonb;
  v_row_id     uuid;
  v_new_diff   jsonb;
  v_old_diff   jsonb;
begin
  if TG_NARGS > 0 then
    v_pk_keys := string_to_array(TG_ARGV[0], ',');
  else
    v_pk_keys := array['id'];
  end if;

  if TG_OP = 'DELETE' then
    v_old_row := to_jsonb(OLD);
    v_source_row := v_old_row;
  else
    v_new_row := to_jsonb(NEW);
    v_source_row := v_new_row;
    if TG_OP = 'UPDATE' then
      v_old_row := to_jsonb(OLD);
    end if;
  end if;

  foreach v_pk_key in array v_pk_keys loop
    v_row_pk := v_row_pk || jsonb_build_object(v_pk_key, v_source_row -> v_pk_key);
  end loop;

  if array_length(v_pk_keys, 1) = 1 then
    begin
      v_row_id := (v_source_row ->> v_pk_keys[1])::uuid;
    exception when others then
      v_row_id := null;
    end;
  end if;

  if TG_OP = 'INSERT' then
    insert into audit.events (actor_id, table_name, action, row_id, row_pk, new_values)
    values (auth.uid(), TG_TABLE_NAME, 'INSERT', v_row_id, v_row_pk, v_new_row);
    return NEW;

  elsif TG_OP = 'DELETE' then
    insert into audit.events (actor_id, table_name, action, row_id, row_pk, old_values)
    values (auth.uid(), TG_TABLE_NAME, 'DELETE', v_row_id, v_row_pk, v_old_row);
    return OLD;

  else
    select coalesce(jsonb_object_agg(k, v_new_row -> k), '{}'::jsonb)
      into v_new_diff
    from jsonb_object_keys(v_new_row) as k
    where k not in ('updated_at', 'created_at')
      and (v_new_row -> k) is distinct from (v_old_row -> k);

    if v_new_diff = '{}'::jsonb then
      return NEW;
    end if;

    select coalesce(jsonb_object_agg(k, v_old_row -> k), '{}'::jsonb)
      into v_old_diff
    from jsonb_object_keys(v_new_diff) as k;

    insert into audit.events (actor_id, table_name, action, row_id, row_pk, old_values, new_values)
    values (auth.uid(), TG_TABLE_NAME, 'UPDATE', v_row_id, v_row_pk, v_old_diff, v_new_diff);

    return NEW;
  end if;
end;
$$;

grant execute on function audit.log_change() to authenticated, anon;
