-- ============================================================
-- YAAR-APP V1.5.5 — PÉRIODE DE LA VAGUE : 15 OCTOBRE → 15 NOVEMBRE 2026
-- À copier/coller dans Supabase > SQL Editor > Run.
-- À exécuter APRÈS Yaar-App-Supabase-V1.5.4-UPDATE.sql. Relançable sans risque.
--
-- Les candidatures de certification gratuite ne sont acceptées que du 15/10/2026 00:00 UTC
-- au 15/11/2026 23:59 UTC (fin exclusive : 16/11/2026 00:00 UTC). Le serveur refuse toute
-- candidature hors période, même avec une application modifiée.
-- À partir du 16/11/2026 00:00 UTC, l'application Android V1.5.5 se bloque et renvoie vers
-- https://yaar-appv133.netlify.app/ (heure vérifiée auprès du serveur grâce à yaar_server_time()).
-- Heure UTC = heure du Togo ; au Bénin (UTC+1) le blocage intervient à 01h00.
-- ============================================================

alter table public.certification_campaign add column if not exists starts_at timestamptz not null default timestamptz '2026-10-15 00:00:00+00';
alter table public.certification_campaign add column if not exists ends_at   timestamptz not null default timestamptz '2026-11-16 00:00:00+00';

update public.certification_campaign
   set starts_at = timestamptz '2026-10-15 00:00:00+00',
       ends_at   = timestamptz '2026-11-16 00:00:00+00',
       updated_at = now()
 where id = 1;

-- Heure officielle du serveur (utilisée par l'application pour le blocage du 16 novembre).
create or replace function public.yaar_server_time()
returns timestamptz
language sql
stable
as $$ select now(); $$;
grant execute on function public.yaar_server_time() to anon, authenticated;

-- Garde-fou serveur mis à jour : contrôle de la période en plus du quota.
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
        camp.starts_at := timestamptz '2026-10-15 00:00:00+00'; camp.ends_at := timestamptz '2026-11-16 00:00:00+00';
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
            if now() < camp.starts_at or now() >= camp.ends_at then
                raise exception 'CERTIFICATION_PERIOD: les candidatures sont ouvertes du % au % uniquement.',
                    to_char(camp.starts_at at time zone 'UTC', 'DD/MM/YYYY'),
                    to_char((camp.ends_at - interval '1 day') at time zone 'UTC', 'DD/MM/YYYY');
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

notify pgrst, 'reload schema';

-- Fin.
-- Prolonger la période plus tard :  update public.certification_campaign set ends_at = timestamptz '2026-12-01 00:00:00+00' where id = 1;
