package com.yaarapp.app.data

/**
 * Limites de publication d'une boutique.
 *
 * MODE GRATUIT : 5 produits actifs, 5 annonces immobilières actives, ville de la boutique + 5 villes.
 * ABONNEMENT ACTIVÉ (clé d'activation, 30 jours) : 25 produits, 20 annonces immobilières,
 * toutes les villes du pays et badge « certifié ».
 *
 * Ces limites sont aussi appliquées par le serveur Supabase (V1.6.0) : l'application ne fait
 * que les afficher et éviter des envois inutiles.
 */
object ShopLimits {
    const val FREE_PRODUCTS = 5
    const val FREE_IMMO_LISTINGS = 5
    /** Villes supplémentaires autorisées en plus de la ville de la boutique (mode gratuit). */
    const val FREE_EXTRA_CITIES = 5

    const val PREMIUM_PRODUCTS = 25
    const val PREMIUM_IMMO_LISTINGS = 20
}

/** Nombre maximum de produits actifs autorisés pour cette boutique à cet instant. */
val Shop.maxProducts: Int
    get() = if (isCertifiedNow()) ShopLimits.PREMIUM_PRODUCTS else ShopLimits.FREE_PRODUCTS

/** Nombre maximum d'annonces immobilières actives autorisées à cet instant. */
val Shop.maxImmoListings: Int
    get() = if (isCertifiedNow()) ShopLimits.PREMIUM_IMMO_LISTINGS else ShopLimits.FREE_IMMO_LISTINGS

/**
 * Tarification de la mise en avant payante d'un produit ("Promouvoir mes produits").
 *
 * Le vendeur choisit :
 * - un nombre d'expositions (le produit sera montré à ce nombre d'ouvertures de
 *   l'application par des acheteurs, toutes boutiques confondues) — entre 50 et 1000 ;
 * - une durée de campagne en jours — entre 10 et 30 — sur laquelle ces expositions
 *   seront réparties.
 *
 * Le prix ne dépend QUE du nombre d'expositions : 20 FCFA/exposition (donc 100
 * expositions = 2 000 FCFA, exemple donné). La durée fixe seulement le rythme de
 * diffusion et la date de fin de campagne.
 */
object AdPricing {
    const val PRICE_PER_EXPOSITION_FCFA = 20
    const val MIN_EXPOSITIONS = 50
    const val MAX_EXPOSITIONS = 1000
    const val EXPOSITION_STEP = 50

    const val MIN_DAYS = 10
    const val MAX_DAYS = 30

    fun priceFor(expositions: Int): Int = expositions * PRICE_PER_EXPOSITION_FCFA

    /** Rythme de diffusion indicatif affiché au vendeur : expositions par jour, arrondi au supérieur. */
    fun expositionsPerDay(expositions: Int, days: Int): Int =
        if (days <= 0) expositions else ((expositions + days - 1) / days)
}
