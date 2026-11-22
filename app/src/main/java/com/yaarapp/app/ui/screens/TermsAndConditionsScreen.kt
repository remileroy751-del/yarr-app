package com.yaarapp.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yaarapp.app.viewmodel.YaarViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TermsAndConditionsScreen(
    viewModel: YaarViewModel,
    onAccepted: () -> Unit
) {
    var accepted by remember { mutableStateOf(false) }
    val scroll = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Conditions d'utilisation", fontWeight = FontWeight.Bold) })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scroll)
                    .padding(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Bienvenue sur Yaar-App",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Veuillez lire attentivement les présentes conditions avant de commencer à utiliser l'application.",
                    style = MaterialTheme.typography.bodyMedium
                )

                TermsSection("1. Objet de Yaar-App",
                    "Yaar-App est une application de vente et d'achat en ligne conçue, dans un premier temps, pour faciliter la mise en relation entre vendeurs et acheteurs de biens et produits physiques. L'application permet notamment de présenter des produits, des boutiques et de faciliter les échanges entre utilisateurs.")

                TermsSection("2. Inscription et informations exactes",
                    "Chaque utilisateur doit créer un seul compte avec des informations réelles et exactes, notamment son nom complet et son numéro de téléphone. Pour profiter pleinement des fonctionnalités de Yaar-App, le compte doit être associé à un numéro de téléphone WhatsApp valide. L'utilisateur est responsable de la confidentialité de ses identifiants et de l'utilisation de son compte.")

                TermsSection("3. Utilisation responsable",
                    "Tout utilisateur s'engage à utiliser Yaar-App de manière responsable, loyale et conforme aux lois applicables. Il lui est notamment demandé de ne pas publier d'informations trompeuses, de ne pas usurper l'identité d'une autre personne et de ne pas utiliser l'application pour des activités frauduleuses ou abusives. Yaar-App se réserve le droit de suspendre ou de supprimer tout compte qui ne respecte pas les présentes conditions ou les règles de la plateforme.")

                TermsSection("4. Rôle de Yaar-App dans les ventes",
                    "Yaar-App ne gère pas directement les ventes ni les paiements entre acheteurs et vendeurs. Son rôle est principalement de mettre les utilisateurs en relation afin qu'ils puissent discuter et convenir des modalités de la transaction. Sauf accord différent entre les parties, le client règle directement le vendeur selon les modalités convenues, notamment lors de la réception physique du produit acheté. Yaar-App n'encaisse pas le prix des produits vendus entre utilisateurs.")

                TermsSection("5. Produits, stocks et responsabilité des vendeurs",
                    "Yaar-App ne gère aucun stock physique. Le vendeur est seul responsable de la disponibilité, de la description, de l'état, de la qualité, de la conformité et de la livraison de ses produits. Yaar-App ne garantit pas qu'un produit présenté par un utilisateur est disponible, conforme à sa description ou adapté aux attentes de l'acheteur. Les utilisateurs doivent vérifier les informations du produit et convenir des conditions de la vente avant toute transaction.")

                TermsSection("6. Échanges entre utilisateurs",
                    "Les discussions intégrées à Yaar-App et les moyens de contact proposés servent à faciliter les échanges entre les parties. Chaque utilisateur reste responsable des informations et engagements qu'il communique à un autre utilisateur. En cas de litige concernant une vente, les parties doivent rechercher directement une solution conformément aux règles et lois applicables.")

                TermsSection("7. Suppression du compte",
                    "Un utilisateur peut demander la suppression définitive de son compte depuis l'application. La suppression entraîne la suppression des données associées au compte qui sont conservées par Yaar-App, dans les limites techniques et légales applicables. L'utilisateur reconnaît que cette suppression peut entraîner la perte définitive de ses informations, de sa boutique, de ses produits et de ses avantages ou forfaits en cours, notamment les périodes de certification ou autres services payants non consommés.")

                TermsSection("8. Mode gratuit, abonnement par clé d'activation et certification",
                    "Yaar-App reste libre d'utilisation, avec des limites. En mode gratuit, un utilisateur peut publier au maximum 5 produits actifs dans sa boutique, 5 annonces immobilières actives, et rendre chaque produit visible dans sa ville ainsi que dans 5 autres villes de son pays au maximum. " +
                        "Un paiement unique permet de débloquer tous les avantages pendant 30 jours : jusqu'à 25 produits actifs, 20 annonces immobilières actives, la publication dans toutes les villes de son pays et le badge « boutique certifiée ». " +
                        "Ce paiement s'effectue sur la boutique en ligne Chariow dont le lien est affiché dans l'application, et donne droit à une clé d'activation (4 lettres, un espace, 4 chiffres, par exemple « ABCD 1234 »). La clé est saisie depuis l'application (menu Profil, bouton « Certifier mon compte », ou lorsqu'une limite du mode gratuit est atteinte). Aucune autre plateforme de paiement n'est nécessaire pour cet abonnement. " +
                        "Chaque clé n'est utilisable qu'une seule fois et active l'abonnement pendant 30 jours à compter de sa saisie. Si une clé est saisie alors qu'un abonnement est déjà en cours, les 30 jours s'ajoutent à la date de fin de l'abonnement actuel. " +
                        "Une fois la période écoulée, les avantages sont automatiquement perdus et la boutique revient au mode gratuit ; les publications au-delà des limites du mode gratuit ne peuvent plus être remises en vente tant que le nombre autorisé n'est pas respecté. " +
                        "Une clé achetée ne peut être ni remboursée, ni échangée, ni revendue par l'utilisateur. " +
                        "Le prix de la clé d'activation, actuellement de 1 500 FCFA pour 30 jours, peut évoluer avec le temps, notamment en fonction de la popularité de l'application ; le prix applicable est celui affiché sur la boutique de clés au moment de l'achat. " +
                        "Les autres services payants éventuels de Yaar-App, comme la promotion de produits, sont traités via le prestataire de paiement affiché au moment du règlement.")

                TermsSection("9. Acceptation",
                    "En cochant la case ci-dessous et en appuyant sur « Continuer », l'utilisateur confirme avoir lu, compris et accepté les présentes conditions d'utilisation. Il peut ensuite poursuivre la création de son compte en sélectionnant son pays et sa ville.")
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(checked = accepted, onCheckedChange = { accepted = it })
                Text(
                    "J'accepte les conditions d'utilisation",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }

            Button(
                onClick = { viewModel.acceptTerms(onAccepted) },
                enabled = accepted,
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                Text("Continuer")
            }
        }
    }
}

@Composable
private fun TermsSection(title: String, body: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(body, style = MaterialTheme.typography.bodyMedium)
    }
}
