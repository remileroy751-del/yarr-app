package com.yaarapp.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Compte utilisateur.
 *
 * NOTE IMPORTANTE : pour cette version de démonstration, les comptes sont stockés
 * uniquement en local (base Room) avec un mot de passe simplement haché. Cela permet
 * de tester tout le parcours (inscription, connexion, boutique, achats) sans backend.
 * Pour une mise en production réelle où plusieurs utilisateurs doivent voir les mêmes
 * boutiques/produits depuis des téléphones différents, il faudra remplacer cette couche
 * par un vrai service (Firebase Auth + Firestore, ou une API avec base de données
 * centrale) — voir le README pour les pistes de migration.
 */
@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val fullName: String,
    val phone: String,
    val passwordHash: String,
    val whatsappNumber: String,
    val createdAt: Long = System.currentTimeMillis()
)
