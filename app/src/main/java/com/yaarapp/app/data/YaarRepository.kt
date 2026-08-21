package com.yaarapp.app.data

import android.content.Context
import com.yaarapp.app.util.PasswordHasher
import kotlinx.coroutines.flow.Flow

sealed class AuthResult {
    data class Success(val user: User) : AuthResult()
    data class Error(val message: String) : AuthResult()
}

sealed class AddProductResult {
    object Success : AddProductResult()
    /** Le vendeur a atteint la limite de produits ACTIFS de son forfait actuel. */
    data class LimitReached(val plan: Plan) : AddProductResult()
    data class Error(val message: String) : AddProductResult()
}

class YaarRepository(context: Context) {

    private val db = YaarDatabase.getInstance(context)
    private val userDao = db.userDao()
    private val shopDao = db.shopDao()
    private val productDao = db.productDao()
    private val cartDao = db.cartDao()

    val session = SessionManager(context)

    // ---------- Amorçage des données de démonstration ----------

    suspend fun seedIfEmpty() {
        if (userDao.count() > 0) return
        val users = SeedData.demoUsers()
        val fashionOwnerId = userDao.insert(users[0]).toInt()
        val electroOwnerId = userDao.insert(users[1]).toInt()

        val shops = SeedData.demoShops(fashionOwnerId, electroOwnerId)
        val fashionShopId = shopDao.insert(shops[0]).toInt()
        val electroShopId = shopDao.insert(shops[1]).toInt()

        productDao.insertAll(SeedData.demoProducts(fashionShopId, electroShopId))
    }

    // ---------- Authentification (locale, voir data/User.kt) ----------

    /**
     * @param whatsappNumber déjà normalisé au format "00" + indicatif + numéro local
     * (voir [com.yaarapp.app.util.PhoneFormatter]).
     */
    suspend fun signUp(
        firstName: String,
        sex: Sex,
        country: Country,
        city: String,
        whatsappNumber: String,
        password: String
    ): AuthResult {
        if (firstName.isBlank() || city.isBlank() || password.length < 4) {
            return AuthResult.Error("Merci de remplir tous les champs (mot de passe : 4 caractères minimum).")
        }
        if (whatsappNumber.length < 10) {
            return AuthResult.Error("Le numéro WhatsApp saisi semble incomplet.")
        }
        if (userDao.findByWhatsapp(whatsappNumber) != null) {
            return AuthResult.Error("Un compte existe déjà avec ce numéro WhatsApp.")
        }
        val user = User(
            firstName = firstName,
            sex = sex,
            country = country,
            city = city,
            whatsappNumber = whatsappNumber,
            passwordHash = PasswordHasher.hash(password)
        )
        val id = userDao.insert(user)
        session.setCurrentUser(id.toInt())
        return AuthResult.Success(user.copy(id = id.toInt()))
    }

    suspend fun login(whatsappNumber: String, password: String): AuthResult {
        val user = userDao.findByWhatsapp(whatsappNumber)
            ?: return AuthResult.Error("Aucun compte trouvé avec ce numéro WhatsApp.")
        if (!PasswordHasher.matches(password, user.passwordHash)) {
            return AuthResult.Error("Mot de passe incorrect.")
        }
        session.setCurrentUser(user.id)
        return AuthResult.Success(user)
    }

    suspend fun logout() {
        session.clearSession()
    }

    suspend fun getUser(id: Int): User? = userDao.findById(id)

    // ---------- Boutique du vendeur connecté ----------

    fun observeMyShop(ownerId: Int): Flow<Shop?> = shopDao.observeShopForOwner(ownerId)

    /** La boutique hérite automatiquement du pays et de la ville du profil du vendeur. */
    suspend fun createShop(owner: User, name: String, whatsappNumber: String): Shop {
        val shop = Shop(
            ownerId = owner.id,
            name = name,
            whatsappNumber = whatsappNumber,
            country = owner.country,
            city = owner.city
        )
        val id = shopDao.insert(shop)
        return shop.copy(id = id.toInt())
    }

