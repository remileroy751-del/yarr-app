package com.yaarapp.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val shopId: Int,
    val name: String,
    val description: String,
    val price: Double,
    /**
     * Peut être :
     * - "res:nom_du_drawable" pour les produits de démonstration intégrés à l'application
     * - un chemin de fichier local (photo importée par le vendeur depuis sa galerie)
     * - une URL http(s) si un backend distant est branché plus tard
     */
    val imageUrl: String,
    val category: String,
    val createdAt: Long = System.currentTimeMillis()
)
