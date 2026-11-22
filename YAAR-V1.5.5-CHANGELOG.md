# Yaar-App V1.5.5

- **Période de la vague de certification gratuite : du 15 octobre au 15 novembre 2026.** Candidatures refusées avant/après
  (écran « Certifier ma boutique » + contrôle serveur Supabase, voir `Yaar-App-Supabase-V1.5.5-UPDATE.sql`).
- **Blocage au 16 novembre 2026 (00:00 UTC)** : écran « Mise à jour obligatoire » plein écran, non contournable, avec le lien
  https://yaar-appv133.netlify.app/ (bouton Télécharger + Copier le lien). Vérifié toutes les 30 s pendant l'utilisation.
- Heure fiable : l'heure du serveur Supabase (`yaar_server_time()`) fait foi ; hors ligne, la date la plus récente observée
  sur l'appareil est conservée (revenir en arrière dans l'horloge du téléphone ne débloque pas l'application).
- Interface admin : période, phase (en cours / terminée) et date de blocage affichées dans l'onglet Certifications.
- Important : la prochaine version publiée sur le site doit avoir une date de blocage différente (`AppUpdateGate.BLOCK_FROM_MS`),
  sinon elle sera elle aussi bloquée.
