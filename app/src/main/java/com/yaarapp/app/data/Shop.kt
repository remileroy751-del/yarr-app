package com.yaarapp.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shops")
data class Shop(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val ownerId: Int,
    /** UID Supabase du propriétaire, stable entre téléphones. */
    val ownerUid: String? = null,
    val name: String,
    val whatsappNumber: String,
    /** Pays et ville de la boutique — repris automatiquement du profil du vendeur à la création. */
    val country: Country,
    val city: String,
    /** Logo de la boutique (facultatif) — chemin local de la photo importée depuis la galerie. */
    val logoUrl: String? = null,
    /** Description libre de l'activité de la boutique (ex : "Vente de vêtements et accessoires femme"). */
    val activityDescription: String = "",
    /** Jusqu'à 3 catégories choisies parmi [ShopCategories.defaultCategories] pour décrire la boutique. */
    val categories: List<String> = emptyList(),
    /**
     * Ancien champ (forfaits payants V1.5.x), conservé uniquement pour la compatibilité de la base.
     * Ignoré depuis V1.6.0 : les limites dépendent de l'abonnement actif (voir [ShopLimits]).
     */
    val extraProductSlots: Int = 0,
    /** Ancien champ (V1.5.x), ignoré depuis V1.6.0. */
    val extraImmoSlots: Int = 0,
    // ---------- Certification de la boutique ----------
    val certificationStatus: CertificationStatus = CertificationStatus.NONE,
    /** Photos recto/verso de la pièce d'identité envoyées lors de la demande de certification. */
    val idCardFrontUrl: String? = null,
    val idCardBackUrl: String? = null,
    val certificationRequestedAt: Long? = null,
    /** Ancienne date de paiement (V1.5.3 et avant) — la certification est désormais gratuite. */
    val certificationPaidAt: Long? = null,
    /** Fin de l'abonnement / certification : calculée par Supabase (30 jours par clé, cumulables). */
    val certificationExpiresAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    /** Dernière modification du nom de la boutique. Null = jamais modifié depuis sa création. */
    val nameChangedAt: Long? = null,
    /** Dernière modification du logo; changement autorisé tous les 30 jours. */
    val logoChangedAt: Long? = null,
    /**
     * Identifiant du document Supabase correspondant (auto-généré par Supabase,
     * globalement unique) une fois cette boutique synchronisée en ligne. `null` tant
     * que la synchronisation n'a pas encore eu lieu (ex. pas de réseau) — l'app reste
     * pleinement fonctionnelle en local dans ce cas, la synchro réessaiera plus tard.
     */
    val remoteId: String? = null,
    /** Date système de validation de la certification par l'administrateur. */
    val certificationActivatedAt: Long? = null,
    /** Rang de création de la boutique dans son pays (1 = première). Attribué par Supabase. */
    val foundingRank: Int? = null
) {
    private companion object {
        const val CHANGE_COOLDOWN_MS = 30L * 24L * 60L * 60L * 1000L
    }

    // La première modification est autorisée immédiatement. Ensuite, le nom ET le logo
    // partagent le même délai de 30 jours : changer l'un bloque aussi l'autre.
    private fun lastIdentityChangeAt(): Long? = listOfNotNull(nameChangedAt, logoChangedAt).maxOrNull()
    fun nextLogoChangeAt(): Long = (lastIdentityChangeAt() ?: System.currentTimeMillis()) + CHANGE_COOLDOWN_MS
    fun canChangeLogo(now: Long = System.currentTimeMillis()): Boolean = lastIdentityChangeAt()?.let { now >= it + CHANGE_COOLDOWN_MS } ?: true

    fun nextNameChangeAt(): Long = (lastIdentityChangeAt() ?: System.currentTimeMillis()) + CHANGE_COOLDOWN_MS
    fun canChangeName(now: Long = System.currentTimeMillis()): Boolean = lastIdentityChangeAt()?.let { now >= it + CHANGE_COOLDOWN_MS } ?: true

    /**
     * Ancienneté réelle = (date du jour) - (date de création de la boutique), affichée avec
     * la plus grande unité complète : 1 jour, 2 jours, 2 semaines, 6 mois, 2 ans...
     */
    fun seniorityLabel(now: Long = System.currentTimeMillis()): String {
        val zone = java.time.ZoneId.systemDefault()
        val start = java.time.Instant.ofEpochMilli(createdAt).atZone(zone).toLocalDateTime()
        val end = java.time.Instant.ofEpochMilli(maxOf(now, createdAt)).atZone(zone).toLocalDateTime()
        val years = java.time.temporal.ChronoUnit.YEARS.between(start, end)
        val months = java.time.temporal.ChronoUnit.MONTHS.between(start, end)
        val days = java.time.temporal.ChronoUnit.DAYS.between(start, end)
        val duration = when {
            years >= 1 -> if (years == 1L) "1 an" else "$years ans"
            months >= 1 -> "$months mois"
            days >= 7 -> (days / 7).let { if (it == 1L) "1 semaine" else "$it semaines" }
            days >= 1 -> if (days == 1L) "1 jour" else "$days jours"
            else -> null
        }
        return if (duration == null) "A rejoint Yaar-App aujourd'hui" else "A rejoint Yaar-App depuis $duration"
    }

    // ---------- Certification ----------

    /** Vrai uniquement si l'abonnement (certification) est actif ET que sa date de fin n'est pas dépassée. */
    fun isCertifiedNow(now: Long = System.currentTimeMillis()): Boolean =
        certificationStatus == CertificationStatus.CERTIFIED && (certificationExpiresAt?.let { now < it } ?: false)

    /** Statut réel à l'instant [now] : une certification dont la date est passée est considérée expirée. */
    fun effectiveCertificationStatus(now: Long = System.currentTimeMillis()): CertificationStatus =
        if (certificationStatus == CertificationStatus.CERTIFIED && !isCertifiedNow(now)) CertificationStatus.EXPIRED
        else certificationStatus
}
