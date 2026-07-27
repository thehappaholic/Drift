alter table public.profiles
add column if not exists wind_down_enabled boolean not null default true;
