package com.yaarapp.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
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

    val products by viewModel.myShopProducts.collectAsStateWithLifecycle()
    val maxProducts = shop!!.plan.maxProducts

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Ma boutique — ${shop!!.name}", fontWeight = FontWeight.Bold) })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                if (products.size >= maxProducts) {
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
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "${products.size} / $maxProducts produits utilisés (forfait ${shop!!.plan.label})",
                    style = MaterialTheme.typography.bodyMedium
                )
                LinearProgressIndicator(
                    progress = { (products.size.toFloat() / maxProducts).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp)
                )
                if (products.size >= maxProducts) {
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
                            onDelete = { viewModel.deleteProduct(product) }
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
            "Chaque compte peut ouvrir une boutique et publier jusqu'à 10 produits gratuitement.",
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
private fun MyProductCard(product: Product, onDelete: () -> Unit) {
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
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .background(MaterialTheme.colorScheme.error, RoundedCornerShape(50))
                ) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "Supprimer",
                        tint = androidx.compose.ui.graphics.Color.White
                    )
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
            }
        }
    }
}
