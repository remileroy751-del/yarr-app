# Yaar-App — Application Android (Marketplace multi-boutiques)

Application mobile native (Kotlin + Jetpack Compose, Material 3) pour **Yaar-App** :
une plateforme où chaque inscrit peut ouvrir un compte, créer sa propre boutique et
vendre ses produits (chaussures, vêtements, accessoires, meubles, électronique...),
et où tout le monde peut parcourir les produits publiés par les autres boutiques et
commander directement via WhatsApp.

## ✨ Fonctionnalités

- **Onboarding pays/ville** : à la toute première ouverture, l'utilisateur choisit son
  **pays** (Bénin, Burkina Faso, Côte d'Ivoire, Togo — avec drapeau) puis sa **ville**
  dans une liste alphabétique qui se réactualise automatiquement selon le pays choisi.
  Il clique sur **Continuer** pour passer à l'inscription (Prénom, Sexe F/M, numéro
  WhatsApp avec indicatif au format `0022890000000`, mot de passe).
- **Comptes utilisateurs** : inscription / connexion par **numéro WhatsApp** (identifiant
  unique) et mot de passe, session conservée entre les ouvertures de l'application.
- **Ma boutique** : chaque compte peut créer une boutique (pays/ville repris
  automatiquement du profil) et y publier des produits (photo, nom, description, prix
  en FCFA, catégorie). **Limite de 5 produits actifs** pour le forfait gratuit.
  - Tout produit publié gratuitement se **désactive automatiquement au bout de 14
    jours** ; à l'ouverture de sa boutique, le vendeur voit une **notification**
    l'invitant à vérifier ses produits.
  - Sous un produit **désactivé** : boutons **Remettre en vente** / **Supprimer**.
  - Sous un produit **actif** : boutons **Désactiver le produit** / **Supprimer
    définitivement**. Un produit désactivé reste visible par le vendeur (jamais
    supprimé automatiquement) — utile pour garder trace des articles déjà vendus.
  - Au-delà de la limite, un écran "Forfaits" invite à souscrire à un forfait
    supérieur (Standard : 30 produits, Pro : 100 produits — souscription/paiement pas
    encore branchés).
- **Acheter** : fil affichant les produits **actifs** de toutes les boutiques,
  filtrables par catégorie, **triés en priorité par la ville de l'acheteur**. Sous le
  prix de chaque produit s'affiche en miniature sa ville de disponibilité (ex.
  "Disponible à Lomé").
- **Recherche (loupe)** : recherche par mot-clé. Les résultats de la **même ville** que
  l'acheteur s'affichent en premier ; un bouton **"Afficher les produits disponibles
  dans d'autres villes"** en bas de liste ouvre la liste complète des villes du pays,
  sélectionnables (une ou plusieurs) pour élargir la recherche.
- **Panier** : les articles sont regroupés par boutique (puisque chaque boutique a son
  propre numéro WhatsApp) ; la validation envoie un message WhatsApp récapitulatif à
  chaque vendeur concerné.
- **Mon profil** : informations du compte connecté (prénom, sexe, pays/ville, numéro
  WhatsApp), aperçu de sa boutique et de son forfait, déconnexion.
