-- ============================================================
-- YAAR-APP V1.6.0 — ABONNEMENT PAR CLÉ D'ACTIVATION (30 JOURS)
-- À copier/coller dans Supabase > SQL Editor > Run.
-- À exécuter APRÈS les scripts V1.5.1 → V1.5.5. Relançable sans risque (idempotent).
--
-- Règles appliquées côté SERVEUR (un client modifié ne peut pas les contourner) :
--   MODE GRATUIT : 5 produits actifs, 5 annonces immobilières actives,
--                  ville de la boutique + 5 villes maximum par produit.
--   MODE ACTIVÉ  : 25 produits actifs, 20 annonces immobilières actives,
--                  toutes les villes du pays, badge « certifié ».
--   Une clé (format « ABCD 1234 ») = 30 jours. Les jours se cumulent si une
--   clé est saisie alors qu'un abonnement est déjà en cours.
--   À l'expiration, la boutique revient automatiquement au mode gratuit.
-- ============================================================

-- ---------- 1. Fin de la vague de certification gratuite ----------
-- Plus de dates limites, plus de quota, plus de blocage de l'application.
update public.certification_campaign set is_open = false, updated_at = now() where id = 1;

-- ---------- 2. Table des clés d'activation ----------
create table if not exists public.activation_keys (
    key_code      text primary key check (key_code ~ '^[A-Z]{4} [0-9]{4}$'),
    duration_days integer not null default 30 check (duration_days > 0),
    status        text not null default 'AVAILABLE' check (status in ('AVAILABLE', 'USED', 'REVOKED')),
    batch_label   text,
    created_at    timestamptz not null default now(),
    used_at       timestamptz,
    used_by_shop  uuid references public.shops(id) on delete set null,
    used_by_uid   uuid
);
create index if not exists activation_keys_status_idx on public.activation_keys (status, created_at desc);

alter table public.activation_keys enable row level security;
-- Aucune lecture/écriture directe pour les utilisateurs : seul l'administrateur peut LIRE la table.
drop policy if exists activation_keys_select_admin on public.activation_keys;
create policy activation_keys_select_admin on public.activation_keys
    for select to authenticated using (public.is_yaar_admin());
revoke all on public.activation_keys from anon, authenticated;
grant select on public.activation_keys to authenticated;

-- Tentatives ratées (protection contre les essais au hasard).
create table if not exists public.activation_attempts (
    id           bigint generated always as identity primary key,
    uid          uuid not null,
    attempted_at timestamptz not null default now()
);
create index if not exists activation_attempts_uid_idx on public.activation_attempts (uid, attempted_at desc);
alter table public.activation_attempts enable row level security;   -- aucune policy = aucun accès direct
revoke all on public.activation_attempts from anon, authenticated;

-- ---------- 3. Fonctions utilitaires ----------
-- Boutique « activée » = certifiée ET date d'expiration non dépassée (heure du serveur).
create or replace function public.shop_is_premium(p_shop_id uuid)
returns boolean
language sql
stable
security definer
set search_path = public
as $$
    select coalesce((
        select s.certification_status = 'CERTIFIED'
               and s.certification_expires_at is not null
               and s.certification_expires_at > now()
          from public.shops s
         where s.id = p_shop_id
    ), false);
$$;
revoke all on function public.shop_is_premium(uuid) from public;
grant execute on function public.shop_is_premium(uuid) to authenticated;

-- Normalise une clé saisie : « jutg5217 », « JUTG  5217 » → « JUTG 5217 ». Retourne null si invalide.
create or replace function public.normalize_activation_key(p_raw text)
returns text
language plpgsql
immutable
as $$
declare
    compact text := upper(regexp_replace(coalesce(p_raw, ''), '[[:space:]]+', '', 'g'));
begin
    if compact ~ '^[A-Z]{4}[0-9]{4}$' then
        return substr(compact, 1, 4) || ' ' || substr(compact, 5, 4);
    end if;
    return null;
end;
$$;

