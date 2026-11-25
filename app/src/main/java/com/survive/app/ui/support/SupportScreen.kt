package com.survive.app.ui.support

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.survive.app.Links
import com.survive.app.ui.components.BackTopBar
import com.survive.app.ui.theme.LocalNightRed

private val PayPalBlue = Color(0xFF0070BA)

/** Page « Soutenir l'application » : don libre via PayPal, ouvert dans le navigateur. */
@Composable
fun SupportScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val night = LocalNightRed.current
    var failed by remember { mutableStateOf(false) }

    Scaffold(topBar = { BackTopBar("Soutenir l'application", onBack) }) { padding ->
        Column(
            Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
        ) {
            Card(
                border = BorderStroke(2.dp, MaterialTheme.colorScheme.secondary),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    Modifier.padding(20.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Icon(
                        Icons.Filled.Favorite, contentDescription = null, modifier = Modifier.size(48.dp),
                        tint = if (night) MaterialTheme.colorScheme.primary else Color(0xFFE53935),
                    )
                    Text("Merci d'utiliser Survive !", style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Text(
                        "L'application est gratuite, sans publicité, sans compte, et fonctionne entièrement sans Internet. " +
                            "Si elle vous est utile, vous pouvez soutenir son développement (nouvelles fiches, illustrations, " +
                            "traductions, nouveaux outils) par un don.",
                        style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center,
                    )
                    Button(
                        onClick = {
                            failed = try {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(Links.DONATION_URL)))
                                false
                            } catch (e: ActivityNotFoundException) {
                                true
                            }
                        },
                        colors = if (night) ButtonDefaults.buttonColors()
                        else ButtonDefaults.buttonColors(containerColor = PayPalBlue, contentColor = Color.White),
                        modifier = Modifier.fillMaxWidth().height(60.dp),
                    ) {
                        Text("Faire un don via PayPal", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    if (failed) {
                        Text("Aucun navigateur trouvé sur ce téléphone. Adresse : ${Links.DONATION_URL}",
                            color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                    }
                    Text(
                        "Le don se fait sur le site de PayPal, du montant de votre choix, et nécessite une connexion Internet. " +
                            "L'application ne reçoit aucune information de paiement.",
                        style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    )
                }
            }
        }
    }
}
