# Base de données en ligne gratuite — pourquoi Firebase, et comment la brancher

## Pourquoi Firebase (Firestore) et pas autre chose

Aujourd'hui, Yaar-App stocke tout **uniquement sur le téléphone** (base Room/SQLite
locale). C'est parfait pour tester, mais ça veut dire que **chaque vendeur et chaque
acheteur voit une base différente** — un acheteur au Togo ne peut pas voir les
produits publiés depuis un téléphone au Bénin.

Pour que tout le monde voie la même chose (les mêmes boutiques, les mêmes produits,
en temps réel), il faut une base de données **en ligne**, partagée par tous les
téléphones. Recommandation : **Firebase Firestore** (Google), pour ces raisons :

- **Gratuit pour démarrer** (offre "Spark") : env. 1 Go de stockage, ~50 000
  lectures/jour et ~20 000 écritures/jour gratuits — largement suffisant pour
  lancer l'app dans les 4 pays et voir venir.
- **Fait pour le mobile** : SDK Android officiel, synchronisation en temps réel,
  fonctionne même avec une connexion instable (les écritures se mettent en attente
  puis se synchronisent).
- **Aucun serveur à gérer** : pas de VPS, pas de code backend à écrire/héberger.
- **Tri et filtres intégrés** (ex : tous les produits d'une ville, triés par date)
  — exactement ce qu'il faut pour "Disponible à Lomé", recherche par ville, etc.
- Alternative valable si vous préférez du SQL : **Supabase** (PostgreSQL, offre
  gratuite généreuse aussi). La logique ci-dessous s'adapterait facilement.

## Ce qui est déjà prêt côté code

Le code actuel (Room) est organisé pour que la bascule soit simple :
- Toute la logique métier passe par `YaarRepository` — c'est le SEUL endroit à
  modifier pour brancher Firestore, les écrans (`MarketplaceScreen`, `MyShopScreen`,
  etc.) n'ont pas à changer.
- Les entités `User`, `Shop`, `Product` sont déjà des `data class` simples,
  directement sérialisables en documents Firestore.
- Pays/ville sont déjà des champs structurés (`Country`, `city: String`), prêts pour
  des requêtes `whereEqualTo("country", ...)`, `whereEqualTo("city", ...)`.

## Étapes pour créer le projet Firebase (à faire vous-même, ~15 min)

Je ne peux pas créer le projet à votre place (il faut votre propre compte Google),
mais voici exactement la marche à suivre :

1. Allez sur **https://console.firebase.google.com** et connectez-vous avec un
   compte Google.
2. **Ajouter un projet** → nommez-le par ex. "Yaar-App" → décochez Google
   Analytics si vous ne voulez pas vous en occuper tout de suite → Créer.
3. Dans le projet, cliquez sur l'icône **Android** pour ajouter une app :
   - Nom du package : `com.yaarapp.app` (doit correspondre exactement à
     `applicationId` dans `app/build.gradle.kts`).
   - Téléchargez le fichier **`google-services.json`** généré et placez-le dans
     `YaarApp/app/google-services.json` (à la racine du dossier `app`).
4. Dans le menu de gauche → **Build → Firestore Database** → **Créer une base de
   données** → choisissez une région proche (ex. `europe-west1`) → démarrez en
   **mode production** (les règles de sécurité ci-dessous protègent les données).
5. Onglet **Règles**, remplacez par :

```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Tout le monde peut LIRE les boutiques et produits (marketplace public)
    match /shops/{shopId} {
      allow read: if true;
      allow write: if request.auth != null && request.auth.uid == resource.data.ownerId;
    }
    match /products/{productId} {
      allow read: if true;
      allow write: if request.auth != null;
    }
    // Un utilisateur ne peut lire/modifier que son propre profil
    match /users/{userId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
  }
}
```

   (Ces règles supposent que vous activez aussi **Firebase Authentication**
   — voir note plus bas — pour que `request.auth` existe.)

## Étapes pour brancher le code Android

1. Dans `YaarApp/build.gradle.kts` (racine), ajoutez le plugin Google Services :

```kotlin
plugins {
    id("com.android.application") version "8.5.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.24" apply false
    id("com.google.devtools.ksp") version "1.9.24-1.0.20" apply false
    id("com.google.gms.google-services") version "4.4.2" apply false   // AJOUT
}
```

2. Dans `YaarApp/app/build.gradle.kts` :

```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("com.google.gms.google-services")   // AJOUT
}

dependencies {
    // ... dépendances existantes ...

    // Firebase (BOM = gère les versions compatibles entre elles automatiquement)
    implementation(platform("com.google.firebase:firebase-bom:33.5.1"))
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-auth-ktx")
}
```

3. Structure des collections Firestore à créer (elles se créent toutes seules à la
   première écriture, rien à faire manuellement) :

```
users/{uid}          → firstName, sex, country, city, whatsappNumber, createdAt
shops/{shopId}       → ownerId, name, whatsappNumber, country, city, plan, createdAt
products/{productId} → shopId, name, description, price, imageUrl, category,
                        country, city, isActive, createdAt, activatedAt
```

4. Exemple de requête avec **tri** (le "tri des résultats" que vous avez demandé) —
   récupérer les produits actifs d'une ville, du plus récent au plus ancien :

```kotlin
firestore.collection("products")
    .whereEqualTo("isActive", true)
    .whereEqualTo("city", "Lomé")
    .orderBy("createdAt", Query.Direction.DESCENDING)
    .get()
```

5. Stratégie recommandée pour la migration : garder Room comme **cache local**
   (l'app reste rapide et fonctionne hors-ligne) et ajouter dans `YaarRepository`
   un "write-through" vers Firestore à chaque `addProduct`, `createShop`,
   `signUp`, etc., + un `addSnapshotListener` Firestore qui met à jour Room quand
   un autre téléphone publie un nouveau produit. C'est le sujet du prochain lot de
   travail une fois le projet Firebase créé de votre côté — dites-moi quand
   `google-services.json` est en place et je branche le code.

## Authentification

Pour que chaque vendeur ne modifie que SA boutique, il faut relier vos comptes
"WhatsApp + mot de passe" actuels à **Firebase Authentication**. Le plus simple :
Firebase Auth par **numéro de téléphone (OTP SMS)**, en réutilisant directement le
numéro WhatsApp déjà collecté au format `00<indicatif><numéro>` (il suffit de le
convertir en `+<indicatif><numéro>` pour Firebase, qui utilise le format E.164).
