alter table public.profiles
add column if not exists age_range text not null default '',
add column if not exists gender text not null default '',
add column if not exists role text not null default '',
add column if not exists other_role text not null default '',
add column if not exists managing text[] not null default '{}';
