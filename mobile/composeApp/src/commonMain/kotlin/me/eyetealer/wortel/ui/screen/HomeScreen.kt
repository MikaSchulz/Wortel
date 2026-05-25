package me.eyetealer.wortel.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.eyetealer.wortel.data.UserState

@Composable
fun HomeScreen(
    onStart: (wordLength: Int) -> Unit,
    hasSavedGame: Boolean = false,
    onResume: () -> Unit = {},
    user: UserState = UserState.SignedOut,
    onSignOut: () -> Unit = {},
    onUpgradeToGoogle: () -> Unit = {},
) {
    var wordLength by remember { mutableStateOf(5) }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
        ) {
            UserBanner(
                user = user,
                onSignOut = onSignOut,
                onUpgrade = onUpgradeToGoogle,
            )

            Text(
                "Wortel",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                fontSize = 56.sp,
            )
            Text(
                "Errate das deutsche Wort.",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 16.sp,
            )

            if (hasSavedGame) {
                Button(onClick = onResume) {
                    Text("Spiel fortfahren", fontSize = 16.sp)
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("Wortlänge:", fontSize = 14.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(5, 6, 7).forEach { len ->
                        val selected = len == wordLength
                        Button(
                            onClick = { wordLength = len },
                            colors = if (selected) {
                                ButtonDefaults.buttonColors()
                            } else {
                                ButtonDefaults.outlinedButtonColors()
                            },
                        ) {
                            Text("$len")
                        }
                    }
                }
            }

            if (hasSavedGame) {
                OutlinedButton(onClick = { onStart(wordLength) }) {
                    Text("Neues Spiel", fontSize = 16.sp)
                }
            } else {
                Button(onClick = { onStart(wordLength) }) {
                    Text("Neues Spiel", fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
private fun UserBanner(
    user: UserState,
    onSignOut: () -> Unit,
    onUpgrade: () -> Unit,
) {
    if (!user.isAuthenticated) return
    Box(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (user.isAnonymous) {
                Text(
                    "Du spielst als Gast.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = onUpgrade) { Text("Mit Google anmelden") }
                    TextButton(onClick = onSignOut) { Text("Abmelden") }
                }
            } else {
                Text(
                    "Eingeloggt als ${user.displayName ?: user.email ?: "Spieler"}",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                )
                TextButton(onClick = onSignOut) { Text("Abmelden") }
            }
        }
    }
}
