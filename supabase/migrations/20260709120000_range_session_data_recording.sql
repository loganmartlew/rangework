-- Range Session Data Recording — schema & snapshot v3 (Stage 1).
--
-- Lands every database object the Range Session Data Recording design needs in a
-- single migration, plus the RPC changes that write and snapshot them. No app
-- code ships in this stage: the new columns/tables are inert until Stages 3–5
-- give them UI and Stage 7 gives them MCP surface. See
-- design-docs/range-session-data-recording/ (epic + stage-01-schema plan, D1–D3).
--
-- Fixed vocabulary (Observation Type identifiers), lowercase snake_case, mirrored
-- as the Stage 2 enum wire values:
--   success, strike_location, contact, shape, distance, direction
--
-- Snapshot v3 adds two keys per unit entry (successCriterion, observationTypes);
-- step expansion (ADR 0004) is copied verbatim from v2. Every new session is
-- stamped snapshot_version = 3 regardless of feature usage; v1/v2 rows are never
-- migrated. The current app tolerates the widened rows and v3 snapshots via
-- supabase-kt's ignoreUnknownKeys default (verified in the Stage 1 plan).

-- ============================================================
-- 1. practice_units.success_criterion (nullable text)
--
-- Null-or-trimmed-non-empty, matching the practice_units_title_check pattern.
-- The save RPC normalizes blank → null.
-- ============================================================

alter table public.practice_units
  add column if not exists success_criterion text;

alter table public.practice_units
  drop constraint if exists practice_units_success_criterion_check;

alter table public.practice_units
  add constraint practice_units_success_criterion_check
    check (success_criterion is null or char_length(trim(success_criterion)) > 0);

-- ============================================================
-- 2. practice_session_items.observation_types (text[])
--
-- Native array of the fixed Observation Type vocabulary; PostgREST serializes it
-- as a JSON array. Containment check is one expression; a future type is a
-- one-line constraint migration shipped with the app change that introduces it.
-- ============================================================

alter table public.practice_session_items
  add column if not exists observation_types text[] not null default '{}'::text[];

alter table public.practice_session_items
  drop constraint if exists practice_session_items_observation_types_check;

alter table public.practice_session_items
  add constraint practice_session_items_observation_types_check
    check (observation_types <@ array[
      'success', 'strike_location', 'contact', 'shape', 'distance', 'direction'
    ]::text[]);

-- ============================================================
-- 3. user_preferences.handedness (text not null default 'RIGHT')
--
-- Uppercase preference-enum style; RIGHT matches the app's current implicit
-- rendering. Existing rows pick up the default.
-- ============================================================

alter table public.user_preferences
  add column if not exists handedness text not null default 'RIGHT';

alter table public.user_preferences
  drop constraint if exists user_preferences_handedness_check;

alter table public.user_preferences
  add constraint user_preferences_handedness_check
    check (handedness in ('RIGHT', 'LEFT'));

-- ============================================================
-- 4. range_sessions: session_note + block_results (D1)
--
-- session_note is singular, distinct from the snapshot's planning-time
-- sessionNotes. block_results is a JSONB map keyed by unitIndex-as-string,
-- mirroring club_overrides exactly:
--   { "0": { "note": "left misses all day", "manualCount": 4 } }
-- Both inner fields optional; app-level freeze enforcement only (D3).
-- ============================================================

alter table public.range_sessions
  add column if not exists session_note text;

alter table public.range_sessions
  add column if not exists block_results jsonb not null default '{}'::jsonb;

-- ============================================================
-- 5. range_session_observations table (D2)
--
-- One row per observed ball, keyed by (range_session_id, step_index). No row and
-- an empty '{}' row both mean "unobserved". The DB never validates value strings
-- or that step_index addresses a Ball Step — Stage 2 owns those rules.
-- ============================================================

create table if not exists public.range_session_observations (
  id uuid primary key default gen_random_uuid(),
  range_session_id uuid not null
    references public.range_sessions (id) on delete cascade,
  step_index integer not null,
  observed_values jsonb not null default '{}'::jsonb,
  created_at timestamptz not null default timezone('utc', now()),
  updated_at timestamptz not null default timezone('utc', now()),
  constraint range_session_observations_step_index_check
    check (step_index >= 0),
  constraint range_session_observations_observed_values_check
    check (jsonb_typeof(observed_values) = 'object'),
  constraint range_session_observations_session_step_unique
    unique (range_session_id, step_index)
);

