package com.yaarapp.app.data

enum class CertificationStatus(val label: String) {
    NONE("Non certifiée"),
    /** Ancien statut (V1.5.x, validation par pièce d'identité) — plus utilisé. */
    PENDING("Certification en cours d'étude"),
    CERTIFIED("Boutique certifiée"),
    /** Ancien statut (V1.5.x) — plus utilisé. */
    REJECTED("Demande refusée"),
    /** L'abonnement (clé d'activation) est arrivé à expiration : retour au mode gratuit. */
    EXPIRED("Abonnement expiré")
}
