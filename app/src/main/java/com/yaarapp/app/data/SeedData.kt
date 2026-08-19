package com.yaarapp.app.data

import com.yaarapp.app.util.PasswordHasher

/**
 * Données de démonstration utilisées uniquement au tout premier lancement de
 * l'application (base de données vide). Elles servent à tester le parcours
 * "Acheter" avec de vrais produits dès l'installation, sans devoir créer une
 * boutique manuellement au préalable.
 *
 * Identifiants de test (voir README) :
 *   Boutique "Chic & Style"   → téléphone 22890000001 / mot de passe test1234
 *   Boutique "Yaar Électro"   → téléphone 22890000002 / mot de passe test1234
 */
object SeedData {

    fun demoUsers(): List<User> = listOf(
        User(
            fullName = "Ama (Chic & Style)",
            phone = "22890000001",
            passwordHash = PasswordHasher.hash("test1234"),
            whatsappNumber = "22890000001"
        ),
        User(
            fullName = "Kossi (Yaar Électro)",
            phone = "22890000002",
            passwordHash = PasswordHasher.hash("test1234"),
            whatsappNumber = "22890000002"
        )
    )

    fun demoShops(fashionOwnerId: Int, electroOwnerId: Int): List<Shop> = listOf(
        Shop(
            ownerId = fashionOwnerId,
            name = "Chic & Style",
            whatsappNumber = "22890000001",
            plan = Plan.GRATUIT
        ),
        Shop(
            ownerId = electroOwnerId,
            name = "Yaar Électro",
            whatsappNumber = "22890000002",
            plan = Plan.GRATUIT
        )
    )

    fun demoProducts(fashionShopId: Int, electroShopId: Int): List<Product> = listOf(
        Product(
            shopId = fashionShopId,
            name = "Sac à main élégant",
            description = "Sac à main tendance, simili-cuir de qualité, idéal pour toutes les occasions.",
            price = 17000.0,
            imageUrl = "res:product_sac",
            category = "Mode"
        ),
        Product(
            shopId = fashionShopId,
            name = "Veste homme élégante",
            description = "Veste croisée pour homme, coupe soignée, parfaite pour les grandes occasions.",
            price = 15000.0,
            imageUrl = "res:product_veste",
            category = "Mode"
        ),
        Product(
            shopId = fashionShopId,
            name = "Chaussures femme talon",
            description = "Sandales à talon fin, brides croisées, coloris doré.",
            price = 9000.0,
            imageUrl = "res:product_chaussure",
            category = "Mode"
        ),
        Product(
            shopId = electroShopId,
            name = "Tondeuse professionnelle",
            description = "Kit de tonte complet avec tondeuse, finisseur et rasoir, accessoires inclus.",
            price = 15000.0,
            imageUrl = "res:product_tondeuse",
            category = "Électronique"
        ),
        Product(
            shopId = electroShopId,
            name = "Mixeur multifonction",
            description = "Blender robuste avec bol en verre, broyeur à glace, plusieurs vitesses.",
            price = 15000.0,
            imageUrl = "res:product_moulinex",
            category = "Électronique"
        ),
        Product(
            shopId = electroShopId,
            name = "Mini frigo 2 portes",
            description = "Réfrigérateur compact 2 portes, idéal chambre ou petit espace.",
            price = 25000.0,
            imageUrl = "res:product_frigo",
            category = "Électronique"
        )
    )
}
