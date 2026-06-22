-- Make the audit actor fall back to the row owner when auth.uid() is null
-- (e.g. service-role cascade deletes during account deletion). This lets the
-- account-deletion scrub remove deletion-generated events by the user's id.

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
  v_actor      uuid;
  v_owner_txt  text;
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

  -- Resolve actor, falling back to the row's owner when there is no JWT actor.
  v_owner_txt := coalesce(
    v_source_row ->> 'owner_id',
    v_source_row ->> 'user_id',
    case when TG_TABLE_NAME = 'profiles' then v_source_row ->> 'id' end
  );
  begin
    v_actor := coalesce(auth.uid(), v_owner_txt::uuid);
  exception when others then
    v_actor := auth.uid();
  end;

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
    values (v_actor, TG_TABLE_NAME, 'INSERT', v_row_id, v_row_pk, v_new_row);
    return NEW;

  elsif TG_OP = 'DELETE' then
    insert into audit.events (actor_id, table_name, action, row_id, row_pk, old_values)
    values (v_actor, TG_TABLE_NAME, 'DELETE', v_row_id, v_row_pk, v_old_row);
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
    values (v_actor, TG_TABLE_NAME, 'UPDATE', v_row_id, v_row_pk, v_old_diff, v_new_diff);

    return NEW;
  end if;
end;
$$;
