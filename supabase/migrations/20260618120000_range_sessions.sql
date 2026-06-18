-- Range Sessions: execution-time snapshots of practice sessions.
-- Tables, indexes, RLS, trigger, and start_range_session RPC.

-- ============================================================
-- 1. range_sessions table
-- ============================================================

create table if not exists public.range_sessions (
  id uuid primary key default gen_random_uuid(),
  owner_id uuid not null default auth.uid()
    references public.profiles (id) on delete cascade,
  source_session_id uuid
    references public.practice_sessions (id) on delete set null,
  session_name text not null,
  snapshot jsonb not null,
  snapshot_version integer not null default 1,
  completed_steps jsonb not null default '[]'::jsonb,
  club_overrides jsonb not null default '{}'::jsonb,
  last_viewed_step_index integer,
  started_at timestamptz not null default timezone('utc', now()),
  completed_at timestamptz,
  abandoned_at timestamptz,
  created_at timestamptz not null default timezone('utc', now()),
  updated_at timestamptz not null default timezone('utc', now())
);

-- ============================================================
-- 2. range_session_time_entries table
-- ============================================================

create table if not exists public.range_session_time_entries (
  id uuid primary key default gen_random_uuid(),
  range_session_id uuid not null
    references public.range_sessions (id) on delete cascade,
  entered_at timestamptz not null,
  exited_at timestamptz
);

-- ============================================================
-- 3. Indexes
-- ============================================================

create index if not exists range_sessions_owner_active_idx
  on public.range_sessions (owner_id, started_at desc)
  where completed_at is null and abandoned_at is null;

create index if not exists range_sessions_source_session_idx
  on public.range_sessions (source_session_id, completed_at desc)
  where completed_at is not null and abandoned_at is null;

-- ============================================================
-- 4. updated_at trigger
-- ============================================================

drop trigger if exists set_range_sessions_updated_at on public.range_sessions;
create trigger set_range_sessions_updated_at
before update on public.range_sessions
for each row
execute function public.set_updated_at();

-- ============================================================
-- 5. RLS policies
-- ============================================================

alter table public.range_sessions enable row level security;
alter table public.range_session_time_entries enable row level security;

drop policy if exists "Users can manage their own range sessions" on public.range_sessions;
create policy "Users can manage their own range sessions"
on public.range_sessions
for all
using (auth.uid() = owner_id)
with check (auth.uid() = owner_id);

drop policy if exists "Users can manage time entries on their own range sessions" on public.range_session_time_entries;
create policy "Users can manage time entries on their own range sessions"
on public.range_session_time_entries
for all
using (
  exists (
    select 1
    from public.range_sessions
    where range_sessions.id = range_session_time_entries.range_session_id
      and range_sessions.owner_id = auth.uid()
  )
)
with check (
  exists (
    select 1
    from public.range_sessions
    where range_sessions.id = range_session_time_entries.range_session_id
      and range_sessions.owner_id = auth.uid()
  )
);

-- ============================================================
-- 6. start_range_session RPC
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

    -- Resolve club: item override takes priority, then unit default
    v_club_code := coalesce(v_item.club_reference, v_unit.default_club_reference);
    v_club_display_name := null;

    if v_club_code is not null then
      select c.display_name into v_club_display_name
      from public.clubs c
      where c.code = v_club_code;
    end if;

    -- Build instructions array and expand steps
    v_instructions := '[]'::jsonb;
    v_instruction_index := 0;

    for v_instr in
      select pui.*
      from public.practice_unit_instructions pui
      where pui.practice_unit_id = v_unit.id
      order by pui.sort_order
    loop
      v_instructions := v_instructions || jsonb_build_object(
        'text', v_instr.text,
        'ballCount', v_instr.ball_count
      );

      for v_rep in 1..v_item.repeat_count loop
        v_steps := v_steps || jsonb_build_object(
          'unitIndex', v_unit_index,
          'instructionIndex', v_instruction_index,
          'repNumber', v_rep,
          'totalReps', v_item.repeat_count,
          'instructionText', v_instr.text,
          'ballCount', v_instr.ball_count,
          'club', v_club_code,
          'clubDisplayName', v_club_display_name,
          'unitTitle', v_unit.title,
          'notes', v_item.notes,
          'focusCue', v_item.focus_cue
        );
      end loop;

      v_instruction_index := v_instruction_index + 1;
    end loop;

    -- Add unit to snapshot
    v_units := v_units || jsonb_build_object(
      'unitTitle', v_unit.title,
      'unitNotes', v_unit.notes,
      'unitFocus', v_unit.focus,
      'itemNotes', v_item.notes,
      'itemFocusCue', v_item.focus_cue,
      'club', v_club_code,
      'clubDisplayName', v_club_display_name,
      'repeatCount', v_item.repeat_count,
      'instructions', v_instructions
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

  -- Insert range_sessions row
  insert into public.range_sessions (
    id, owner_id, source_session_id, session_name, snapshot, snapshot_version
  ) values (
    p_range_session_id, auth.uid(), p_session_id, v_session.name, v_snapshot, 1
  );

  -- Return the full inserted row as JSONB
  select to_jsonb(rs.*) into v_result
  from public.range_sessions rs
  where rs.id = p_range_session_id;

  return v_result;
end;
$$;

grant execute on function public.start_range_session(uuid, uuid)
  to authenticated;
