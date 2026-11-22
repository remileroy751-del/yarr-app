# Yaar-App V1.5.4

- **Ancienneté de la boutique** : « A rejoint Yaar-App depuis … » = date du jour − date de création, affichée avec la
  plus grande unité complète : 1 jour, 2 jours, 2 semaines, 6 mois, 2 ans (« aujourd'hui » avant 24 h).
  Profil, Boutique et vitrine publique. Aucun SQL nécessaire (utilise `shops.created_at`).
- **Vague de certification GRATUITE de 12 mois** : 100 premières boutiques créées dans chaque pays (Togo 100, Bénin 100…).
  Plus aucun paiement : la boutique envoie sa pièce d'identité recto + verso depuis « Certifier ma boutique (gratuit) ».
- **Activation / expiration** : la validation par l'administrateur enregistre `certification_activated_at` (date système)
  et calcule `certification_expires_at` = activation + 365 jours. Expiration automatique (pg_cron toutes les 30 min +
  contrôle à l'affichage dans l'application et dans l'admin).
- **Profil d'une boutique certifiée** : date d'activation, date d'expiration et compte à rebours (jours restants, puis h/min/s).
- **Interface admin** (`index.html`) : nouvel onglet « Certifications » — quota par pays, demandes en attente avec
  visionneuse recto/verso, boutons Valider (12 mois) / Refuser, certifications actives avec jours restants, expirées/refusées.
- **Sécurité serveur** : un trigger Supabase empêche un client de se certifier lui-même, de modifier les dates ou de
  dépasser le quota ; le rang de création dans le pays est attribué par le serveur et n'est jamais réutilisé.
- Base locale Room 13 → 14 (`certificationActivatedAt`, `foundingRank`). SQL : `Yaar-App-Supabase-V1.5.4-UPDATE.sql`.
