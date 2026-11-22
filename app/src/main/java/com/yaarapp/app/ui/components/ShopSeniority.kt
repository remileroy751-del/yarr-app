package com.yaarapp.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.yaarapp.app.data.Shop
import kotlinx.coroutines.delay

/**
 * "A rejoint Yaar-App depuis 2 semaines" : date du jour - date de création de la boutique,
 * recalculée chaque minute (voir [Shop.seniorityLabel]).
 */
@Composable
fun rememberShopSeniorityLabel(shop: Shop): String {
    var now by remember(shop.createdAt) { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(shop.createdAt) {
        while (true) {
            now = System.currentTimeMillis()
            delay(60_000L)
        }
    }
    return shop.seniorityLabel(now)
}
