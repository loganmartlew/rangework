-- Atomically mutate range-session completion progress.
--
-- `completed_steps` is a JSONB array. The original client implementation did a
-- SELECT, merged one counter tap locally, then replaced the whole array. Rapid
-- ball capture therefore allowed two requests to read the same baseline and
-- overwrite one another. Lock the session row and perform the merge inside one
-- database transaction so every accepted tap is retained.

create or replace function public.set_range_session_steps_completion(
  p_range_session_id uuid,
  p_step_indices integer[],
  p_completed boolean
) returns jsonb
language plpgsql
security invoker
set search_path = ''
as $$
declare
  v_completed_steps jsonb;
  v_updated_steps jsonb;
  v_completed_at timestamptz := now();
  v_step_index integer;
  v_result jsonb;
begin
  -- RLS enforces ownership. FOR UPDATE serializes every completion mutation for
  -- this session, including requests arriving from another client instance.
  select rs.completed_steps
  into v_completed_steps
  from public.range_sessions as rs
  where rs.id = p_range_session_id
  for update;

  if not found then
    raise exception 'Range session not found or not accessible';
  end if;

  v_completed_steps := coalesce(v_completed_steps, '[]'::jsonb);

  if p_completed then
    v_updated_steps := v_completed_steps;

    -- Preserve caller order, ignore duplicate/negative indices, and give every
    -- step completed by this one counter tap the same timestamp.
    for v_step_index in
      select candidate.step_index
      from unnest(coalesce(p_step_indices, array[]::integer[]))
        with ordinality as candidate(step_index, ordinal)
      where candidate.step_index >= 0
      group by candidate.step_index
      order by min(candidate.ordinal)
    loop
      if not exists (
        select 1
        from jsonb_array_elements(v_updated_steps) as existing(completed_step)
        where (existing.completed_step ->> 'stepIndex')::integer = v_step_index
      ) then
        v_updated_steps := v_updated_steps || jsonb_build_object(
          'stepIndex', v_step_index,
          'completedAt', v_completed_at
        );
      end if;
    end loop;
  else
    select coalesce(
      jsonb_agg(existing.completed_step order by existing.ordinal),
      '[]'::jsonb
    )
    into v_updated_steps
    from jsonb_array_elements(v_completed_steps)
      with ordinality as existing(completed_step, ordinal)
    where not (
      (existing.completed_step ->> 'stepIndex')::integer = any(
        coalesce(p_step_indices, array[]::integer[])
      )
    );
  end if;

  update public.range_sessions as rs
  set completed_steps = v_updated_steps
  where rs.id = p_range_session_id
  returning to_jsonb(rs.*) into v_result;

  return v_result;
end;
$$;

grant execute on function public.set_range_session_steps_completion(uuid, integer[], boolean)
  to authenticated;
