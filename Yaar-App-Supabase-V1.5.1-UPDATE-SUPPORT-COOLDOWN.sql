-- YAAR-APP — Mise à jour V1.5.2
-- À exécuter une seule fois dans Supabase SQL Editor. Relançable.

-- 1) Colonnes de suivi des modifications nom/logo
alter table public.shops add column if not exists name_changed_at timestamptz;
alter table public.shops add column if not exists logo_changed_at timestamptz;

-- 2) La date de création est immuable : les mises à jour de la boutique ne doivent
-- jamais remplacer created_at par une date récente envoyée par l'application.
create or replace function public.preserve_shop_created_at()
returns trigger language plpgsql security definer set search_path = public as $$
begin
    new.created_at = old.created_at;
    return new;
end;
$$;
drop trigger if exists shops_preserve_created_at on public.shops;
create trigger shops_preserve_created_at before update on public.shops for each row execute function public.preserve_shop_created_at();

-- 2) Nom et logo partagent un cooldown de 30 jours. La première modification est libre.
create or replace function public.enforce_shop_name_change_cooldown()
returns trigger language plpgsql security definer set search_path = public as $$
begin
    if new.name is distinct from old.name then
        if greatest(coalesce(old.name_changed_at, to_timestamp(0)), coalesce(old.logo_changed_at, to_timestamp(0))) > now() - interval '30 days' then
            raise exception 'Le nom ou le logo de cette boutique a été modifié récemment. Une nouvelle modification sera possible après 30 jours.';
        end if;
        new.name_changed_at = now();
    else
        new.name_changed_at = old.name_changed_at;
    end if;
    return new;
end;
$$;
drop trigger if exists shops_name_change_cooldown on public.shops;
create trigger shops_name_change_cooldown before update on public.shops for each row execute function public.enforce_shop_name_change_cooldown();

create or replace function public.enforce_shop_logo_change_cooldown()
returns trigger language plpgsql security definer set search_path = public as $$
begin
    if new.logo_url is distinct from old.logo_url then
        if greatest(coalesce(old.name_changed_at, to_timestamp(0)), coalesce(old.logo_changed_at, to_timestamp(0))) > now() - interval '30 days' then
            raise exception 'Le nom ou le logo de cette boutique a été modifié récemment. Une nouvelle modification sera possible après 30 jours.';
        end if;
        new.logo_changed_at = now();
    else
        new.logo_changed_at = old.logo_changed_at;
    end if;
    return new;
end;
$$;
drop trigger if exists shops_logo_change_cooldown on public.shops;
create trigger shops_logo_change_cooldown before update on public.shops for each row execute function public.enforce_shop_logo_change_cooldown();

-- 3) Messagerie utilisateur → Support Yaar-App (80 caractères maximum côté base)
create table if not exists public.support_messages (
    id uuid primary key default gen_random_uuid(),
    user_uid uuid not null references auth.users(id) on delete cascade,
    message text not null check (char_length(trim(message)) between 1 and 80),
    status text not null default 'NEW' check (status in ('NEW','READ','REPLIED')),
    admin_reply text,
    created_at timestamptz not null default now(),
    replied_at timestamptz
);
create index if not exists support_messages_user_idx on public.support_messages(user_uid, created_at desc);
alter table public.support_messages enable row level security;

drop policy if exists support_messages_insert_own on public.support_messages;
create policy support_messages_insert_own on public.support_messages for insert to authenticated with check (user_uid = auth.uid());

drop policy if exists support_messages_select_own on public.support_messages;
create policy support_messages_select_own on public.support_messages for select to authenticated using (user_uid = auth.uid() or public.is_yaar_admin());

drop policy if exists support_messages_update_admin on public.support_messages;
create policy support_messages_update_admin on public.support_messages for update to authenticated using (public.is_yaar_admin()) with check (public.is_yaar_admin());

grant select, insert, update on public.support_messages to authenticated;

-- 4) Les fonctions/policies admin existantes de V1.5.1 restent inchangées.
-- Assurez-vous que yaar_admins et is_yaar_admin existent déjà.
