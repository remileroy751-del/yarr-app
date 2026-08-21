package com.yaarapp.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class Plan(val maxProducts: Int, val label: String) {
    GRATUIT(5, "Gratuit"),
    STANDARD(30, "Standard"),
    PRO(100, "Pro (illimité ou presque)")
}

@Entity(tableName = "shops")
data class Shop(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val ownerId: Int,
    val name: String,
    val whatsappNumber: String,
    /** Pays et ville de la boutique — repris automatiquement du profil du vendeur à la création. */
    val country: Country,
    val city: String,
    val plan: Plan = Plan.GRATUIT,
    val createdAt: Long = System.currentTimeMillis()
)
