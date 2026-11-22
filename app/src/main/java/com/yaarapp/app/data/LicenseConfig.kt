package com.yaarapp.app.data

/**
 * Abonnement par CLÉ D'ACTIVATION (V1.6.0).
 *
 * Une clé (format « ABCD 1234 ») achetée sur la boutique Chariow active l'abonnement pendant
 * [DURATION_DAYS] jours. Les jours se cumulent si une clé est saisie alors qu'un abonnement est
 * déjà en cours. Le serveur Supabase fait foi (fonction `redeem_activation_key`) ; ces
 * constantes servent uniquement à l'affichage et aux contrôles côté application.
 *
 * Le tarif peut évoluer avec la popularité de l'application (voir les conditions d'utilisation).
 */
object LicenseConfig {
    const val DURATION_DAYS = 30
    const val PRICE_FCFA = 1500

    /** Boutique Chariow qui vend les clés d'activation. */
    const val KEY_STORE_URL = "https://wldadmwl.mychariow.shop/prd_m7lrqhgu"
    const val KEY_EXAMPLE = "JUTG 5217"

    private val COMPACT_KEY = Regex("^[A-Z]{4}[0-9]{4}$")

    /** « jutg5217 », « JUTG  5217 » → « JUTG 5217 ». Retourne null si le format est invalide. */
    fun normalizeKey(raw: String): String? {
        val compact = raw.uppercase().filter { !it.isWhitespace() }
        if (!COMPACT_KEY.matches(compact)) return null
        return compact.substring(0, 4) + " " + compact.substring(4)
    }

    /** Met en forme la saisie au fil de la frappe : majuscules, 8 caractères max, espace après le 4e. */
    fun formatTyping(raw: String): String {
        val cleaned = raw.uppercase().filter { it in 'A'..'Z' || it in '0'..'9' }.take(8)
        return if (cleaned.length > 4) cleaned.substring(0, 4) + " " + cleaned.substring(4) else cleaned
    }
}

/** Résultat de la saisie d'une clé d'activation. */
sealed class ActivationResult {
    data class Success(val expiresAt: Long, val daysAdded: Int) : ActivationResult()
    data class Failure(val message: String) : ActivationResult()
}