- **Menu du bas** (dans l'ordre demandé) : **Mon profil**, **Ma boutique**, **Acheter**.
- **Design** : thème Material 3 aux couleurs de votre logo (orange `#F7941D` / vert
  `#1E8E3E`), icône de l'app générée à partir de vos fichiers, cartes produits au
  format carré (1:1) avec coins arrondis.
- **Données de démonstration** : vos 6 photos de produits ont été recadrées au format
  1:1 et publiées dans deux boutiques de test pour que le fil "Acheter" ne soit pas
  vide au premier lancement (voir comptes de test ci-dessous).

## 🔑 Comptes de test (données de démonstration)

| Boutique       | Numéro WhatsApp   | Mot de passe | Ville          |
|----------------|--------------------|--------------|----------------|
| Chic & Style   | 0022890000001      | test1234     | Lomé (Togo)    |
| Yaar Électro   | 0022990000002      | test1234     | Cotonou (Bénin)|

Ces deux boutiques possèdent déjà des produits (sac à main, veste, chaussures pour
Chic & Style ; tondeuse, mixeur, mini-frigo pour Yaar Électro) construits à partir des
photos que vous avez fournies. Vous pouvez aussi créer un **nouveau compte** depuis
l'écran d'inscription : vous verrez alors ces produits de démonstration dans l'onglet
"Acheter", exactement comme le ferait un vrai acheteur découvrant la plateforme.

## ⚠️ Limitation importante à connaître avant la mise en production

Dans cette version, **les comptes, boutiques, produits et paniers sont stockés
uniquement en local sur l'appareil** (base de données Room). Cela permet de tester tout
le parcours immédiatement, sans serveur. **Mais cela signifie que deux téléphones
différents ne voient pas les mêmes boutiques/produits** : chaque installation a sa
propre base de données locale, isolée des autres.

Pour une vraie plateforme où les boutiques créées par un vendeur sont visibles par tous
les acheteurs sur tous les téléphones, il faut brancher un **backend partagé** — la
piste la plus rapide est **Firebase** (Firebase Authentication pour les comptes,
Firestore pour les boutiques/produits, Firebase Storage pour les photos). La couche
`data/YaarRepository.kt` a été conçue pour isoler cette logique : c'est le seul fichier
à réécrire pour brancher un vrai backend, sans toucher aux écrans.
**Voir le guide détaillé étape par étape : [`BACKEND_FIREBASE.md`](./BACKEND_FIREBASE.md)**
(création du projet Firebase, dépendances Gradle, règles de sécurité, requêtes triées
par ville...).

## 🗂 Structure du projet

```
app/src/main/java/com/yaarapp/app/
├── MainActivity.kt              # Point d'entrée, héberge le NavHost Compose
├── YaarApplication.kt           # Initialise la base de données et amorce les données de démo
├── data/
│   ├── User.kt, Shop.kt, Product.kt, CartItem.kt     # Modèles (entités Room)
│   ├── Location.kt              # Enum Country (pays + drapeau + indicatif) + CityRepository
│   ├── Sex.kt                   # Enum Sexe (F / M)
│   ├── UserDao.kt, ShopDao.kt, ProductDao.kt, CartDao.kt
│   ├── YaarDatabase.kt          # Base Room (4 tables)
│   ├── YaarRepository.kt        # Authentification, boutique, marketplace, panier
│   ├── SessionManager.kt        # Session (DataStore) — utilisateur connecté
│   └── SeedData.kt              # Comptes/boutiques/produits de démonstration
├── nav/                         # Routes + NavHost (onboarding → auth → onglets principaux)
├── ui/
│   ├── components/               # ProductCard, barre de navigation du bas, filtres
│   ├── screens/                  # Onboarding (pays/ville), Login, SignUp, Marketplace,
│   │                              # Recherche, Détail produit, Panier, Ma boutique,
│   │                              # Ajout produit, Profil, Forfaits, Splash
│   └── theme/                    # Couleurs, typographie, thème Material 3
└── util/
    ├── WhatsAppHelper.kt         # Construction des liens wa.me (par boutique)
    ├── PhoneFormat.kt            # Formatage du numéro WhatsApp (00 + indicatif + numéro)
    ├── ImageStorage.kt           # Copie des photos importées + résolution des images
    └── PasswordHasher.kt         # Hachage simple des mots de passe (démo locale)
```

## 🛠 Personnaliser

1. **Données de démonstration** — modifiables ou supprimables dans
   `data/SeedData.kt`. Pour repartir d'une base vide, retirez simplement l'appel à
   `repository.seedIfEmpty()` dans `YaarApplication.kt`.
2. **Limites des forfaits** — modifiables dans l'énumération `Plan` (`data/Shop.kt`).
3. **Logo / icône** — déjà intégrés dans `res/mipmap-*` et `res/drawable-nodpi`.
4. **Photos produits de démonstration** — dans `res/drawable-nodpi/product_*.jpg`
   (recadrées au format 1:1 à partir des photos que vous avez fournies).

## ▶️ Compiler le projet

### Option A — Android Studio (recommandé)
1. Ouvrez le dossier du projet dans **Android Studio Koala (2024.1)** ou plus récent.
2. Android Studio régénère automatiquement le wrapper Gradle (`gradlew`) à la première
   synchronisation — aucune action supplémentaire n'est nécessaire.
3. Cliquez sur **Run ▶** ou **Build > Build Bundle(s) / APK(s) > Build APK(s)**.

### Option B — Ligne de commande
Le dépôt ne contient pas le binaire `gradle-wrapper.jar` (il ne peut pas être généré
dans cet environnement sans accès réseau). Deux façons de compiler en ligne de commande :

```bash
# 1) Si Gradle est installé sur votre machine :
gradle wrapper --gradle-version 8.7   # régénère gradlew une seule fois
./gradlew assembleDebug

# 2) Sans installation locale, utilisez directement Gradle :
gradle assembleDebug
```

L'APK généré se trouve dans `app/build/outputs/apk/debug/`.

### Option C — GitHub Actions (CI automatique)
Le workflow `.github/workflows/android-build.yml` est prêt à l'emploi : à chaque
`push`, il installe le JDK 17, installe Gradle, régénère le wrapper, compile l'APK de
debug et le publie en tant qu'artefact téléchargeable.

## 📦 Prérequis techniques

- Android Studio Koala+ / JDK 17
- `compileSdk` / `targetSdk` 34, `minSdk` 24 (Android 7.0+)
- Kotlin 1.9.24, Jetpack Compose (BOM 2024.06.00), Material 3, Navigation Compose, Room,
  DataStore Preferences, Coil
- Le choix de photo utilise le **sélecteur de photos système** (Photo Picker), qui ne
  nécessite aucune permission de stockage sur Android récent.

## 🚀 Prochaines étapes suggérées

- Brancher un vrai backend (Firebase recommandé) pour que les boutiques soient
  visibles sur tous les téléphones — voir la section "Limitation importante" ci-dessus.
- Activer le paiement des forfaits (Mobile Money / carte) sur l'écran "Forfaits".
- Ajouter un historique de commandes et des notifications.
- Publier l'application sur le Google Play Store (nécessite un compte développeur
  Google Play).

---
Basé sur le projet **Yaar-App** — identité, logique métier et photos de produits
reprises de vos échanges et des fichiers fournis.
