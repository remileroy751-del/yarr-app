# Yaar-App V1.6.0 — Mode gratuit + abonnement par clé d'activation

## Supprimé
- Vague de certification gratuite (15 oct → 15 nov 2026), quota par pays, envoi de pièce d'identité.
- Écran bloquant « Mise à jour obligatoire » du 16 novembre 2026 (`AppUpdateGate`, `UpdateRequiredScreen`) et son message.
- Forfaits payants 5 000 FCFA via PayDunya (produits et immobilier) et écran « Plans ».
  (PayDunya reste utilisé uniquement pour « Promouvoir mes produits ».)

## Nouveau
- **Mode gratuit** : 5 produits actifs, 5 annonces immobilières actives, ville de la boutique + 5 villes par produit.
- **Abonnement 30 jours par clé** (1 500 FCFA, prix modifiable, précisé dans les CGU) : 25 produits, 20 annonces
  immobilières, toutes les villes du pays, badge certifié.
- **Clé** : format `ABCD 1234`, usage unique. Les jours se cumulent si un abonnement est déjà en cours.
  À l'expiration, retour automatique au mode gratuit.
- **Fenêtre de saisie de clé** (lien vers la boutique Chariow : https://wldadmwl.mychariow.shop/prd_m7lrqhgu) affichée :
  Profil > « Certifier mon compte », et dès qu'on dépasse 5 produits ou 5 annonces immobilières.
- **Admin (index.html)** : onglet « Clés d'activation » — import d'un fichier .txt (une clé par ligne, plusieurs
  clés à la fois), statistiques, recherche, révocation, abonnements actifs/expirés, « +30 jours » manuel.
- **Sécurité serveur** : échange de clé et limites 5/25 et 5/20 appliqués par Supabase (impossible à contourner
  avec une app modifiée) ; blocage après 8 clés fausses en 30 min ; le client ne peut plus modifier
  son statut ni ses dates.
- CGU mises à jour (version 2 : réaffichées une fois à chaque utilisateur).
- Correction : lecture des dates Supabase (`+00:00`) plus robuste.

## À faire pour publier
1. Supabase > SQL Editor : exécuter `Yaar-App-Supabase-V1.6.0-ACTIVATION-KEYS.sql`.
2. Publier `index.html` (GitHub Pages), onglet « Clés d'activation » > importer vos fichiers .txt.
3. Pousser le code sur GitHub : le workflow compile l'APK.