drop trigger if exists set_range_session_observations_updated_at on public.range_session_observations;
create trigger set_range_session_observations_updated_at
before update on public.range_session_observations
for each row
execute function public.set_updated_at();

drop trigger if exists audit_range_session_observations on public.range_session_observations;
create trigger audit_range_session_observations
after insert or update or delete on public.range_session_observations
for each row execute function audit.log_change();

alter table public.range_session_observations enable row level security;

drop policy if exists "Users can manage observations on their own range sessions" on public.range_session_observations;
create policy "Users can manage observations on their own range sessions"
on public.range_session_observations
for all
using (
  exists (
    select 1
    from public.range_sessions
    where range_sessions.id = range_session_observations.range_session_id
      and range_sessions.owner_id = auth.uid()
  )
)
with check (
  exists (
    select 1
    from public.range_sessions
    where range_sessions.id = range_session_observations.range_session_id
      and range_sessions.owner_id = auth.uid()
  )
);

grant select, insert, update, delete
on table public.range_session_observations
to authenticated;

-- ============================================================
-- 6. save_practice_unit: trailing p_success_criterion
--
-- The signature changes (a new trailing argument), so the current 7-arg function
-- is dropped before recreating — otherwise named-parameter callers that omit the
-- new arg become ambiguous between overloads. The default keeps app + MCP calls
-- working unchanged. The drop removes grants, so EXECUTE is re-granted.
-- ============================================================

drop function if exists public.save_practice_unit(uuid, text, text, text, text, jsonb, uuid[]);

create or replace function public.save_practice_unit(
  p_unit_id uuid,
  p_title text,
  p_notes text,
  p_focus text,
  p_default_club_code text,
  p_instructions jsonb,
  p_tag_ids uuid[] default '{}'::uuid[],
  p_success_criterion text default null
) returns void
language plpgsql
set search_path = ''
as $$
begin
  insert into public.practice_units
    (id, owner_id, title, notes, focus, default_club_code, success_criterion)
  values
    (p_unit_id, auth.uid(), p_title, p_notes, p_focus, p_default_club_code,
     nullif(trim(p_success_criterion), ''))
  on conflict (id) do update set
    title             = excluded.title,
    notes             = excluded.notes,
    focus             = excluded.focus,
    default_club_code = excluded.default_club_code,
    success_criterion = excluded.success_criterion
  where public.practice_units.owner_id = auth.uid();

  delete from public.practice_unit_instructions
  where practice_unit_id = p_unit_id;

  insert into public.practice_unit_instructions
    (id, practice_unit_id, sort_order, text, ball_count, club_code)
  select
    gen_random_uuid(),
    p_unit_id,
    (inst->>'order')::int,
    inst->>'text',
    case when inst->>'ball_count' is null then null
         else (inst->>'ball_count')::int end,
    inst->>'club_code'
  from jsonb_array_elements(p_instructions) inst;

  delete from public.practice_unit_tags
  where practice_unit_id = p_unit_id;

  insert into public.practice_unit_tags (practice_unit_id, tag_id)
  select distinct p_unit_id, tid
  from unnest(coalesce(p_tag_ids, '{}'::uuid[])) as tid;
end;
$$;

grant execute on function public.save_practice_unit(uuid, text, text, text, text, jsonb, uuid[], text)
  to authenticated;

-- ============================================================
-- 7. save_practice_session: persist observation_types + validate success rule
--
-- Signature unchanged (observation_types rides inside each p_items element), so
-- CREATE OR REPLACE. A missing key → empty array; duplicates are deduplicated.
-- After insert, reject any item that enables `success` on a unit with no
-- success_criterion — the atomic-boundary half of the two-layer success rule.
-- ============================================================

