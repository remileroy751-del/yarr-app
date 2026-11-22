package com.yaarapp.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yaarapp.app.data.LicenseConfig
import com.yaarapp.app.data.ShopLimits
import com.yaarapp.app.viewmodel.YaarViewModel

/**
 * Fenêtre « Entrez votre clé d'activation ».
 *
 * Affichée une seule fois à la racine de la navigation ; n'importe quel écran l'ouvre avec
 * [YaarViewModel.showActivationPrompt] : bouton « Certifier mon compte » du Profil, limite de
 * 5 produits atteinte, limite de 5 annonces immobilières atteinte.
 */
@Composable
fun ActivationKeyDialog(viewModel: YaarViewModel) {
    val reason by viewModel.activationPrompt.collectAsStateWithLifecycle()
    val state by viewModel.activationState.collectAsStateWithLifecycle()
    val shop by viewModel.myShop.collectAsStateWithLifecycle()
    var code by remember { mutableStateOf("") }
    val uriHandler = LocalUriHandler.current

    val currentReason = reason ?: return
    val working = state is YaarViewModel.ActivationState.Working
    val success = state as? YaarViewModel.ActivationState.Success
    val errorMessage = (state as? YaarViewModel.ActivationState.Error)?.message
    val activeUntil = shop?.takeIf { it.isCertifiedNow() }?.certificationExpiresAt

    val title = when (currentReason) {
        YaarViewModel.ActivationReason.PRODUCT_LIMIT -> "Limite de produits atteinte"
        YaarViewModel.ActivationReason.IMMO_LIMIT -> "Limite d'annonces atteinte"
        YaarViewModel.ActivationReason.CERTIFY -> "Certifier mon compte"
    }

    val dismiss = {
        code = ""
        viewModel.dismissActivationPrompt()
    }

    AlertDialog(
        onDismissRequest = { if (!working) dismiss() },
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (success != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                        Text(
                            "Clé activée avec succès !",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                    Text("+${success.daysAdded} jours ajoutés à votre boutique.")
                    Text(
                        "Votre abonnement est actif jusqu'au ${formatCertificationDate(success.expiresAt)}.",
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "Vous pouvez maintenant publier jusqu'à ${ShopLimits.PREMIUM_PRODUCTS} produits et ${ShopLimits.PREMIUM_IMMO_LISTINGS} annonces immobilières, dans toutes les villes de votre pays, avec le badge certifié.",
                        style = MaterialTheme.typography.bodySmall
                    )
                } else {
                    Text(
                        when (currentReason) {
                            YaarViewModel.ActivationReason.PRODUCT_LIMIT ->
                                "Le mode gratuit permet ${ShopLimits.FREE_PRODUCTS} produits actifs au maximum. Entrez une clé d'activation pour passer à ${ShopLimits.PREMIUM_PRODUCTS} produits."
                            YaarViewModel.ActivationReason.IMMO_LIMIT ->
                                "Le mode gratuit permet ${ShopLimits.FREE_IMMO_LISTINGS} annonces immobilières actives au maximum. Entrez une clé d'activation pour passer à ${ShopLimits.PREMIUM_IMMO_LISTINGS} annonces."
                            YaarViewModel.ActivationReason.CERTIFY ->
                                "Entrez votre clé d'activation pour certifier votre compte et débloquer tous les avantages pendant ${LicenseConfig.DURATION_DAYS} jours."
                        }
                    )
                    Text(
                        "Avec une clé (${LicenseConfig.DURATION_DAYS} jours) :\n" +
                            "• ${ShopLimits.PREMIUM_PRODUCTS} produits actifs\n" +
                            "• ${ShopLimits.PREMIUM_IMMO_LISTINGS} annonces immobilières\n" +
                            "• publication dans toutes les villes de votre pays\n" +
                            "• badge « boutique certifiée »",
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (activeUntil != null) {
                        Text(
                            "Abonnement actif jusqu'au ${formatCertificationDate(activeUntil)}. Les ${LicenseConfig.DURATION_DAYS} jours de la nouvelle clé s'ajoutent automatiquement à cette date.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    OutlinedButton(
                        onClick = { runCatching { uriHandler.openUri(LicenseConfig.KEY_STORE_URL) } },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text("  Télécharger une clé d'activation", fontWeight = FontWeight.SemiBold)
                    }
                    Text(
                        "Tarif actuel : ${formatFcfa(LicenseConfig.PRICE_FCFA)} FCFA pour ${LicenseConfig.DURATION_DAYS} jours (susceptible d'évoluer).",
                        style = MaterialTheme.typography.labelSmall
                    )
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = LicenseConfig.formatTyping(it) },
                        label = { Text("Clé d'activation") },
                        placeholder = { Text(LicenseConfig.KEY_EXAMPLE) },
                        singleLine = true,
                        enabled = !working,
                        isError = errorMessage != null,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (errorMessage != null) {
                        Text(errorMessage, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        },
        confirmButton = {
            if (success != null) {
                Button(onClick = dismiss) { Text("Fermer") }
            } else {
                Button(
                    onClick = { viewModel.redeemActivationKey(code) },
                    enabled = !working && code.length == 9
                ) {
                    if (working) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Activer")
                    }
                }
            }
        },
        dismissButton = {
            if (success == null) {
                TextButton(onClick = dismiss, enabled = !working) { Text("Plus tard") }
            }
        }
    )
}

private fun formatFcfa(amount: Int): String =
    java.text.NumberFormat.getIntegerInstance(java.util.Locale.FRANCE).format(amount).replace('\u202f', ' ').replace('\u00a0', ' ')
