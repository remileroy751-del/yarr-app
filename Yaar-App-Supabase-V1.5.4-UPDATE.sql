-- ============================================================
-- YAAR-APP V1.5.4 — CERTIFICATION GRATUITE 12 MOIS
-- À copier/coller dans Supabase > SQL Editor > Run.
-- Script relançable sans risque (idempotent). Aucune donnée n'est supprimée.
-- Prérequis : les scripts V1.5.1 (IMMO-ADMIN) et V1.5.3 ont déjà été exécutés
-- (fonction is_yaar_admin() et colonne shops.created_at).
--
-- L'ancienneté des boutiques ("A rejoint Yaar-App depuis ...") ne demande AUCUNE
-- modification SQL : elle utilise shops.created_at, déjà présent et immuable.
-- ============================================================

-- ---------- 1. Paramètres de la vague de certification ----------
create table if not exists public.certification_campaign (
    id integer primary key default 1 check (id = 1),
    is_open boolean not null default true,
    free_quota_per_country integer not null default 100 check (free_quota_per_country > 0),
    validity_days integer not null default 365 check (validity_days > 0),
    updated_at timestamptz not null default now()
);
insert into public.certification_campaign (id) values (1) on conflict (id) do nothing;

alter table public.certification_campaign enable row level security;
drop policy if exists certification_campaign_read on public.certification_campaign;
create policy certification_campaign_read on public.certification_campaign
    for select to anon, authenticated using (true);
grant select on public.certification_campaign to anon, authenticated;

-- ---------- 2. Nouvelles colonnes sur shops ----------
-- Date d'activation = date système au moment de la validation par l'administrateur.
alter table public.shops add column if not exists certification_activated_at timestamptz;
-- Rang de création de la boutique dans son pays (1 = première boutique du pays). Jamais modifié.
alter table public.shops add column if not exists founding_rank integer;

-- Compteur par pays : le rang n'est jamais réattribué, même si une boutique est supprimée.
create table if not exists public.shop_rank_counters (
    country text primary key,
    last_rank integer not null default 0
);
alter table public.shop_rank_counters enable row level security;  -- aucune policy = aucun accès direct

-- Rangs des boutiques existantes (ordre de création dans chaque pays)
with base as (
    select country, coalesce(max(founding_rank), 0) as max_rank
      from public.shops group by country
), ranked as (
    select s.id,
           b.max_rank + row_number() over (partition by s.country order by s.created_at, s.id) as rn
      from public.shops s
      join base b on b.country = s.country
     where s.founding_rank is null
)
update public.shops s set founding_rank = ranked.rn
  from ranked where s.id = ranked.id;

insert into public.shop_rank_counters (country, last_rank)
select country, max(founding_rank) from public.shops
 where founding_rank is not null group by country
on conflict (country) do update
   set last_rank = greatest(public.shop_rank_counters.last_rank, excluded.last_rank);

create unique index if not exists shops_country_founding_rank_idx
    on public.shops (country, founding_rank) where founding_rank is not null;