create or replace function public.save_practice_session(
  p_session_id uuid,
  p_name text,
  p_notes text,
  p_items jsonb,
  p_tag_ids uuid[] default '{}'::uuid[]
) returns void
language plpgsql
set search_path = ''
as $$
begin
  insert into public.practice_sessions (id, owner_id, name, notes)
  values (p_session_id, auth.uid(), p_name, p_notes)
  on conflict (id) do update set
    name  = excluded.name,
    notes = excluded.notes
  where public.practice_sessions.owner_id = auth.uid();

  delete from public.practice_session_items
  where practice_session_id = p_session_id;

  insert into public.practice_session_items
    (id, practice_session_id, practice_unit_id, sort_order,
     repeat_count, club_code, notes, focus_cue, observation_types)
  select
    gen_random_uuid(),
    p_session_id,
    (item->>'practice_unit_id')::uuid,
    (item->>'order')::int,
    (item->>'repeat_count')::int,
    item->>'club_code',
    item->>'notes',
    item->>'focus_cue',
    coalesce(
      (select array_agg(distinct value)
       from jsonb_array_elements_text(item->'observation_types') as value),
      '{}'::text[]
    )
  from jsonb_array_elements(p_items) item;

  -- Success-requires-criterion (atomic boundary). start_range_session applies the
  -- complementary snapshot-time filter for criteria removed after this save.
  if exists (
    select 1
    from public.practice_session_items psi
    join public.practice_units pu on pu.id = psi.practice_unit_id
    where psi.practice_session_id = p_session_id
      and 'success' = any(psi.observation_types)
      and pu.success_criterion is null
  ) then
    raise exception
      'Cannot enable the success observation on a unit without a success criterion';
  end if;

  delete from public.practice_session_tags
  where practice_session_id = p_session_id;

  insert into public.practice_session_tags (practice_session_id, tag_id)
  select distinct p_session_id, tid
  from unnest(coalesce(p_tag_ids, '{}'::uuid[])) as tid;
end;
$$;

grant execute on function public.save_practice_session(uuid, text, text, jsonb, uuid[])
  to authenticated;

-- ============================================================
-- 8. start_range_session v3
--
-- Snapshot version 3 adds two keys to each unit entry: successCriterion (the
-- unit's criterion in force at start, null when absent — editing the unit later
-- never reinterprets this session) and observationTypes (the item's enabled
-- types, with `success` filtered out when successCriterion is null, so derived
-- success counts always have a rubric). Every new session is stamped version 3.
--
-- Step expansion (ADR 0004) — one Ball Step per ball, Action Steps for 0/null —
-- is copied verbatim from snapshot v2; only the unit-entry keys and the version
-- literal differ.
-- ============================================================

create or replace function public.start_range_session(
  p_range_session_id uuid,
  p_session_id uuid
) returns jsonb
language plpgsql
security invoker
set search_path = ''
as $$
declare
  v_session record;
  v_snapshot jsonb;
  v_units jsonb := '[]'::jsonb;
  v_steps jsonb := '[]'::jsonb;
  v_unit_index integer := 0;
  v_item record;
  v_unit record;
  v_instr record;
  v_club_code text;
  v_club_display_name text;
  v_instructions jsonb;
  v_instruction_index integer;
  v_rep integer;
  v_expanded_count integer;
  v_step_ball_count integer;
  v_ball integer;
  v_success_criterion text;
  v_observation_types text[];
  v_result jsonb;
