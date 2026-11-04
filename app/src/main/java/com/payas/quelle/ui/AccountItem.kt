package com.payas.quelle.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.payas.quelle.QuelleApplication
import com.payas.quelle.sync.SignInResult
import com.payas.quelle.sync.SyncStatus
import com.payas.quelle.sync.signInWithGoogle
import com.payas.quelle.sync.signOut
import com.payas.quelle.ui.theme.InkMuted
import kotlinx.coroutines.launch

/** Yan menünün altındaki hesap düğmesi: giriş yap / senkron durumu / çıkış. */
@Composable
fun AccountItem() {
    val context = LocalContext.current
    val sync = (context.applicationContext as QuelleApplication).sync
    val user by sync.user.collectAsState()
    val status by sync.status.collectAsState()
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var showDialog by remember { mutableStateOf(false) }

    val icon = when {
        user == null -> Icons.Outlined.AccountCircle
        status == SyncStatus.Error -> Icons.Outlined.CloudOff
        status == SyncStatus.Syncing -> Icons.Outlined.CloudSync
        else -> Icons.Outlined.CloudDone
    }

    NavigationRailItem(
        selected = false,
        enabled = !busy,
        onClick = {
            if (user != null) {
                showDialog = true
            } else {
                busy = true
                scope.launch {
                    when (val result = signInWithGoogle(context)) {
                        is SignInResult.Failed -> Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                        SignInResult.Success -> Toast.makeText(context, "Giriş yapıldı, verilerin eşitleniyor.", Toast.LENGTH_SHORT).show()
                        SignInResult.Cancelled -> Unit
                    }
                    busy = false
                }
            }
        },
        icon = { Icon(icon, contentDescription = null) },
        label = { Text(if (user == null) "Giriş yap" else "Senkron") },
    )

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Google hesabı", fontWeight = FontWeight.ExtraBold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(user?.email ?: "", fontWeight = FontWeight.Bold)
                    Text(status.label, color = InkMuted)
                    Text(
                        "Verilerin bu hesapla tüm cihazlarında aynı kalır.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            },
            confirmButton = { TextButton(onClick = { showDialog = false }) { Text("Tamam") } },
            dismissButton = {
                TextButton(onClick = {
                    showDialog = false
                    scope.launch { signOut(context) }
                }) { Text("Çıkış yap", color = MaterialTheme.colorScheme.error) }
            },
        )
    }
}
