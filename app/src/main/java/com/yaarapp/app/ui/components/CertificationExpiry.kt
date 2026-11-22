package com.yaarapp.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yaarapp.app.data.LicenseConfig
import com.yaarapp.app.data.Shop
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val DAY_MS = 24L * 60L * 60L * 1000L

fun formatCertificationDate(millis: Long): String =
    SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE).format(Date(millis))

/** Heure courante qui avance chaque seconde (sert au compte à rebours). */
@Composable
fun rememberTickingNow(): Long {
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1_000L)
        }
    }
    return now
}

/**
 * Carte "Boutique certifiée" : date d'activation, date d'expiration et compte à rebours
 * (jours restants, puis heures/minutes/secondes). Visible dans le profil d'une boutique certifiée.
 */
@Composable
fun CertificationExpiryCard(shop: Shop, modifier: Modifier = Modifier) {
    val expiresAt = shop.certificationExpiresAt ?: return
    val now = rememberTickingNow()
    val remaining = (expiresAt - now).coerceAtLeast(0L)
    val days = remaining / DAY_MS
    val hours = (remaining % DAY_MS) / 3_600_000L
    val minutes = (remaining % 3_600_000L) / 60_000L
    val seconds = (remaining % 60_000L) / 1_000L
    val totalMs = LicenseConfig.DURATION_DAYS * DAY_MS
    val fraction = (remaining.toFloat() / totalMs.toFloat()).coerceIn(0f, 1f)
    val urgent = days < 5

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (urgent) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.VerifiedUser, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                Text("Abonnement actif · Boutique certifiée", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 8.dp))
            }
            shop.certificationActivatedAt?.let {
                Text("Activé le ${formatCertificationDate(it)}", style = MaterialTheme.typography.bodySmall)
            }
            Text("Expire le ${formatCertificationDate(expiresAt)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(
                if (days >= 1) "Il reste $days ${if (days == 1L) "jour" else "jours"}" else "Dernier jour d'abonnement",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = if (urgent) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 4.dp)
            )
            Text(
                String.format(Locale.FRANCE, "%02d h %02d min %02d s", hours, minutes, seconds),
                style = MaterialTheme.typography.bodyMedium
            )
            LinearProgressIndicator(
                progress = { fraction },
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
            )
        }
    }
}
