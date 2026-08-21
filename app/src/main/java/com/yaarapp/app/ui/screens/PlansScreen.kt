package com.yaarapp.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yaarapp.app.data.Plan

private data class PlanInfo(val plan: Plan, val priceLabel: String, val description: String)

private val plansCatalog = listOf(
    PlanInfo(Plan.GRATUIT, "0 FCFA", "Jusqu'à 5 produits actifs en ligne. Idéal pour démarrer."),
    PlanInfo(Plan.STANDARD, "À définir / mois", "Jusqu'à 30 produits en ligne, pour les boutiques en croissance."),
    PlanInfo(Plan.PRO, "À définir / mois", "Jusqu'à 100 produits en ligne, pour les grossistes et grandes boutiques.")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlansScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Forfaits boutique") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(padding)) {
            Text(
                "La souscription aux forfaits payants (paiement Mobile Money, etc.) sera activée dans une prochaine mise à jour. En attendant, voici un aperçu des forfaits prévus :",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(16.dp)
            )
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(plansCatalog) { info ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)) {
                            Text(
                                info.plan.label,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                info.priceLabel,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                            Text(
                                info.description,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                            Row2("Jusqu'à ${info.plan.maxProducts} produits en ligne")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Row2(text: String) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.padding(top = 8.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        Icon(
            Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(end = 6.dp)
        )
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}
