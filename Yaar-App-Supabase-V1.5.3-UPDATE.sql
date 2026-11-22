-- YAAR-APP V1.5.3 - À copier/coller dans Supabase > SQL Editor > Run.
-- Script relançable sans risque (idempotent).

-- 1) Notifications "Je suis intéressé" : supprime les éventuels doublons côté serveur
--    (on garde la plus ancienne notification par produit + acheteur + propriétaire).
delete from public.interests i
using public.interests older
where i.product_id = older.product_id
  and i.buyer_id = older.buyer_id
  and i.shop_owner_id = older.shop_owner_id
  and (i.created_at, i.id) > (older.created_at, older.id);

-- 2) Colonnes de suivi des modifications nom/logo de boutique
alter table public.shops add column if not exists name_changed_at timestamptz;
alter table public.shops add column if not exists logo_changed_at timestamptz;

-- 3) La date de création de la boutique est immuable (base de l'ancienneté)
create or replace function public.preserve_shop_created_at()
returns trigger language plpgsql security definer set search_path = public as $$
begin
    new.created_at = old.created_at;
    return new;
end;
$$;
drop trigger if exists shops_preserve_created_at on public.shops;
create trigger shops_preserve_created_at before update on public.shops
for each row execute function public.preserve_shop_created_at();

-- 4) Nom et logo : une seule modification tous les 30 jours (délai partagé)
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
create trigger shops_name_change_cooldown before update on public.shops
for each row execute function public.enforce_shop_name_change_cooldown();

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
create trigger shops_logo_change_cooldown before update on public.shops
for each row execute function public.enforce_shop_logo_change_cooldown();

-- 5) Boutiques certifiées : aucune contrainte à ajouter, la colonne products.available_cities
--    (text[]) accepte déjà autant de villes que nécessaire.
