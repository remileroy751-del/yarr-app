package com.yaarapp.app.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.yaarapp.app.data.CartItem
import com.yaarapp.app.data.Product
import com.yaarapp.app.data.Shop
import java.net.URLEncoder

object WhatsAppHelper {

    /** Commande directe d'un seul produit (bouton "Acheter" sur la fiche produit). */
    fun orderProduct(context: Context, product: Product, shop: Shop) {
        val message = "Bonjour, je souhaite commander le produit \"${product.name}\" " +
            "(${formatPrice(product.price)}) publié sur votre boutique ${shop.name} sur Yaar-App."
        openWhatsApp(context, shop.whatsappNumber, message)
    }

    /**
     * Commande du panier. Comme chaque produit peut venir d'une boutique différente,
     * on regroupe les articles par boutique et on envoie un message WhatsApp distinct
     * à chaque vendeur concerné.
     */
    fun orderCartGroupedByShop(context: Context, items: List<CartItem>) {
        if (items.isEmpty()) {
            Toast.makeText(context, "Votre panier est vide", Toast.LENGTH_SHORT).show()
            return
        }
        val byShop = items.groupBy { it.shopId }
        byShop.values.forEach { shopItems ->
            val shopName = shopItems.first().shopName
            val shopNumber = shopItems.first().shopWhatsappNumber
            val sb = StringBuilder()
            sb.append("Bonjour, je souhaite commander sur votre boutique $shopName (via Yaar-App) :\n\n")
            var total = 0.0
            shopItems.forEach { item ->
                val lineTotal = item.price * item.quantity
                total += lineTotal
                sb.append("• ${item.productName} x${item.quantity} - ${formatPrice(lineTotal)}\n")
            }
            sb.append("\nTotal : ${formatPrice(total)}")
            openWhatsApp(context, shopNumber, sb.toString())
        }
    }

    private fun formatPrice(value: Double): String = "${value.toLong()} FCFA"

    private fun openWhatsApp(context: Context, number: String, message: String) {
        val encoded = URLEncoder.encode(message, "UTF-8").replace("+", "%20")
        val url = "https://wa.me/$number?text=$encoded"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "WhatsApp n'est pas installé", Toast.LENGTH_SHORT).show()
        }
    }
}