-- ---------- 4. Garde-fou sur la boutique (remplace la version V1.5.5) ----------
-- Un utilisateur ordinaire ne peut JAMAIS modifier lui-même le statut de certification, les dates
-- ou les capacités. Seuls l'éditeur SQL, l'administrateur ou une fonction interne le peuvent.
create or replace function public.shops_certification_guard()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
declare
    is_system boolean;
    rank_value integer;
begin
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
            new.extra_product_slots := 0;
            new.extra_immo_slots := 0;
        end if;
        return new;
    end if;

    -- ----- UPDATE -----
    new.founding_rank := old.founding_rank;   -- immuable pour tout le monde
    if is_system then
        return new;
    end if;

    new.certification_status := old.certification_status;
    new.certification_activated_at := old.certification_activated_at;
    new.certification_expires_at := old.certification_expires_at;
    new.certification_paid_at := old.certification_paid_at;
    new.certification_requested_at := old.certification_requested_at;
    new.id_card_front_url := old.id_card_front_url;
    new.id_card_back_url := old.id_card_back_url;
    new.id_card_front_storage_path := old.id_card_front_storage_path;
    new.id_card_back_storage_path := old.id_card_back_storage_path;
    new.extra_product_slots := old.extra_product_slots;
    new.extra_immo_slots := old.extra_immo_slots;
    return new;
end;
$$;

drop trigger if exists shops_certification_guard on public.shops;
create trigger shops_certification_guard
before insert or update on public.shops
for each row execute function public.shops_certification_guard();

-- ---------- 5. Limites de publication appliquées par le serveur ----------
-- Remplace l'ancienne limite immobilière (5 + extra_immo_slots).
drop trigger if exists products_immo_limit on public.products;
drop function if exists public.enforce_immo_listing_limit();

create or replace function public.enforce_publication_limits()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
declare
    existing_active boolean;
    existing_exists boolean := false;
    premium boolean;
    is_immo boolean;
    allowed_count integer;
    current_count integer;
begin
    if tg_op = 'UPDATE' then
        existing_exists := true;
        existing_active := old.is_active;
    else
        -- Upsert d'une ligne déjà présente : le BEFORE INSERT se déclenche quand même.
        select p.is_active into existing_active from public.products p where p.id = new.id;
        existing_exists := found;
    end if;

    -- Seules les NOUVELLES publications et les réactivations sont contrôlées.
    -- Un produit déjà actif qu'on synchronise ou qu'on met à jour n'est jamais bloqué.
    if new.is_active is not true then
        return new;
    end if;
    if existing_exists and coalesce(existing_active, false) then
        if tg_op = 'INSERT' then
            return new;
        end if;
        if new.listing_type is not distinct from old.listing_type
           and new.shop_id is not distinct from old.shop_id then
            return new;
        end if;
    end if;

    premium := public.shop_is_premium(new.shop_id);
    is_immo := new.listing_type in ('IMMO_SALE', 'IMMO_RENT');

    if is_immo then
        allowed_count := case when premium then 20 else 5 end;
    else
        allowed_count := case when premium then 25 else 5 end;
    end if;

    select count(*) into current_count
      from public.products p
     where p.shop_id = new.shop_id
       and p.is_active = true
       and p.id is distinct from new.id
       and ((p.listing_type in ('IMMO_SALE', 'IMMO_RENT')) = is_immo);

    if current_count >= allowed_count then
        if is_immo then
            raise exception 'LICENSE_LIMIT_IMMO: limite de % annonces immobilières actives atteinte.', allowed_count;
        else
            raise exception 'LICENSE_LIMIT_PRODUCTS: limite de % produits actifs atteinte.', allowed_count;
        end if;
    end if;

    -- Mode gratuit : ville de la boutique (en premier) + 5 villes maximum.
    if not premium and new.available_cities is not null and cardinality(new.available_cities) > 6 then
        new.available_cities := new.available_cities[1:6];
    end if;

    return new;
end;
$$;

drop trigger if exists products_publication_limits on public.products;
create trigger products_publication_limits
before insert or update of shop_id, listing_type, is_active
on public.products
for each row execute function public.enforce_publication_limits();

