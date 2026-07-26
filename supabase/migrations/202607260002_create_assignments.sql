create table if not exists public.assignments (
    id uuid primary key,
    user_id uuid not null references auth.users(id) on delete cascade,
    title text not null check (char_length(trim(title)) between 1 and 200),
    course text not null default '',
    deadline_date date not null,
    deadline_time time,
    priority text not null default 'Medium'
        check (priority in ('Low', 'Medium', 'High')),
    is_completed boolean not null default false,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create index if not exists assignments_user_deadline_idx
on public.assignments (user_id, is_completed, deadline_date, deadline_time);

alter table public.assignments enable row level security;

drop policy if exists "Users can read their own assignments" on public.assignments;
create policy "Users can read their own assignments"
on public.assignments for select
to authenticated
using ((select auth.uid()) = user_id);

drop policy if exists "Users can create their own assignments" on public.assignments;
create policy "Users can create their own assignments"
on public.assignments for insert
to authenticated
with check ((select auth.uid()) = user_id);

drop policy if exists "Users can update their own assignments" on public.assignments;
create policy "Users can update their own assignments"
on public.assignments for update
to authenticated
using ((select auth.uid()) = user_id)
with check ((select auth.uid()) = user_id);

drop policy if exists "Users can delete their own assignments" on public.assignments;
create policy "Users can delete their own assignments"
on public.assignments for delete
to authenticated
using ((select auth.uid()) = user_id);

create or replace function public.set_assignment_updated_at()
returns trigger
language plpgsql
security invoker
set search_path = ''
as $$
begin
    new.updated_at = now();
    return new;
end;
$$;

drop trigger if exists set_assignments_updated_at on public.assignments;
create trigger set_assignments_updated_at
before update on public.assignments
for each row execute function public.set_assignment_updated_at();