begin
  -- Read practice session (RLS enforces ownership)
  select * into v_session
  from public.practice_sessions
  where id = p_session_id;

  if not found then
    raise exception 'Practice session not found or not accessible';
  end if;

  -- Iterate over session items in order
  for v_item in
    select psi.*
    from public.practice_session_items psi
    where psi.practice_session_id = p_session_id
    order by psi.sort_order
  loop
    -- Read the practice unit (RLS enforces ownership)
    select * into v_unit
    from public.practice_units pu
    where pu.id = v_item.practice_unit_id;

    if not found then
      continue;
    end if;

    -- Success criterion in force at start; observation types with `success`
    -- filtered out when the criterion has since been removed from the unit.
    v_success_criterion := v_unit.success_criterion;
    v_observation_types := coalesce(v_item.observation_types, '{}'::text[]);
    if v_success_criterion is null then
      v_observation_types := array_remove(v_observation_types, 'success');
    end if;

    -- Build the instructions array for the snapshot unit entry, resolving each
    -- instruction's club via Club Resolution precedence.
    v_instructions := '[]'::jsonb;

    for v_instr in
      select pui.*
      from public.practice_unit_instructions pui
      where pui.practice_unit_id = v_unit.id
      order by pui.sort_order
    loop
      v_club_code := coalesce(v_item.club_code, v_instr.club_code, v_unit.default_club_code);
      v_club_display_name := null;

      if v_club_code is not null then
        select c.display_name into v_club_display_name
        from public.clubs c
        where c.code = v_club_code;
      end if;

      v_instructions := v_instructions || jsonb_build_object(
        'text', v_instr.text,
        'ballCount', v_instr.ball_count,
        'club', v_club_code,
        'clubDisplayName', v_club_display_name
      );
    end loop;

    -- Expand steps: rep outer, instruction inner, ball innermost.
    -- This produces: rep1-instr1-ball1, rep1-instr1-ball2, …, rep1-instr2-…,
    -- rep2-instr1-…, matching how a pass is executed on the range.
    for v_rep in 1..v_item.repeat_count loop
      v_instruction_index := 0;

      for v_instr in
        select pui.*
        from public.practice_unit_instructions pui
        where pui.practice_unit_id = v_unit.id
        order by pui.sort_order
      loop
        v_club_code := coalesce(v_item.club_code, v_instr.club_code, v_unit.default_club_code);
        v_club_display_name := null;

        if v_club_code is not null then
          select c.display_name into v_club_display_name
          from public.clubs c
          where c.code = v_club_code;
        end if;

        -- ADR 0004 expansion rule: N > 0 → N Ball Steps of one ball each;
        -- 0 and null (Uncounted) → a single Action Step keeping its value.
        if v_instr.ball_count is not null and v_instr.ball_count > 0 then
          v_expanded_count := v_instr.ball_count;
          v_step_ball_count := 1;
        else
          v_expanded_count := 1;
          v_step_ball_count := v_instr.ball_count;
        end if;

        for v_ball in 1..v_expanded_count loop
          v_steps := v_steps || jsonb_build_object(
            'unitIndex', v_unit_index,
            'instructionIndex', v_instruction_index,
            'repNumber', v_rep,
            'totalReps', v_item.repeat_count,
            'instructionText', v_instr.text,
            'ballCount', v_step_ball_count,
            'club', v_club_code,
            'clubDisplayName', v_club_display_name,
            'unitTitle', v_unit.title,
            'notes', v_item.notes,
            'focusCue', v_item.focus_cue
          );
        end loop;

        v_instruction_index := v_instruction_index + 1;
      end loop;
    end loop;

    -- Add unit to snapshot. The unit object carries no club of its own
    -- (per-instruction clubs live on each entry in the instructions array).
    -- v3 adds successCriterion + observationTypes.
    v_units := v_units || jsonb_build_object(
      'unitTitle', v_unit.title,
      'unitNotes', v_unit.notes,
      'unitFocus', v_unit.focus,
      'itemNotes', v_item.notes,
      'itemFocusCue', v_item.focus_cue,
      'repeatCount', v_item.repeat_count,
      'instructions', v_instructions,
      'successCriterion', v_success_criterion,
      'observationTypes', to_jsonb(v_observation_types)
    );

    v_unit_index := v_unit_index + 1;
  end loop;

  -- Validate steps not empty
  if jsonb_array_length(v_steps) = 0 then
    raise exception 'Session has no instructions';
  end if;

  -- Assemble snapshot
  v_snapshot := jsonb_build_object(
    'sessionNotes', v_session.notes,
    'units', v_units,
    'steps', v_steps
  );

  -- Insert range_sessions row (snapshot_version 3 = data-recording shape)
  insert into public.range_sessions (
    id, owner_id, source_session_id, session_name, snapshot, snapshot_version
  ) values (
    p_range_session_id, auth.uid(), p_session_id, v_session.name, v_snapshot, 3
  );

  -- Return the full inserted row as JSONB
  select to_jsonb(rs.*) into v_result
  from public.range_sessions rs
  where rs.id = p_range_session_id;

  return v_result;
end;
$$;
