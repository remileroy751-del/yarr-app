YAAR-APP — SUPER ADMIN

1. Déployer le dépôt sur GitHub.
2. Dans Supabase > Authentication > Users, créer le compte administrateur (email + mot de passe).
3. Dans le fichier SQL Yaar-App-Supabase-V1.5.1-IMMO-ADMIN.sql, remplacer VOTRE_EMAIL_ADMIN@EXEMPLE.COM par exactement cet email, puis exécuter le SQL dans Supabase.
4. Dans GitHub > Settings > Pages, choisir le déploiement depuis la branche principale et le dossier / (root).
5. Ouvrir l'URL GitHub Pages du dépôt : index.html est à la racine du dépôt.

Sécurité : ne jamais mettre la clé service_role dans index.html. Le fichier utilise uniquement la clé publishable Supabase et les policies/RPC administratives côté base.

V1.5.2 — mises à jour importantes
- Le premier changement de nom/logo est libre. Après un changement de nom OU de logo, les deux restent verrouillés pendant 30 jours.
- La date de création de la boutique est conservée côté Supabase et l'ancienneté est calculée depuis shops.created_at.
- Les boutiques certifiées utilisent admin/verified-badge.png.
- Les utilisateurs peuvent écrire au support depuis leur profil (80 caractères maximum).
- L'onglet « Messages reçus » du Super Admin permet de répondre aux utilisateurs.
- Avant de publier cette version, exécuter Yaar-App-Supabase-V1.5.1-UPDATE-SUPPORT-COOLDOWN.sql dans Supabase SQL Editor.

V1.5.4 — Certification gratuite 12 mois
- Avant de publier cette version, exécuter Yaar-App-Supabase-V1.5.4-UPDATE.sql dans Supabase SQL Editor.
- Onglet « Certifications » : voir la pièce d'identité (recto/verso), valider (12 mois à partir d'aujourd'hui) ou refuser.
- La date d'activation est la date système au moment du clic sur « Valider » ; l'expiration est calculée automatiquement (+365 jours).
- Une certification est désactivée automatiquement à l'expiration (tâche pg_cron toutes les 30 minutes).
- Pour clôturer la vague : update public.certification_campaign set is_open = false where id = 1;

V1.5.5 — Période 15 octobre → 15 novembre 2026
- Exécuter Yaar-App-Supabase-V1.5.5-UPDATE.sql (après le V1.5.4).
- Le 16 novembre 2026 à 00:00 UTC, l'application V1.5.5 se bloque et renvoie vers https://yaar-appv133.netlify.app/.

V1.6.0 — Clés d'activation (abonnement 30 jours)
- Exécuter Yaar-App-Supabase-V1.6.0-ACTIVATION-KEYS.sql dans Supabase SQL Editor.
- Admin > onglet « Clés d'activation » : choisir un fichier .txt (une clé par ligne, ex. JUTG 5217), puis « Envoyer vers Yaar-App ».
  Les doublons et lignes invalides sont ignorés et comptés. Jusqu'à 5 000 clés par envoi (découpé automatiquement).
- Mettez ensuite les mêmes clés en vente sur Chariow ; chaque clé saisie dans l'app donne 30 jours (cumulables).
- La mention du blocage au 16 novembre 2026 ci-dessus (V1.5.5) ne s'applique plus.
