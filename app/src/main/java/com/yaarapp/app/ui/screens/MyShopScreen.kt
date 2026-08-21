package com.yaarapp.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.yaarapp.app.data.FREE_LISTING_DURATION_DAYS
import com.yaarapp.app.data.Product
import com.yaarapp.app.util.ImageStorage
import com.yaarapp.app.viewmodel.YaarViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyShopScreen(
    viewModel: YaarViewModel,
    onAddProduct: () -> Unit,
    onSeePlans: () -> Unit
) {
    val shop by viewModel.myShop.collectAsStateWithLifecycle()
    val error by viewModel.shopCreationError.collectAsStateWithLifecycle()

    if (shop == null) {
        CreateShopForm(error = error) { name, whatsapp ->
            viewModel.createShop(name, whatsapp) {}
        }
        return
    }

    // Vérifie, à chaque ouverture de la boutique, si des produits ont dépassé
    // les 14 jours d'exposition gratuite et doivent être désactivés.
    LaunchedEffect(shop!!.id) {
        viewModel.checkShopExpirations()
    }

    val products by viewModel.myShopProducts.collectAsStateWithLifecycle()
    val expiredNotice by viewModel.expiredNotice.collectAsStateWithLifecycle()
    val maxProducts = shop!!.plan.maxProducts
    val activeCount = products.count { it.isActive }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Ma boutique — ${shop!!.name}", fontWeight = FontWeight.Bold) })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                if (activeCount >= maxProducts) {
                    onSeePlans()
                } else {
                    onAddProduct()
                }
            }) {
                Icon(Icons.Filled.Add, contentDescription = "Ajouter un produit")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (expiredNotice != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.Notifications,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "$expiredNotice produit(s) désactivé(s) automatiquement",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                "Ces produits sont en ligne depuis plus de $FREE_LISTING_DURATION_DAYS jours. Vérifiez votre boutique : remettez en vente ceux encore disponibles, ou supprimez ceux déjà vendus.",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    TextButton(
                        onClick = { viewModel.dismissExpiredNotice() },
                        modifier = Modifier.align(Alignment.End).padding(end = 8.dp, bottom = 4.dp)
                    ) {
                        Text("Compris")
                    }
                }
            }

            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "$activeCount / $maxProducts produits actifs (forfait ${shop!!.plan.label})",
                    style = MaterialTheme.typography.bodyMedium
                )
                LinearProgressIndicator(
                    progress = { (activeCount.toFloat() / maxProducts).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp)
                )
                if (activeCount >= maxProducts) {
                    TextButton(onClick = onSeePlans, modifier = Modifier.padding(top = 4.dp)) {
                        Text("Limite atteinte — voir les forfaits disponibles")
                    }
                }
            }

            if (products.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Filled.Storefront,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        "Aucun produit publié pour le moment. Appuyez sur + pour ajouter votre premier article.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(products, key = { it.id }) { product ->
                        MyProductCard(
                            product = product,
                            onDelete = { viewModel.deleteProduct(product) },
                            onDeactivate = { viewModel.deactivateProduct(product) },
                            onRepublish = { viewModel.republishProduct(product) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CreateShopForm(
    error: String?,
    onCreate: (name: String, whatsapp: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var whatsapp by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Filled.Storefront,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            "Créer ma boutique",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 12.dp, bottom = 20.dp)
        )
        Text(
            "Chaque compte peut ouvrir une boutique et publier jusqu'à 5 produits gratuitement (au-delà, désactivez ou remettez en vente vos articles pour continuer à publier).",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Nom de la boutique") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = whatsapp,
            onValueChange = { whatsapp = it },
            label = { Text("Numéro WhatsApp de la boutique") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        )
        if (error != null) {
            Text(
                error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        Button(
            onClick = { onCreate(name.trim(), whatsapp.trim()) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("Créer ma boutique")
        }
    }
}

@Composable
private fun MyProductCard(
    product: Product,
    onDelete: () -> Unit,
    onDeactivate: () -> Unit,
    onRepublish: () -> Unit
) {
    val context = LocalContext.current
    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Box {
                AsyncImage(
                    model = ImageStorage.resolveImageModel(context, product.imageUrl),
                    contentDescription = product.name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentScale = ContentScale.Crop
                )
                if (!product.isActive) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(6.dp)
                            .background(MaterialTheme.colorScheme.error, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("Désactivé", color = Color.White, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    "${product.price.toLong()} FCFA",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    product.name,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1
                )
                Text(
                    "Disponible à ${product.city}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )

                if (product.isActive) {
                    // Produit publié en ligne : Désactiver / Supprimer définitivement
                    OutlinedButton(
                        onClick = onDeactivate,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                    ) {
                        Text("Désactiver le produit", style = MaterialTheme.typography.labelMedium)
                    }
                    TextButton(
                        onClick = onDelete,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Supprimer définitivement",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                } else {
                    // Produit désactivé : Remettre en vente / Supprimer
                    Button(
                        onClick = onRepublish,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                    ) {
                        Text("Remettre en vente", style = MaterialTheme.typography.labelMedium)
                    }
                    TextButton(
                        onClick = onDelete,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Supprimer",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }
    }
}
