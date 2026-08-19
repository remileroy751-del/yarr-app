package com.yaarapp.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yaarapp.app.data.AddProductResult
import com.yaarapp.app.data.AuthResult
import com.yaarapp.app.data.CartItem
import com.yaarapp.app.data.Plan
import com.yaarapp.app.data.Product
import com.yaarapp.app.data.Shop
import com.yaarapp.app.data.User
import com.yaarapp.app.data.YaarRepository
import kotlinx.coroutines.flow.Flow
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

    fun refreshCurrentUser() {
        viewModelScope.launch {
            val id = currentUserId.value
            _currentUser.value = if (id != null) repository.getUser(id) else null
        }
    }

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError

    fun signUp(fullName: String, phone: String, password: String, whatsappNumber: String, onDone: () -> Unit) {
        viewModelScope.launch {
            when (val result = repository.signUp(fullName, phone, password, whatsappNumber)) {
                is AuthResult.Success -> {
                    _authError.value = null
                    _currentUser.value = result.user
                    onDone()
                }
                is AuthResult.Error -> _authError.value = result.message
            }
        }
    }

    fun login(phone: String, password: String, onDone: () -> Unit) {
        viewModelScope.launch {
            when (val result = repository.login(phone, password)) {
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

    // ---------- Ma boutique ----------

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val myShop: StateFlow<Shop?> = currentUserId.flatMapLatest { id ->
        if (id == null) emptyFlow() else repository.observeMyShop(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val myShopProducts: StateFlow<List<Product>> = myShop.flatMapLatest { shop ->
        if (shop == null) emptyFlow() else repository.observeShopProducts(shop.id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _shopCreationError = MutableStateFlow<String?>(null)
    val shopCreationError: StateFlow<String?> = _shopCreationError

    fun createShop(name: String, whatsappNumber: String, onDone: () -> Unit) {
        val ownerId = currentUserId.value ?: return
        if (name.isBlank() || whatsappNumber.isBlank()) {
            _shopCreationError.value = "Merci de renseigner le nom de la boutique et le numéro WhatsApp."
            return
        }
        viewModelScope.launch {
            repository.createShop(ownerId, name, whatsappNumber)
            _shopCreationError.value = null
            onDone()
        }
    }

    private val _addProductError = MutableStateFlow<String?>(null)
    val addProductError: StateFlow<String?> = _addProductError

    private val _limitReached = MutableStateFlow<Plan?>(null)
    val limitReached: StateFlow<Plan?> = _limitReached

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
                    _limitReached.value = null
                    onSuccess()
                }
                is AddProductResult.LimitReached -> _limitReached.value = result.plan
                is AddProductResult.Error -> _addProductError.value = result.message
            }
        }
    }

    fun clearAddProductMessages() {
        _addProductError.value = null
        _limitReached.value = null
    }

    fun deleteProduct(product: Product) {
        viewModelScope.launch { repository.deleteProduct(product) }
    }

    // ---------- Panier ----------

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
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
