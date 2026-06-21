alter table public.profiles
  add column if not exists first_name text,
  add column if not exists last_name text;

create or replace function public.sync_profile_from_auth_user()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
begin
  insert into public.profiles (
    id,
    email,
    display_name,
    first_name,
    last_name,
    avatar_url
  )
  values (
    new.id,
    new.email,
    coalesce(
      new.raw_user_meta_data ->> 'full_name',
      new.raw_user_meta_data ->> 'name',
      case
        when new.email is null then null
        else split_part(new.email, '@', 1)
      end
    ),
    coalesce(
      new.raw_user_meta_data ->> 'given_name',
      split_part(
        coalesce(
          new.raw_user_meta_data ->> 'full_name',
          new.raw_user_meta_data ->> 'name',
          split_part(new.email, '@', 1)
        ),
        ' ', 1
      )
    ),
    coalesce(
      new.raw_user_meta_data ->> 'family_name',
      nullif(
        trim(
          substring(
            coalesce(
              new.raw_user_meta_data ->> 'full_name',
              new.raw_user_meta_data ->> 'name',
              ''
            )
            from position(' ' in coalesce(
              new.raw_user_meta_data ->> 'full_name',
              new.raw_user_meta_data ->> 'name',
              ''
            )) + 1
          )
        ),
        ''
      )
    ),
    new.raw_user_meta_data ->> 'avatar_url'
  )
  on conflict (id) do update
    set email        = excluded.email,
        display_name = coalesce(excluded.display_name, public.profiles.display_name),
        first_name   = coalesce(excluded.first_name,   public.profiles.first_name),
        last_name    = coalesce(excluded.last_name,    public.profiles.last_name),
        avatar_url   = coalesce(excluded.avatar_url,   public.profiles.avatar_url),
        updated_at   = timezone('utc', now());

  return new;
end;
$$;

-- Grant table-level access; the profiles table was missing this despite having RLS policies
grant select, update
on table public.profiles
to authenticated;

-- Backfill first_name/last_name for existing rows from display_name
update public.profiles
set
  first_name = case
    when first_name is not null then first_name
    when display_name is not null then split_part(display_name, ' ', 1)
    else null
  end,
  last_name = case
    when last_name is not null then last_name
    when display_name is not null and position(' ' in display_name) > 0
      then trim(substring(display_name from position(' ' in display_name) + 1))
    else null
  end
where first_name is null or last_name is null;
