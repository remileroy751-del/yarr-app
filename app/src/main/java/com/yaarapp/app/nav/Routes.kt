package com.yaarapp.app.nav

object Routes {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val SIGNUP = "signup"

    const val MARKETPLACE = "marketplace"          // "Acheter"
    const val PRODUCT_DETAIL = "product/{productId}"
    const val CART = "cart"

    const val MY_SHOP = "my_shop"                   // "Ma boutique"
    const val CREATE_SHOP = "create_shop"
    const val ADD_PRODUCT = "add_product"
    const val PLANS = "plans"

    const val PROFILE = "profile"                   // "Mon profil"

    fun productDetail(productId: Int) = "product/$productId"
}
