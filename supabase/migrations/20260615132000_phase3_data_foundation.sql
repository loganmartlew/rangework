create table if not exists public.user_preferences (
  user_id uuid primary key references public.profiles (id) on delete cascade,
  unit_system text not null default 'IMPERIAL',
  distance_unit text not null default 'YARDS',
  speed_unit text not null default 'MILES_PER_HOUR',
  created_at timestamptz not null default timezone('utc', now()),
  updated_at timestamptz not null default timezone('utc', now()),
  constraint user_preferences_unit_system_check
    check (unit_system in ('IMPERIAL', 'METRIC', 'CUSTOM')),
  constraint user_preferences_distance_unit_check
    check (distance_unit in ('YARDS', 'METERS')),
  constraint user_preferences_speed_unit_check
    check (speed_unit in ('MILES_PER_HOUR', 'KILOMETRES_PER_HOUR', 'METRES_PER_SECOND'))
);

create table if not exists public.practice_units (
  id uuid primary key default gen_random_uuid(),
  owner_id uuid not null default auth.uid() references public.profiles (id) on delete cascade,
  title text not null,
  notes text,
  focus text,
  default_club_reference text,
  tags text[] not null default '{}'::text[],
  default_ball_count integer,
  created_at timestamptz not null default timezone('utc', now()),
  updated_at timestamptz not null default timezone('utc', now()),
  constraint practice_units_title_check
    check (char_length(trim(title)) > 0),
  constraint practice_units_default_ball_count_check
    check (default_ball_count is null or default_ball_count > 0)
);

create table if not exists public.practice_unit_instructions (
  id uuid primary key default gen_random_uuid(),
  practice_unit_id uuid not null references public.practice_units (id) on delete cascade,
  sort_order integer not null,
  text text not null,
  club_reference text,
  rep_count integer,
  ball_count integer,
  created_at timestamptz not null default timezone('utc', now()),
  updated_at timestamptz not null default timezone('utc', now()),
  constraint practice_unit_instructions_sort_order_check
    check (sort_order > 0),
  constraint practice_unit_instructions_text_check
    check (char_length(trim(text)) > 0),
  constraint practice_unit_instructions_rep_count_check
    check (rep_count is null or rep_count > 0),
  constraint practice_unit_instructions_ball_count_check
    check (ball_count is null or ball_count > 0),
  constraint practice_unit_instructions_unique_sort_order
    unique (practice_unit_id, sort_order)
);

create table if not exists public.practice_sessions (
  id uuid primary key default gen_random_uuid(),
  owner_id uuid not null default auth.uid() references public.profiles (id) on delete cascade,
  name text not null,
  notes text,
  created_at timestamptz not null default timezone('utc', now()),
  updated_at timestamptz not null default timezone('utc', now()),
  constraint practice_sessions_name_check
    check (char_length(trim(name)) > 0)
);

create table if not exists public.practice_session_items (
  id uuid primary key default gen_random_uuid(),
  practice_session_id uuid not null references public.practice_sessions (id) on delete cascade,
  practice_unit_id uuid not null references public.practice_units (id) on delete restrict,
  sort_order integer not null,
  notes text,
  focus_cue text,
  rest_seconds integer,
  override_ball_count integer,
  created_at timestamptz not null default timezone('utc', now()),
  updated_at timestamptz not null default timezone('utc', now()),
  constraint practice_session_items_sort_order_check
    check (sort_order > 0),
  constraint practice_session_items_rest_seconds_check
    check (rest_seconds is null or rest_seconds > 0),
  constraint practice_session_items_override_ball_count_check
    check (override_ball_count is null or override_ball_count > 0),
  constraint practice_session_items_unique_sort_order
    unique (practice_session_id, sort_order)
);

create index if not exists practice_units_owner_id_idx
  on public.practice_units (owner_id, updated_at desc);

create index if not exists practice_unit_instructions_unit_id_idx
  on public.practice_unit_instructions (practice_unit_id, sort_order);

create index if not exists practice_sessions_owner_id_idx
  on public.practice_sessions (owner_id, updated_at desc);

create index if not exists practice_session_items_session_id_idx
  on public.practice_session_items (practice_session_id, sort_order);

drop trigger if exists set_user_preferences_updated_at on public.user_preferences;
create trigger set_user_preferences_updated_at
before update on public.user_preferences
for each row
execute function public.set_updated_at();

drop trigger if exists set_practice_units_updated_at on public.practice_units;
create trigger set_practice_units_updated_at
before update on public.practice_units
for each row
execute function public.set_updated_at();

drop trigger if exists set_practice_unit_instructions_updated_at on public.practice_unit_instructions;
create trigger set_practice_unit_instructions_updated_at
before update on public.practice_unit_instructions
for each row
execute function public.set_updated_at();

drop trigger if exists set_practice_sessions_updated_at on public.practice_sessions;
create trigger set_practice_sessions_updated_at
before update on public.practice_sessions
for each row
execute function public.set_updated_at();

drop trigger if exists set_practice_session_items_updated_at on public.practice_session_items;
create trigger set_practice_session_items_updated_at
before update on public.practice_session_items
for each row
execute function public.set_updated_at();

alter table public.user_preferences enable row level security;
alter table public.practice_units enable row level security;
alter table public.practice_unit_instructions enable row level security;
alter table public.practice_sessions enable row level security;
alter table public.practice_session_items enable row level security;

drop policy if exists "Users can manage their own preferences" on public.user_preferences;
create policy "Users can manage their own preferences"
on public.user_preferences
for all
using (auth.uid() = user_id)
with check (auth.uid() = user_id);

drop policy if exists "Users can manage their own practice units" on public.practice_units;
create policy "Users can manage their own practice units"
on public.practice_units
for all
using (auth.uid() = owner_id)
with check (auth.uid() = owner_id);

drop policy if exists "Users can manage instructions on their own practice units" on public.practice_unit_instructions;
create policy "Users can manage instructions on their own practice units"
on public.practice_unit_instructions
for all
using (
  exists (
    select 1
    from public.practice_units
    where practice_units.id = practice_unit_instructions.practice_unit_id
      and practice_units.owner_id = auth.uid()
  )
)
with check (
  exists (
    select 1
    from public.practice_units
    where practice_units.id = practice_unit_instructions.practice_unit_id
      and practice_units.owner_id = auth.uid()
  )
);

drop policy if exists "Users can manage their own practice sessions" on public.practice_sessions;
create policy "Users can manage their own practice sessions"
on public.practice_sessions
for all
using (auth.uid() = owner_id)
with check (auth.uid() = owner_id);

drop policy if exists "Users can manage items on their own practice sessions" on public.practice_session_items;
create policy "Users can manage items on their own practice sessions"
on public.practice_session_items
for all
using (
  exists (
    select 1
    from public.practice_sessions
    where practice_sessions.id = practice_session_items.practice_session_id
      and practice_sessions.owner_id = auth.uid()
  )
)
with check (
  exists (
    select 1
    from public.practice_sessions
    where practice_sessions.id = practice_session_items.practice_session_id
      and practice_sessions.owner_id = auth.uid()
  )
  and exists (
    select 1
    from public.practice_units
    where practice_units.id = practice_session_items.practice_unit_id
      and practice_units.owner_id = auth.uid()
  )
);