    fun observeShopProducts(shopId: Int): Flow<List<Product>> = productDao.observeByShop(shopId)

    suspend fun addProduct(
        shop: Shop,
        name: String,
        description: String,
        price: Double,
        imageUrl: String,
        category: String
    ): AddProductResult {
        if (name.isBlank() || description.isBlank() || imageUrl.isBlank() || price <= 0) {
            return AddProductResult.Error("Merci de remplir tous les champs (photo, nom, description, prix).")
        }
        val activeCount = productDao.countActiveForShop(shop.id)
        if (activeCount >= shop.plan.maxProducts) {
            return AddProductResult.LimitReached(shop.plan)
        }
        productDao.insert(
            Product(
                shopId = shop.id,
                name = name,
                description = description,
                price = price,
                imageUrl = imageUrl,
                category = category.ifBlank { "Divers" },
                country = shop.country,
                city = shop.city
            )
        )
        return AddProductResult.Success
    }

    suspend fun deleteProduct(product: Product) = productDao.delete(product)

    /** Le vendeur désactive manuellement un produit encore actif (ex : produit vendu). */
    suspend fun deactivateProduct(product: Product) {
        productDao.update(product.copy(isActive = false))
    }

    /**
     * Remet un produit désactivé en vente : réactive et réinitialise le compteur de 14 jours.
     * Vérifie que la boutique n'a pas déjà atteint sa limite de produits actifs.
     */
    suspend fun reactivateProduct(product: Product, shop: Shop): AddProductResult {
        val activeCount = productDao.countActiveForShop(shop.id)
        if (activeCount >= shop.plan.maxProducts) {
            return AddProductResult.LimitReached(shop.plan)
        }
        productDao.update(product.copy(isActive = true, activatedAt = System.currentTimeMillis()))
        return AddProductResult.Success
    }

    suspend fun productCountForShop(shopId: Int): Int = productDao.countActiveForShop(shopId)

    /**
     * À appeler chaque fois que le vendeur ouvre sa boutique : désactive automatiquement
     * tout produit actif dont les 14 jours d'exposition gratuite sont dépassés, et
     * retourne le nombre de produits concernés (pour afficher la notification).
     */
    suspend fun deactivateExpiredProducts(shopId: Int): Int {
        val cutoff = System.currentTimeMillis() - FREE_LISTING_DURATION_MS
        return productDao.deactivateExpired(shopId, cutoff)
    }

    // ---------- Marketplace ("Acheter") ----------

    fun observeMarketplaceProducts(): Flow<List<Product>> = productDao.observeAllActive()

    fun observeCategories(): Flow<List<String>> = productDao.observeCategories()

    suspend fun getProduct(id: Int): Product? = productDao.getById(id)

    suspend fun getShop(id: Int): Shop? = shopDao.getById(id)

    // ---------- Panier (par utilisateur connecté) ----------

    fun observeCart(userId: Int): Flow<List<CartItem>> = cartDao.observeCart(userId)

    suspend fun addToCart(userId: Int, product: Product, shop: Shop, quantity: Int = 1) {
        val existing = cartDao.getItem(userId, product.id)
        if (existing != null) {
            cartDao.update(existing.copy(quantity = existing.quantity + quantity))
        } else {
            cartDao.upsert(
                CartItem(
                    userId = userId,
                    productId = product.id,
                    productName = product.name,
                    price = product.price,
                    imageUrl = product.imageUrl,
                    shopId = shop.id,
                    shopName = shop.name,
                    shopWhatsappNumber = shop.whatsappNumber,
                    quantity = quantity
                )
            )
        }
    }

    suspend fun updateCartQuantity(item: CartItem, quantity: Int) {
        if (quantity <= 0) cartDao.delete(item) else cartDao.update(item.copy(quantity = quantity))
    }

    suspend fun removeFromCart(item: CartItem) = cartDao.delete(item)

    suspend fun clearCart(userId: Int) = cartDao.clear(userId)
}