-- ---------- 6. Saisie d'une clé par l'utilisateur ----------
-- Retourne toujours un JSON {ok, code, message, expires_at, activated_at, days_added}
-- (pas d'exception pour une clé fausse, afin que la tentative ratée soit bien enregistrée).
create or replace function public.redeem_activation_key(p_code text)
returns jsonb
language plpgsql
security definer
set search_path = public
as $$
declare
    v_uid uuid := auth.uid();
    normalized text;
    failed_recent integer;
    shop_row public.shops%rowtype;
    key_row public.activation_keys%rowtype;
    base_time timestamptz;
    new_expiry timestamptz;
    new_activated timestamptz;
    still_active boolean;
begin
    if v_uid is null then
        return jsonb_build_object('ok', false, 'code', 'NOT_AUTHENTICATED', 'message', 'Vous devez être connecté.');
    end if;

    -- Protection : 8 clés invalides maximum toutes les 30 minutes.
    delete from public.activation_attempts where attempted_at < now() - interval '2 days';
    select count(*) into failed_recent
      from public.activation_attempts
     where activation_attempts.uid = v_uid
       and attempted_at > now() - interval '30 minutes';
    if failed_recent >= 8 then
        return jsonb_build_object('ok', false, 'code', 'TOO_MANY_ATTEMPTS',
            'message', 'Trop de tentatives incorrectes. Réessayez dans 30 minutes.');
    end if;

    normalized := public.normalize_activation_key(p_code);
    if normalized is null then
        insert into public.activation_attempts (uid) values (v_uid);
        return jsonb_build_object('ok', false, 'code', 'INVALID_FORMAT',
            'message', 'Format invalide. Une clé ressemble à : JUTG 5217 (4 lettres, un espace, 4 chiffres).');
    end if;

    select * into shop_row from public.shops where owner_uid = v_uid for update;
    if not found then
        return jsonb_build_object('ok', false, 'code', 'NO_SHOP',
            'message', 'Créez d''abord votre boutique avant d''activer une clé.');
    end if;

    select * into key_row from public.activation_keys where key_code = normalized for update;
    if not found then
        insert into public.activation_attempts (uid) values (v_uid);
        return jsonb_build_object('ok', false, 'code', 'KEY_NOT_FOUND', 'message', 'Clé d''activation introuvable.');
    end if;
    if key_row.status <> 'AVAILABLE' then
        insert into public.activation_attempts (uid) values (v_uid);
        return jsonb_build_object('ok', false, 'code', 'KEY_ALREADY_USED',
            'message', 'Cette clé a déjà été utilisée ou n''est plus valide.');
    end if;

    -- Cumul des jours : si l'abonnement est encore en cours, on ajoute à partir de sa date de fin.
    still_active := shop_row.certification_status = 'CERTIFIED'
                    and shop_row.certification_expires_at is not null
                    and shop_row.certification_expires_at > now();
    base_time := case when still_active then shop_row.certification_expires_at else now() end;
    new_expiry := base_time + make_interval(days => key_row.duration_days);
    new_activated := case when still_active then coalesce(shop_row.certification_activated_at, now()) else now() end;

    update public.activation_keys
       set status = 'USED', used_at = now(), used_by_shop = shop_row.id, used_by_uid = v_uid
     where key_code = normalized;

    perform set_config('yaar.system', 'on', true);
    update public.shops
       set certification_status = 'CERTIFIED',
           certification_activated_at = new_activated,
           certification_expires_at = new_expiry
     where id = shop_row.id;

    return jsonb_build_object(
        'ok', true, 'code', 'OK',
        'message', 'Clé activée avec succès.',
        'expires_at', new_expiry,
        'activated_at', new_activated,
        'days_added', key_row.duration_days
    );
end;
$$;
revoke all on function public.redeem_activation_key(text) from public;
grant execute on function public.redeem_activation_key(text) to authenticated;

-- ---------- 7. Administration : import des clés depuis un fichier .txt ----------
create or replace function public.admin_import_activation_keys(p_keys text[], p_batch text default null)
returns jsonb
language plpgsql
security definer
set search_path = public
as $$
declare
    k text;
    normalized text;
    n_inserted integer := 0;
    n_duplicates integer := 0;
    n_invalid integer := 0;
    n_rows integer;
begin
    if not public.is_yaar_admin() then
        raise exception 'not_authorized';
    end if;
    if p_keys is null or array_length(p_keys, 1) is null then
        return jsonb_build_object('inserted', 0, 'duplicates', 0, 'invalid', 0);
    end if;
    if array_length(p_keys, 1) > 5000 then
        raise exception 'Maximum 5000 clés par envoi.';
    end if;

    foreach k in array p_keys loop
        normalized := public.normalize_activation_key(k);
        if normalized is null then
            n_invalid := n_invalid + 1;
            continue;
        end if;
        insert into public.activation_keys (key_code, batch_label)
        values (normalized, nullif(trim(coalesce(p_batch, '')), ''))
        on conflict (key_code) do nothing;
        get diagnostics n_rows = row_count;
        if n_rows = 1 then n_inserted := n_inserted + 1; else n_duplicates := n_duplicates + 1; end if;
    end loop;

    return jsonb_build_object('inserted', n_inserted, 'duplicates', n_duplicates, 'invalid', n_invalid);
end;
$$;
revoke all on function public.admin_import_activation_keys(text[], text) from public;
grant execute on function public.admin_import_activation_keys(text[], text) to authenticated;

-- Invalide une clé encore disponible (ex. remboursement, clé compromise).
create or replace function public.admin_revoke_activation_key(p_key text)
returns boolean
language plpgsql
security definer
set search_path = public
as $$
declare
    n integer;
begin
    if not public.is_yaar_admin() then
        raise exception 'not_authorized';
    end if;
    update public.activation_keys
       set status = 'REVOKED'
     where key_code = public.normalize_activation_key(p_key) and status = 'AVAILABLE';
    get diagnostics n = row_count;
    return n = 1;
end;
$$;
revoke all on function public.admin_revoke_activation_key(text) from public;
grant execute on function public.admin_revoke_activation_key(text) to authenticated;

-- ---------- 8. Actions manuelles de l'administrateur sur un abonnement ----------
-- Ancien bouton « Vérifier / Retirer badge » : offre 30 jours (cumulés) ou retire l'abonnement.
create or replace function public.admin_set_shop_certified(p_shop_id uuid, p_certified boolean)
returns void
language plpgsql
security definer
set search_path = public
as $$
declare
    s public.shops%rowtype;
    active_now boolean;
    base_time timestamptz;
begin
    if not public.is_yaar_admin() then
        raise exception 'not_authorized';
    end if;
    perform set_config('yaar.system', 'on', true);

    if p_certified then
        select * into s from public.shops where id = p_shop_id for update;
        if not found then raise exception 'Boutique introuvable.'; end if;
        active_now := s.certification_status = 'CERTIFIED' and s.certification_expires_at > now();
        base_time := case when active_now then s.certification_expires_at else now() end;
        update public.shops
           set certification_status = 'CERTIFIED',
               certification_activated_at = case when active_now then coalesce(certification_activated_at, now()) else now() end,
               certification_expires_at = base_time + interval '30 days'
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

-- ---------- 9. Expiration automatique (retour au mode gratuit) ----------
-- Inchangée : passe en EXPIRED toute boutique dont la date de fin est dépassée.
-- (Les limites ci-dessus utilisent de toute façon la date de fin, sans attendre le cron.)
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

do $$
begin
    create extension if not exists pg_cron;
    perform cron.schedule('yaar-expire-certifications', '*/30 * * * *',
                          'select public.expire_shop_certifications();');
exception when others then
    raise notice 'pg_cron indisponible (%). Les limites restent appliquées via la date de fin.', sqlerrm;
end;
$$;

select public.expire_shop_certifications();
notify pgrst, 'reload schema';

-- ============================================================
-- OPTIONNEL — boutiques certifiées gratuitement pendant la vague V1.5.4/V1.5.5 :
-- elles gardent leur certification jusqu'à leur date d'expiration d'origine (365 jours).
-- Pour les remettre en mode gratuit immédiatement, décommentez :
--
--   select set_config('yaar.system', 'on', false);
--   update public.shops
--      set certification_status = 'NONE', certification_activated_at = null, certification_expires_at = null
--    where certification_status = 'CERTIFIED';
-- ============================================================
-- Fin.