-- Boutiques déjà certifiées avant cette version : 12 mois à compter d'aujourd'hui.
-- (Supprimez ce bloc si vous préférez les laisser sans date d'expiration.)
update public.shops
   set certification_activated_at = now()
 where certification_status = 'CERTIFIED' and certification_activated_at is null;

-- ---------- 3. Garde-fou serveur sur la certification ----------
-- Un client modifié ne peut ni se certifier lui-même, ni changer les dates, ni dépasser le quota.
-- Le client peut seulement passer de NONE/REJECTED à PENDING (candidature) s'il est éligible.
create or replace function public.shops_certification_guard()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
declare
    camp public.certification_campaign%rowtype;
    is_system boolean;
    rank_value integer;
begin
    select * into camp from public.certification_campaign where id = 1;
    if camp.id is null then
        camp.is_open := true; camp.free_quota_per_country := 100; camp.validity_days := 365;
    end if;

    -- "Système" : éditeur SQL / cron (pas de JWT), administrateur, ou fonction interne.
    is_system := auth.uid() is null
              or public.is_yaar_admin()
              or coalesce(current_setting('yaar.system', true), '') = 'on';

    if tg_op = 'INSERT' then
        -- Upsert d'une boutique déjà existante : la branche UPDATE fera le contrôle.
        if exists (select 1 from public.shops s where s.id = new.id) then
            return new;
        end if;

        if new.founding_rank is null or not is_system then
            insert into public.shop_rank_counters (country, last_rank)
            values (new.country, 1)
            on conflict (country) do update set last_rank = public.shop_rank_counters.last_rank + 1
            returning last_rank into rank_value;
            new.founding_rank := rank_value;
        end if;

        if not is_system then
            new.certification_status := 'NONE';
            new.certification_requested_at := null;
            new.certification_paid_at := null;
            new.certification_activated_at := null;
            new.certification_expires_at := null;
            new.id_card_front_storage_path := null;
            new.id_card_back_storage_path := null;
        end if;
        return new;
    end if;

    -- ----- UPDATE -----
    new.founding_rank := old.founding_rank;   -- immuable pour tout le monde

    if is_system then
        -- Date d'expiration calculée automatiquement : activation + validity_days (365 jours).
        if new.certification_status = 'CERTIFIED'
           and new.certification_activated_at is not null
           and (old.certification_status is distinct from 'CERTIFIED'
                or new.certification_activated_at is distinct from old.certification_activated_at) then
            new.certification_expires_at :=
                new.certification_activated_at + make_interval(days => camp.validity_days);
        end if;
        return new;
    end if;

    -- Utilisateur ordinaire : les dates et les pièces déjà envoyées ne se modifient pas par synchro.
    new.certification_activated_at := old.certification_activated_at;
    new.certification_paid_at := old.certification_paid_at;
    new.certification_expires_at := old.certification_expires_at;
    new.certification_requested_at := old.certification_requested_at;
    new.id_card_front_url := old.id_card_front_url;
    new.id_card_back_url := old.id_card_back_url;
    new.id_card_front_storage_path := coalesce(new.id_card_front_storage_path, old.id_card_front_storage_path);
    new.id_card_back_storage_path := coalesce(new.id_card_back_storage_path, old.id_card_back_storage_path);

    if new.certification_status is distinct from old.certification_status then
        if new.certification_status = 'PENDING' and old.certification_status in ('NONE', 'REJECTED') then
            if not camp.is_open then
                raise exception 'CERTIFICATION_NOT_ELIGIBLE: la vague de certification gratuite est terminée.';
            end if;
            if old.founding_rank is null or old.founding_rank > camp.free_quota_per_country then
                raise exception 'CERTIFICATION_NOT_ELIGIBLE: offre réservée aux % premières boutiques de chaque pays.', camp.free_quota_per_country;
            end if;
            if old.certification_activated_at is not null then
                raise exception 'CERTIFICATION_NOT_ELIGIBLE: la certification gratuite a déjà été utilisée par cette boutique.';
            end if;
            if new.id_card_front_storage_path is null or new.id_card_back_storage_path is null then
                raise exception 'CERTIFICATION_NOT_ELIGIBLE: pièce d''identité recto et verso obligatoires.';
            end if;
            new.certification_requested_at := now();
        else
            -- Toute autre transition (ex. se mettre CERTIFIED) est ignorée.
            new.certification_status := old.certification_status;
        end if;
    end if;

    return new;
end;
$$;

drop trigger if exists shops_certification_guard on public.shops;
create trigger shops_certification_guard
before insert or update on public.shops
for each row execute function public.shops_certification_guard();

-- ---------- 4. Actions administrateur ----------
-- Validation : statut CERTIFIED, date d'activation = date système maintenant,
-- date d'expiration = activation + 365 jours (calculée par le trigger ci-dessus).
create or replace function public.admin_validate_shop_certification(p_shop_id uuid)
returns timestamptz
language plpgsql
security definer
set search_path = public
as $$
declare
    s public.shops%rowtype;
    camp public.certification_campaign%rowtype;
begin
    if not public.is_yaar_admin() then
        raise exception 'not_authorized';
    end if;
    perform set_config('yaar.system', 'on', true);

    select * into camp from public.certification_campaign where id = 1;
    select * into s from public.shops where id = p_shop_id for update;
    if not found then
        raise exception 'Boutique introuvable.';
    end if;
    if s.certification_status <> 'PENDING' then
        raise exception 'Cette boutique n''a pas de demande de certification en attente.';
    end if;
    if s.id_card_front_storage_path is null or s.id_card_back_storage_path is null then
        raise exception 'Pièce d''identité recto/verso manquante : validation impossible.';
    end if;

    update public.shops
       set certification_status = 'CERTIFIED',
           certification_activated_at = now(),
           certification_expires_at = now() + make_interval(days => coalesce(camp.validity_days, 365))
     where id = p_shop_id;

    return now() + make_interval(days => coalesce(camp.validity_days, 365));
end;
$$;
revoke all on function public.admin_validate_shop_certification(uuid) from public;
grant execute on function public.admin_validate_shop_certification(uuid) to authenticated;

create or replace function public.admin_reject_shop_certification(p_shop_id uuid)
returns void
language plpgsql
security definer
set search_path = public
as $$
begin
    if not public.is_yaar_admin() then
        raise exception 'not_authorized';
    end if;
    perform set_config('yaar.system', 'on', true);
    update public.shops
       set certification_status = 'REJECTED'
     where id = p_shop_id and certification_status = 'PENDING';
end;
$$;
revoke all on function public.admin_reject_shop_certification(uuid) from public;
grant execute on function public.admin_reject_shop_certification(uuid) to authenticated;

-- Ancien bouton "Vérifier / Retirer badge" : conservé, mais respecte désormais les 12 mois.
create or replace function public.admin_set_shop_certified(p_shop_id uuid, p_certified boolean)
returns void
language plpgsql
security definer
set search_path = public
as $$
declare
    camp public.certification_campaign%rowtype;
begin
    if not public.is_yaar_admin() then
        raise exception 'not_authorized';
    end if;
    perform set_config('yaar.system', 'on', true);
    select * into camp from public.certification_campaign where id = 1;

    if p_certified then
        update public.shops
           set certification_status = 'CERTIFIED',
               certification_activated_at = now(),
               certification_expires_at = now() + make_interval(days => coalesce(camp.validity_days, 365))
         where id = p_shop_id;
    else
        update public.shops
           set certification_status = 'NONE',
               certification_activated_at = null,
               certification_expires_at = null
         where id = p_shop_id;
    end if;
end;
$$;
revoke all on function public.admin_set_shop_certified(uuid, boolean) from public;
grant execute on function public.admin_set_shop_certified(uuid, boolean) to authenticated;

-- ---------- 5. Expiration automatique après 365 jours ----------
create or replace function public.expire_shop_certifications()
returns integer
language plpgsql
security definer
set search_path = public
as $$
declare
    n integer;
begin
    perform set_config('yaar.system', 'on', true);
    update public.shops
       set certification_status = 'EXPIRED'
     where certification_status = 'CERTIFIED'
       and certification_expires_at is not null
       and certification_expires_at <= now();
    get diagnostics n = row_count;
    return n;
end;
$$;
revoke all on function public.expire_shop_certifications() from public;
grant execute on function public.expire_shop_certifications() to authenticated;

-- Planification automatique toutes les 30 minutes (pg_cron).
-- Si pg_cron n'est pas disponible, le script continue : l'application et l'interface admin
-- appliquent de toute façon la date d'expiration à l'affichage.
do $$
begin
    create extension if not exists pg_cron;
    perform cron.schedule('yaar-expire-certifications', '*/30 * * * *',
                          'select public.expire_shop_certifications();');
exception when others then
    raise notice 'pg_cron indisponible (%). Activez-le dans Database > Extensions puis relancez ce script.', sqlerrm;
end;
$$;

-- ---------- 6. L'administrateur peut consulter les pièces d'identité ----------
drop policy if exists id_cards_storage_select_admin on storage.objects;
create policy id_cards_storage_select_admin on storage.objects
    for select to authenticated
    using (bucket_id = 'id_cards' and public.is_yaar_admin());

-- ---------- 7. Application immédiate ----------
select public.expire_shop_certifications();
notify pgrst, 'reload schema';

-- Fin.
-- Pour clôturer la vague plus tard :  update public.certification_campaign set is_open = false where id = 1;
-- Pour changer le quota :             update public.certification_campaign set free_quota_per_country = 150 where id = 1;
