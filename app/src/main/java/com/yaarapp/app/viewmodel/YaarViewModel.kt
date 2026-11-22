package com.yaarapp.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yaarapp.app.data.ActivationResult
import com.yaarapp.app.data.AddProductResult
import com.yaarapp.app.data.AdCampaign
import com.yaarapp.app.data.AdPricing
import com.yaarapp.app.data.AuthResult
import com.yaarapp.app.data.CartItem
import com.yaarapp.app.data.ChatConversation
import com.yaarapp.app.data.Country
import com.yaarapp.app.data.Interest
import com.yaarapp.app.data.LicenseConfig
import com.yaarapp.app.data.InterestStatus
import com.yaarapp.app.data.Product
import com.yaarapp.app.data.ProductCategories
import com.yaarapp.app.data.PayDunyaCreateRequest
import com.yaarapp.app.data.PayDunyaCreateResponse
import com.yaarapp.app.data.PayDunyaService
import com.yaarapp.app.data.Shop
import com.yaarapp.app.data.SupportMessage
import com.yaarapp.app.data.User
import com.yaarapp.app.data.YaarRepository
import com.yaarapp.app.data.maxImmoListings
import com.yaarapp.app.data.maxProducts
import com.yaarapp.app.util.PhoneFormat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class YaarViewModel(private val repository: YaarRepository) : ViewModel() {

    private val payDunyaService = PayDunyaService()

    // ---------- Session ----------

    val currentUserId: StateFlow<Int?> =
        repository.session.currentUserId.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _sessionReady = MutableStateFlow(false)
    val sessionReady: StateFlow<Boolean> = _sessionReady

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser

    companion object {
        /** V2 : conditions mises à jour (abonnement par clé d'activation) — réaffichées une fois à tous. */
        const val TERMS_VERSION = 2
    }

    /** Vrai lorsque les conditions correspondant à la version courante ont été acceptées. */
    val termsAccepted: StateFlow<Boolean> = repository.session.acceptedTermsVersion
        .map { it >= TERMS_VERSION }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun acceptTerms(onDone: () -> Unit) {
        viewModelScope.launch {
            repository.session.acceptTerms(TERMS_VERSION)
            onDone()
        }
    }

    /** Les anciens comptes de la version précédente doivent définir un mot de passe une seule fois. */
    val needsAccountUpgrade: StateFlow<Boolean> = flowOf(false)
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    init {
        viewModelScope.launch {
            runCatching { repository.synchronizeSession() }
            _sessionReady.value = true
        }
        viewModelScope.launch {
            currentUserId.collect { id ->
                val local = if (id != null) repository.getUser(id) else null
                _currentUser.value = local
                if (local?.firebaseUid != null) {
                    runCatching { repository.restoreAccount(local) }
                        .onSuccess { restored -> _currentUser.value = restored }
                }
            }
        }
        // Moteur d'exposition des campagnes publicitaires : une "exposition" par ouverture d'app.
        viewModelScope.launch { repository.recordAppOpenExposure() }
    }

    // ---------- Onboarding : sélection pays / ville (avant inscription) ----------

    private val _onboardingCountry = MutableStateFlow<Country?>(null)
    val onboardingCountry: StateFlow<Country?> = _onboardingCountry

    private val _onboardingCity = MutableStateFlow<String?>(null)
    val onboardingCity: StateFlow<String?> = _onboardingCity

    fun selectOnboardingCountry(country: Country) {
        _onboardingCountry.value = country
        _onboardingCity.value = null // la ville se réinitialise automatiquement quand le pays change
    }

    fun selectOnboardingCity(city: String) {
        _onboardingCity.value = city
    }

    /** Liste des villes du pays sélectionné, se met à jour automatiquement quand le pays change. */
    val onboardingCities: StateFlow<List<String>> = _onboardingCountry.map { country ->
        if (country == null) emptyList() else com.yaarapp.app.data.CityRepository.citiesFor(country)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ---------- Authentification ----------

    fun isValidEmail(email: String): Boolean = repository.isValidEmail(email)

    fun savedLoginEmail(): String = repository.savedLoginEmail()
    fun rememberedLoginPassword(): String? = repository.rememberedLoginPassword()
    fun saveLoginCredentials(email: String, password: String?, rememberPassword: Boolean) =
        repository.saveLoginCredentials(email, password, rememberPassword)

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError

    /** Étapes 2 et 3 : identité e-mail puis mot de passe. */
    fun signUp(
        firstName: String,
        email: String,
        localWhatsappNumber: String,
        password: String,
        onDone: () -> Unit
    ) {
        val country = _onboardingCountry.value
        val city = _onboardingCity.value
        if (country == null || city == null) {
            _authError.value = "Merci de sélectionner votre pays et votre ville."
            return
        }
        if (!repository.isValidEmail(email)) {
            _authError.value = "Veuillez saisir une adresse e-mail valide."
            return
        }
        if (!PhoneFormat.isValidLocalNumber(localWhatsappNumber)) {
            _authError.value = "Le numéro WhatsApp saisi semble incomplet."
            return
        }
        if (!repository.isValidPassword(password)) {
            _authError.value = "Le mot de passe doit contenir exactement 6 caractères, lettres et chiffres uniquement."
            return
        }
        val whatsappNumber = PhoneFormat.formatWhatsapp(country, localWhatsappNumber)
        viewModelScope.launch {
            when (val result = repository.signUp(firstName, email, country, city, whatsappNumber, password)) {
                is AuthResult.Success -> {
                    _authError.value = null
                    _currentUser.value = result.user
                    onDone()
                }
                is AuthResult.Error -> _authError.value = result.message
            }
        }
    }

    fun secureLegacyAccount(password: String, onDone: () -> Unit) {
        val user = _currentUser.value
        if (user == null) {
            _authError.value = "Aucun compte local à sécuriser."
            return
        }
        viewModelScope.launch {
            when (val result = repository.secureLegacyAccount(user, password)) {
                is AuthResult.Success -> {
                    _authError.value = null
                    _currentUser.value = result.user
                    onDone()
                }
                is AuthResult.Error -> _authError.value = result.message
            }
        }
    }

    /** Connexion par adresse e-mail + mot de passe Supabase. */
    fun login(email: String, password: String, onDone: () -> Unit) {
        viewModelScope.launch {
            when (val result = repository.login(email, password)) {
                is AuthResult.Success -> {
                    _authError.value = null
                    _currentUser.value = result.user
                    onDone()
                }
                is AuthResult.Error -> _authError.value = result.message
            }
        }
    }

    fun clearAuthError() {
        _authError.value = null
    }

    fun refreshCurrentUser() {
        val id = currentUserId.value ?: return
        viewModelScope.launch { _currentUser.value = repository.getUser(id) }
    }

    /** Réglage "Recevoir des notifications", modifiable dans Mon profil. */
    fun setNotificationsEnabled(enabled: Boolean) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            _currentUser.value = repository.setNotificationsEnabled(user, enabled)
        }
    }

    fun logout(onDone: () -> Unit) {
        viewModelScope.launch {
            repository.logout()
            _currentUser.value = null
            onDone()
        }
    }

    // ---------- Marketplace ("Acheter") ----------

    val allProducts: StateFlow<List<Product>> =
        repository.observeMarketplaceProducts().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Liste fixe (voir ProductCategories) — toujours affichée en entier, même sans produit encore publié. */
    val categories: List<String> = ProductCategories.all

    val immoListings: StateFlow<List<Product>> = repository.observeImmoListings().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory

    val filteredProducts: StateFlow<List<Product>> =
        combine(allProducts, _selectedCategory, currentUser) { products, category, user ->
            val scoped = if (user == null) products else products.filter {
                it.country == user.country && user.city in (it.availableCities.ifEmpty { listOf(it.city) })
            }
            val categorized = if (category == null) scoped else scoped.filter { it.category == category }
            if (user == null) categorized else categorized.sortedWith(
                compareByDescending<Product> { it.isPromoted }
                    .thenByDescending { user.city in (it.availableCities.ifEmpty { listOf(it.city) }) }
                    .thenByDescending { it.createdAt }
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectCategory(category: String?) {
        _selectedCategory.value = category
    }

    suspend fun getProduct(id: Int): Product? = repository.getProduct(id)
    suspend fun getShop(id: Int): Shop? = repository.getShop(id)

    // ---------- Recherche (loupe) : priorité aux produits de la ville de l'acheteur ----------

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    private val searchMatches: StateFlow<List<Product>> =
        combine(allProducts, _searchQuery) { products, query ->
            if (query.isBlank()) emptyList()
            else products.filter { p ->
                p.name.contains(query, ignoreCase = true) ||
                    p.description.contains(query, ignoreCase = true) ||
                    p.category.contains(query, ignoreCase = true)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Résultats de la même ville que l'acheteur — affichés en priorité. */
    val searchResultsSameCity: StateFlow<List<Product>> =
        combine(searchMatches, currentUser) { matches, user ->
            if (user == null) matches else matches
                .filter { it.country == user.country && user.city in (it.availableCities.ifEmpty { listOf(it.city) }) }
                .sortedWith(compareByDescending<Product> { it.isPromoted }.thenByDescending { it.createdAt })
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Résultats du même pays, après ceux de la ville de l'acheteur. */
    val searchResultsOtherCities: StateFlow<List<Product>> =
        combine(searchMatches, currentUser) { matches, user ->
            if (user == null) emptyList() else matches
                .filter { p ->
                    p.country == user.country &&
                        user.city !in (p.availableCities.ifEmpty { listOf(p.city) }) &&
                        (p.availableCities.ifEmpty { listOf(p.city) }).isNotEmpty()
                }
                .sortedWith(compareByDescending<Product> { it.isPromoted }.thenByDescending { it.createdAt })
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ---------- Ma boutique ----------

    /**
     * Dernier état de connexion, affiché en
     * bannière dans "Ma boutique" — permet de diagnostiquer sans outil externe.
     */
    val lastSyncEvent: StateFlow<String?> = repository.lastSyncEvent

    fun dismissLastSyncEvent() = repository.clearLastSyncEvent()

    @OptIn(ExperimentalCoroutinesApi::class)
    val myShop: StateFlow<Shop?> = currentUserId.flatMapLatest { id ->
        if (id == null) {
            flowOf(null)
        } else {
            repository.observeMyShop(id).catch { emit(null) }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val myShopProducts: StateFlow<List<Product>> = myShop.flatMapLatest { shop ->
        if (shop == null) {
            flowOf(emptyList())
        } else {
            repository.observeShopProducts(shop.id).catch { emit(emptyList()) }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _shopCreationError = MutableStateFlow<String?>(null)
    val shopCreationError: StateFlow<String?> = _shopCreationError

    private val _shopNameMessage = MutableStateFlow<String?>(null)
    val shopNameMessage: StateFlow<String?> = _shopNameMessage

    fun renameShop(newName: String) {
        val shop = myShop.value ?: return
        viewModelScope.launch {
            repository.renameShop(shop, newName).onSuccess {
                _shopNameMessage.value = "Nom de la boutique modifié avec succès. Une nouvelle modification sera possible dans 30 jours."
            }.onFailure {
                _shopNameMessage.value = it.message ?: "Impossible de modifier le nom de votre boutique."
            }
        }
    }

    fun clearShopNameMessage() { _shopNameMessage.value = null }

    fun createShop(
        name: String,
        logoUrl: String?,
        activityDescription: String,
        categories: List<String>,
        onDone: () -> Unit
    ) {
        val owner = _currentUser.value
        if (owner == null || name.isBlank()) {
            _shopCreationError.value = "Merci de renseigner le nom de la boutique."
            return
        }
        viewModelScope.launch {
            // Le numéro WhatsApp de la boutique est automatiquement celui du compte
            // connecté (déjà saisi une seule fois à l'inscription) — pas besoin de le
            // ressaisir ici.
            repository.createShop(owner, name, owner.whatsappNumber, logoUrl, activityDescription, categories)
            _shopCreationError.value = null
            onDone()
        }
    }

    /**
     * Notification "produits à vérifier" : nombre de produits désactivés automatiquement
     * (14 jours écoulés) lors de la dernière ouverture de la boutique par le vendeur.
     * `null` = pas de notification à afficher.
     */
    private val _expiredNotice = MutableStateFlow<Int?>(null)
    val expiredNotice: StateFlow<Int?> = _expiredNotice

    /** À appeler à chaque ouverture de l'écran "Ma boutique" par le vendeur. */
    fun checkShopExpirations() {
        val shop = myShop.value ?: return
        viewModelScope.launch {
            runCatching { repository.deactivateExpiredProducts(shop.id) }
                .onSuccess { count -> if (count > 0) _expiredNotice.value = count }
                .onFailure { e ->
                    repository.reportLocalSyncIssue(
                        "Impossible de vérifier les produits expirés : ${e.message ?: "erreur locale"}"
                    )
                }
        }
    }

    fun dismissExpiredNotice() {
        _expiredNotice.value = null
    }

    private val _addProductError = MutableStateFlow<String?>(null)
    val addProductError: StateFlow<String?> = _addProductError

    private val _publicationStatus = MutableStateFlow<String?>(null)
    val publicationStatus: StateFlow<String?> = _publicationStatus
    private var publicationStatusJob: Job? = null

    fun addProduct(
        name: String,
        description: String,
        price: Double,
        imageUrl: String,
        category: String,
        availableCities: List<String>,
        internalDiscussionEnabled: Boolean = true,
        whatsappDiscussionEnabled: Boolean = true,
        onSuccess: () -> Unit
    ) {
        val shop = myShop.value ?: return
        viewModelScope.launch {
            when (val result = repository.addProduct(shop, name, description, price, imageUrl, category, availableCities, internalDiscussionEnabled, whatsappDiscussionEnabled)) {
                is AddProductResult.Success -> {
                    _addProductError.value = null
                    publicationStatusJob?.cancel()
                    _publicationStatus.value = "En cours de validation"
                    publicationStatusJob = viewModelScope.launch {
                        delay(10_000L)
                        _publicationStatus.value = "Publié"
                    }
                    onSuccess()
                }
                is AddProductResult.LimitReached -> {
                    _addProductError.value = null
                    _activationPrompt.value = ActivationReason.PRODUCT_LIMIT
                }
                is AddProductResult.Error -> _addProductError.value = result.message
            }
        }
    }

    fun addImmoListing(title: String, description: String, firstImageUrl: String, secondImageUrl: String?, listingType: String, onSuccess: () -> Unit) {
        val shop = myShop.value ?: return
        viewModelScope.launch {
            when (val result = repository.addImmoListing(shop, title, description, firstImageUrl, secondImageUrl, listingType)) {
                is AddProductResult.Success -> { _addProductError.value = null; onSuccess() }
                is AddProductResult.LimitReached -> { _addProductError.value = null; _activationPrompt.value = ActivationReason.IMMO_LIMIT }
                is AddProductResult.Error -> _addProductError.value = result.message
            }
        }
    }

    fun updateShopLogo(logoUrl: String, onSuccess: () -> Unit = {}) {
        val shop = myShop.value ?: return
        viewModelScope.launch { repository.updateShopLogo(shop, logoUrl).onSuccess { onSuccess() }.onFailure { _shopNameMessage.value = it.message } }
    }

    fun clearAddProductMessages() {
        _addProductError.value = null
    }

    fun deleteProduct(product: Product) {
        viewModelScope.launch { repository.deleteProduct(product) }
    }

    /** Vendeur : désactive manuellement un produit encore actif (ex : déjà vendu). */
    fun deactivateProduct(product: Product) {
        viewModelScope.launch { repository.deactivateProduct(product) }
    }

    /** Vendeur : remet en vente un produit désactivé (relance les 14 jours d'exposition gratuite). */
    fun republishProduct(product: Product) {
        val shop = myShop.value ?: return
        viewModelScope.launch {
            when (val result = repository.reactivateProduct(product, shop)) {
                is AddProductResult.LimitReached ->
                    _activationPrompt.value =
                        if (product.listingType == "PRODUCT") ActivationReason.PRODUCT_LIMIT else ActivationReason.IMMO_LIMIT
                else -> Unit
            }
        }
    }

    // ---------- Paiement PayDunya : uniquement la publicité ("Promouvoir mes produits") ----------
    // L'abonnement de 30 jours ne passe plus par un prestataire de paiement : il s'active par clé.

    sealed class PendingPayment {
        abstract val amountFcfa: Int
        abstract val description: String

        /** Bouton "Promouvoir mes produits" : campagne dont le prix dépend du nombre d'expositions choisi. */
        data class ProductPromotion(val product: Product, val expositions: Int, val days: Int) : PendingPayment() {
            override val amountFcfa get() = AdPricing.priceFor(expositions)
            override val description get() =
                "Promotion \"${product.name}\" — $expositions expositions sur $days jours — Yaar-App"
        }

    }

    private val _pendingPayment = MutableStateFlow<PendingPayment?>(null)
    val pendingPayment: StateFlow<PendingPayment?> = _pendingPayment

    /** Étape 1 du bouton "Promouvoir mes produits" : le vendeur choisit le produit. */
    private val _productToPromote = MutableStateFlow<Product?>(null)
    val productToPromote: StateFlow<Product?> = _productToPromote

    fun selectProductToPromote(product: Product) {
        _productToPromote.value = product
    }

    /**
     * Étape 2 : le vendeur règle le nombre d'expositions (bornes [AdPricing.MIN_EXPOSITIONS]
     * à [AdPricing.MAX_EXPOSITIONS]) et le nombre de jours ([AdPricing.MIN_DAYS] à
     * [AdPricing.MAX_DAYS]) → le prix est calculé automatiquement puis ouvre PayDunya.
     */
    fun requestAdCampaign(expositions: Int, days: Int) {
        val product = _productToPromote.value ?: return
        val safeExpositions = expositions.coerceIn(AdPricing.MIN_EXPOSITIONS, AdPricing.MAX_EXPOSITIONS)
        val safeDays = days.coerceIn(AdPricing.MIN_DAYS, AdPricing.MAX_DAYS)
        _pendingPayment.value = PendingPayment.ProductPromotion(product, safeExpositions, safeDays)
    }

    // ---------- Abonnement par clé d'activation (30 jours, cumulables) ----------

    /** Pourquoi la fenêtre « Entrez votre clé d'activation » est affichée. */
    enum class ActivationReason { CERTIFY, PRODUCT_LIMIT, IMMO_LIMIT }

    sealed interface ActivationState {
        object Idle : ActivationState
        object Working : ActivationState
        data class Success(val expiresAt: Long, val daysAdded: Int) : ActivationState
        data class Error(val message: String) : ActivationState
    }

    private val _activationPrompt = MutableStateFlow<ActivationReason?>(null)
    val activationPrompt: StateFlow<ActivationReason?> = _activationPrompt

    private val _activationState = MutableStateFlow<ActivationState>(ActivationState.Idle)
    val activationState: StateFlow<ActivationState> = _activationState

    /** Ouvre la fenêtre de saisie de la clé (menu Profil, limite de produits ou d'annonces atteinte). */
    fun showActivationPrompt(reason: ActivationReason) {
        _activationState.value = ActivationState.Idle
        _activationPrompt.value = reason
    }

    fun dismissActivationPrompt() {
        _activationPrompt.value = null
        _activationState.value = ActivationState.Idle
    }

    fun redeemActivationKey(rawCode: String) {
        if (_activationState.value is ActivationState.Working) return
        val normalized = LicenseConfig.normalizeKey(rawCode)
        if (normalized == null) {
            _activationState.value = ActivationState.Error(
                "Format invalide. Une clé ressemble à : ${LicenseConfig.KEY_EXAMPLE} (4 lettres, un espace, 4 chiffres)."
            )
            return
        }
        _activationState.value = ActivationState.Working
        viewModelScope.launch {
            _activationState.value = when (val result = repository.redeemActivationKey(normalized)) {
                is ActivationResult.Success -> ActivationState.Success(result.expiresAt, result.daysAdded)
                is ActivationResult.Failure -> ActivationState.Error(result.message)
            }
        }
    }

    /**
     * Appelé avant d'ouvrir le formulaire de publication d'un produit : si la limite du mode actuel
     * est atteinte, la fenêtre de clé d'activation s'affiche à la place. Retourne true si on peut publier.
     */
    fun checkCanPublishProduct(): Boolean {
        val shop = myShop.value ?: return true
        val active = myShopProducts.value.count { it.isActive && it.listingType == "PRODUCT" }
        if (active >= shop.maxProducts) {
            showActivationPrompt(ActivationReason.PRODUCT_LIMIT)
            return false
        }
        return true
    }

    /** Idem pour les annonces immobilières (5 en mode gratuit, 20 avec abonnement). */
    fun checkCanPublishImmo(): Boolean {
        val shop = myShop.value ?: return true
        val active = myShopProducts.value.count { it.isActive && it.listingType != "PRODUCT" }
        if (active >= shop.maxImmoListings) {
            showActivationPrompt(ActivationReason.IMMO_LIMIT)
            return false
        }
        return true
    }

    suspend fun createPayDunyaPayment(): Pair<String, String> {
        val payment = _pendingPayment.value ?: error("Aucun paiement en attente.")
        val user = _currentUser.value ?: error("Aucun compte connecté.")
        val shop = myShop.value
        val request = when (payment) {
            is PendingPayment.ProductPromotion -> PayDunyaCreateRequest(
                purpose = "PRODUCT_PROMOTION", amountFcfa = payment.amountFcfa, description = payment.description,
                productId = payment.product.remoteId, shopId = shop?.remoteId, expositions = payment.expositions, durationDays = payment.days
            )
        }
        if (request.shopId.isNullOrBlank()) error("Boutique introuvable. Ouvrez d'abord votre boutique.")
        val response = payDunyaService.createPayment(request)
        return response.paymentId to response.checkoutUrl
    }

    suspend fun checkPayDunyaPayment(paymentId: String): String =
        payDunyaService.checkPayment(paymentId).status

    fun checkPayDunyaPayment(paymentId: String, onResult: (String) -> Unit) {
        viewModelScope.launch {
            runCatching { checkPayDunyaPayment(paymentId) }
                .onSuccess { status ->
                    if (status == "COMPLETED") onPaymentSuccess { onResult(status) } else onResult(status)
                }
                .onFailure { onResult("ERROR") }
        }
    }

    /** Appelé après confirmation serveur PayDunya. */
    fun onPaymentSuccess(onDone: () -> Unit) {
        val payment = _pendingPayment.value ?: return
        viewModelScope.launch {
            when (payment) {
                is PendingPayment.ProductPromotion -> {
                    val shop = myShop.value
                    if (shop != null) repository.createAdCampaign(payment.product, shop, payment.expositions, payment.days)
                }
            }
            _pendingPayment.value = null
            _productToPromote.value = null
            onDone()
        }
    }

    // ---------- "Ma Publicité" : suivi des campagnes en cours ----------

    @OptIn(ExperimentalCoroutinesApi::class)
    val activeAdCampaigns: StateFlow<List<AdCampaign>> = myShop.flatMapLatest { shop ->
        if (shop == null) emptyFlow() else repository.observeActiveAdCampaignsForShop(shop.id)
    }.catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ---------- Notifications "Je suis intéressé" (boutique) ----------

    /** Sur la fiche produit, l'acheteur clique "Je suis intéressé" → notifie le vendeur. */
    fun expressInterest(product: Product, shop: Shop) {
        val buyer = _currentUser.value ?: return
        viewModelScope.launch { repository.expressInterest(product, shop, buyer) }

    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val myInterests: StateFlow<List<Interest>> = myShop.flatMapLatest { shop ->
        if (shop == null) emptyFlow() else repository.observeInterestsForOwner(shop.ownerId)
    }.catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val unreadInterestCount: StateFlow<Int> = myShop.flatMapLatest { shop ->
        if (shop == null) flowOf(0) else repository.observeUnreadInterestCount(shop.ownerId)
    }.catch { emit(0) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun markInterestRead(interest: Interest) {
        viewModelScope.launch { repository.markInterestRead(interest) }
    }

    /** Le vendeur répond depuis la notification : "Oui disponible" / "Non indisponible". */
    fun setInterestStatus(interest: Interest, status: InterestStatus) {
        viewModelScope.launch { repository.setInterestStatus(interest, status) }
    }

    // ---------- Panier ----------

    @OptIn(ExperimentalCoroutinesApi::class)
    val cartItems: StateFlow<List<CartItem>> = currentUserId.flatMapLatest { id ->
        if (id == null) emptyFlow() else repository.observeCart(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val cartTotal: StateFlow<Double> =
        cartItems.map { items -> items.sumOf { it.price * it.quantity } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val cartItemCount: StateFlow<Int> =
        cartItems.map { items -> items.sumOf { it.quantity } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val conversations: StateFlow<List<ChatConversation>> = currentUser.flatMapLatest { user ->
        val uid = user?.firebaseUid
        if (uid.isNullOrBlank()) emptyFlow() else repository.observeConversations(uid)
    }.catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun refreshConversations() { /* Le flux se rafraîchit automatiquement. */ }

    fun conversation(id: String): kotlinx.coroutines.flow.Flow<ChatConversation?> = repository.observeConversation(id)

    fun openChat(product: Product, shop: Shop, onReady: (String?, String?) -> Unit) {
        val buyer = _currentUser.value
        if (buyer?.firebaseUid.isNullOrBlank()) { onReady(null, "Veuillez vous connecter pour discuter."); return }
        viewModelScope.launch {
            runCatching { repository.ensureConversation(product, shop, buyer!!) }
                .onSuccess { onReady(it, null) }
                .onFailure { onReady(null, it.message ?: "Impossible d'ouvrir la discussion.") }
        }
    }

    fun chatConversationId(product: Product, shop: Shop): String? {
        val buyer = _currentUser.value ?: return null
        if (buyer.firebaseUid == null) return null
        return repository.conversationId(product, shop, buyer)
    }

    fun observeChatMessages(conversationId: String): kotlinx.coroutines.flow.Flow<List<com.yaarapp.app.data.ChatMessage>> =
        repository.observeChatMessages(conversationId)

    fun sendChatMessage(product: Product, shop: Shop, text: String, onResult: (String?) -> Unit = {}) {
        val buyer = _currentUser.value
        if (buyer == null || buyer.firebaseUid == null) { onResult("Veuillez vous connecter pour discuter avec le fournisseur."); return }
        if (text.isBlank()) return
        viewModelScope.launch {
            runCatching { repository.sendChatMessage(product, shop, buyer, text) }
                .onSuccess { onResult(null) }
                .onFailure { onResult(it.message ?: "Impossible d'envoyer le message.") }
        }
    }

    private val _supportMessages = MutableStateFlow<List<SupportMessage>>(emptyList())
    val supportMessages: StateFlow<List<SupportMessage>> = _supportMessages

    fun refreshSupportMessages() {
        viewModelScope.launch {
            runCatching { repository.getSupportMessages() }
                .onSuccess { _supportMessages.value = it }
        }
    }

    fun sendSupportMessage(text: String, onResult: (String?) -> Unit = {}) {
        val clean = text.trim()
        if (clean.isBlank()) { onResult("Veuillez saisir votre message."); return }
        if (clean.length > 80) { onResult("Le message ne doit pas dépasser 80 caractères."); return }
        viewModelScope.launch {
            runCatching { repository.sendSupportMessage(clean) }
                .onSuccess {
                    refreshSupportMessages()
                    onResult(null)
                }
                .onFailure { onResult(it.message ?: "Impossible d'envoyer le message.") }
        }
    }

    fun sendChatMessageInConversation(conversationId: String, text: String, onResult: (String?) -> Unit = {}) {
        val sender = _currentUser.value
        if (sender == null || sender.firebaseUid == null) { onResult("Veuillez vous connecter pour discuter."); return }
        if (text.isBlank()) return
        viewModelScope.launch {
            runCatching { repository.sendChatMessageInConversation(conversationId, sender, text) }
                .onSuccess { onResult(null) }
                .onFailure { onResult(it.message ?: "Impossible d'envoyer le message.") }
        }
    }

    fun deleteAccount(password: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val user = _currentUser.value
        if (user == null) { onError("Aucun compte connecté."); return }
        viewModelScope.launch {
            runCatching { repository.deleteAccount(user, password) }
                .onSuccess { _currentUser.value = null; onSuccess() }
                .onFailure { onError(it.message ?: "Impossible de supprimer le compte.") }
        }
    }

    fun addToCart(product: Product, shop: Shop) {
        val userId = currentUserId.value ?: return
        viewModelScope.launch { repository.addToCart(userId, product, shop) }
    }

    fun increaseQuantity(item: CartItem) {
        viewModelScope.launch { repository.updateCartQuantity(item, item.quantity + 1) }
    }

    fun decreaseQuantity(item: CartItem) {
        viewModelScope.launch { repository.updateCartQuantity(item, item.quantity - 1) }
    }

    fun removeFromCart(item: CartItem) {
        viewModelScope.launch { repository.removeFromCart(item) }
    }

    fun clearCart() {
        val userId = currentUserId.value ?: return
        viewModelScope.launch { repository.clearCart(userId) }
    }
}
