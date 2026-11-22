# Yaar-App V1.5.3

- **Notifications « Je suis intéressé » dupliquées** : chaque notification est maintenant identifiée par un
  identifiant Supabase unique (`remoteId`, index unique en base locale). Une synchro ou une réouverture de
  l'app ne peut plus recréer la même notification. Migration Room 12 -> 13 : les doublons déjà présents sont
  supprimés automatiquement au premier lancement.
- **Ancienneté en direct** : « A rejoint Yaar-App il y a … » = date/heure du jour - date/heure de création de la
  boutique (années, mois, jours + heures/minutes/secondes qui défilent chaque seconde). Affiché dans Profil,
  Boutique et vitrine publique.
- **Nom / logo** : une seule modification tous les 30 jours (contrôle local + triggers serveur, voir SQL V1.5.3).
- **Boutiques certifiées** : publication d'un produit dans toutes les villes de leur pays (case
  « Toutes les villes du pays »). Boutiques non certifiées : ville de la boutique + 5 villes maximum.
- Menu du bas : « Ma boutique » devient « Boutique ».
