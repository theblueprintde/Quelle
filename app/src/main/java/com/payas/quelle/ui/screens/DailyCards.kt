package com.payas.quelle.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.payas.quelle.data.DailyGoal
import com.payas.quelle.data.Song
import com.payas.quelle.ui.theme.Green
import com.payas.quelle.ui.theme.GreenDeep
import com.payas.quelle.ui.theme.GreenLight
import com.payas.quelle.ui.theme.InkMuted

/** Günün tek ana hedefi: yaz, kaydet, bitince işaretle. */
@Composable
fun GoalCard(goal: DailyGoal?, date: String, onSave: (DailyGoal?) -> Unit) {
    var editing by remember(goal) { mutableStateOf(goal == null) }
    var text by remember(goal) { mutableStateOf(goal?.text ?: "") }

    QuelleCard("Günün hedefi", Modifier.fillMaxWidth()) {
        if (editing || goal == null) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text("Bugün neyi başarmak istiyorsun?") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    onSave(if (text.isBlank()) null else DailyGoal(date, text.trim(), goal?.done ?: false))
                    editing = false
                }),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(30.dp)
                        .background(if (goal.done) Color(0xFF3E7D52) else GreenLight, RoundedCornerShape(10.dp))
                        .clickable(role = Role.Checkbox, onClickLabel = "Hedefi işaretle") { onSave(goal.copy(done = !goal.done)) },
                    contentAlignment = Alignment.Center,
                ) {
                    if (goal.done) Icon(Icons.Filled.Check, contentDescription = "Tamamlandı", tint = Color.White, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    goal.text,
                    modifier = Modifier.weight(1f),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textDecoration = if (goal.done) TextDecoration.LineThrough else null,
                )
                IconButton(onClick = { editing = true }) {
                    Icon(Icons.Outlined.Edit, contentDescription = "Hedefi düzenle", tint = InkMuted)
                }
            }
        }
    }
}

/** Günün şarkısı ve YouTube Music'te açma düğmesi. */
@Composable
fun SongCard(song: Song) {
    val context = LocalContext.current
    QuelleCard("Günün şarkısı", Modifier.fillMaxWidth(), container = GreenLight) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(Modifier.size(52.dp).background(Green, RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.MusicNote, contentDescription = null, tint = Color.White)
            }
            Column(Modifier.weight(1f)) {
                Text(song.title, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
                Text(song.artist, fontWeight = FontWeight.SemiBold, color = GreenDeep)
            }
        }
        Button(
            onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(song.youtubeMusicUrl))) },
            colors = ButtonDefaults.buttonColors(containerColor = GreenDeep),
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        ) {
            Icon(Icons.Filled.PlayArrow, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text("YouTube Music'te aç", fontWeight = FontWeight.ExtraBold)
        }
    }
}
