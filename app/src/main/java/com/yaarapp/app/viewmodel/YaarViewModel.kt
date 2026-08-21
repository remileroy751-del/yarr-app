package com.yaarapp.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yaarapp.app.data.AddProductResult
import com.yaarapp.app.data.AuthResult
import com.yaarapp.app.data.CartItem
import com.yaarapp.app.data.Country
import com.yaarapp.app.data.Plan
import com.yaarapp.app.data.Product
import com.yaarapp.app.data.Sex
import com.yaarapp.app.data.Shop
import com.yaarapp.app.data.User
import com.yaarapp.app.data.YaarRepository
import com.yaarapp.app.util.PhoneFormat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class YaarViewModel(private val repository: YaarRepository) : ViewModel() {

    // ---------- Session ----------

    val currentUserId: StateFlow<Int?> =
        repository.session.currentUserId.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser

    init {
        viewModelScope.launch {
            currentUserId.collect { id ->
                _currentUser.value = if (id != null) repository.getUser(id) else null
            }
        }
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

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError

    /** @param localWhatsappNumber numéro tel que tapé par l'utilisateur, sans l'indicatif pays. */
    fun signUp(
        firstName: String,
        sex: Sex,
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
        if (!PhoneFormat.isValidLocalNumber(localWhatsappNumber)) {
            _authError.value = "Le numéro WhatsApp saisi semble incomplet."
            return
        }
        val whatsappNumber = PhoneFormat.formatWhatsapp(country, localWhatsappNumber)
        viewModelScope.launch {
            when (val result = repository.signUp(firstName, sex, country, city, whatsappNumber, password)) {
                is AuthResult.Success -> {
                    _authError.value = null
                    _currentUser.value = result.user
                    onDone()
                }
                is AuthResult.Error -> _authError.value = result.message
            }
        }
    }

    fun login(whatsappNumber: String, password: String, onDone: () -> Unit) {
        viewModelScope.launch {
            when (val result = repository.login(whatsappNumber, password)) {
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

    val categories: StateFlow<List<String>> =
        repository.observeCategories().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory

    val filteredProducts: StateFlow<List<Product>> =
        combine(allProducts, _selectedCategory) { products, category ->
            if (category == null) products else products.filter { it.category == category }
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
            if (user == null) matches else matches.filter { it.city == user.city }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Bouton "Afficher les produits disponibles dans d'autres villes". */
    private val _showOtherCitiesPicker = MutableStateFlow(false)
    val showOtherCitiesPicker: StateFlow<Boolean> = _showOtherCitiesPicker

    fun toggleOtherCitiesPicker() {
        _showOtherCitiesPicker.value = !_showOtherCitiesPicker.value
    }

    /** Villes (autres que la sienne) cochées par l'acheteur dans la liste. */
    private val _selectedOtherCities = MutableStateFlow<Set<String>>(emptySet())
    val selectedOtherCities: StateFlow<Set<String>> = _selectedOtherCities

    fun toggleOtherCity(city: String) {
        _selectedOtherCities.value = _selectedOtherCities.value.let {
            if (city in it) it - city else it + city
        }
    }

    /** Toutes les autres villes du pays de l'acheteur (liste complète, pas seulement celles avec résultats). */
    val otherCitiesInCountry: StateFlow<List<String>> =
        currentUser.map { user ->
            if (user == null) emptyList()
            else com.yaarapp.app.data.CityRepository.citiesFor(user.country).filter { it != user.city }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Résultats des villes cochées par l'acheteur dans "Afficher d'autres villes". */
    val searchResultsOtherCities: StateFlow<List<Product>> =
        combine(searchMatches, _selectedOtherCities) { matches, extraCities ->
            if (extraCities.isEmpty()) emptyList() else matches.filter { it.city in extraCities }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ---------- Ma boutique ----------

    @OptIn(ExperimentalCoroutinesApi::class)
    val myShop: StateFlow<Shop?> = currentUserId.flatMapLatest { id ->
        if (id == null) emptyFlow() else repository.observeMyShop(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val myShopProducts: StateFlow<List<Product>> = myShop.flatMapLatest { shop ->
        if (shop == null) emptyFlow() else repository.observeShopProducts(shop.id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _shopCreationError = MutableStateFlow<String?>(null)
    val shopCreationError: StateFlow<String?> = _shopCreationError

    fun createShop(name: String, whatsappNumber: String, onDone: () -> Unit) {
        val owner = _currentUser.value
        if (owner == null || name.isBlank() || whatsappNumber.isBlank()) {
            _shopCreationError.value = "Merci de renseigner le nom de la boutique et le numéro WhatsApp."
            return
        }
        viewModelScope.launch {
            repository.createShop(owner, name, whatsappNumber)
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
            val count = repository.deactivateExpiredProducts(shop.id)
            if (count > 0) _expiredNotice.value = count
        }
    }

    fun dismissExpiredNotice() {
        _expiredNotice.value = null
    }

    private val _addProductError = MutableStateFlow<String?>(null)
    val addProductError: StateFlow<String?> = _addProductError

    fun addProduct(
        name: String,
        description: String,
        price: Double,
        imageUrl: String,
        category: String,
        onSuccess: () -> Unit
    ) {
        val shop = myShop.value ?: return
        viewModelScope.launch {
            when (val result = repository.addProduct(shop, name, description, price, imageUrl, category)) {
                is AddProductResult.Success -> {
                    _addProductError.value = null
                    onSuccess()
                }
                is AddProductResult.LimitReached ->
                    _addProductError.value =
                        "Limite de ${result.plan.maxProducts} produits actifs atteinte pour le forfait ${result.plan.label}. " +
                            "Désactivez un produit ou consultez les forfaits disponibles."
                is AddProductResult.Error -> _addProductError.value = result.message
            }
        }
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
                    _addProductError.value =
                        "Impossible de remettre ce produit en vente : limite de ${result.plan.maxProducts} " +
                            "produits actifs atteinte pour le forfait ${result.plan.label}."
                else -> Unit
            }
        }
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
