package com.payas.quelle.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import com.payas.quelle.ui.screens.rememberStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.payas.quelle.ui.screens.AdminScreen
import com.payas.quelle.ui.screens.CalendarScreen
import com.payas.quelle.ui.screens.FocusScreen
import com.payas.quelle.ui.screens.LessonsScreen
import com.payas.quelle.ui.screens.TodayScreen
import com.payas.quelle.ui.screens.WeekScreen
import com.payas.quelle.ui.theme.*

enum class Destination(val label: String, val icon: ImageVector) {
    Today("Bugün", Icons.Outlined.WbSunny),
    Calendar("Takvim", Icons.Outlined.CalendarMonth),
    Focus("Odak", Icons.Outlined.Timer),
    Lessons("Dersler", Icons.Outlined.School),
    Week("Hafta", Icons.Outlined.BarChart),
    Admin("Yönetim", Icons.Outlined.AdminPanelSettings),
}

/** Üstteki ana ekranlar; yönetim paneli menünün altında, hesabın yanında durur. */
private val mainDestinations = Destination.entries.filter { it != Destination.Admin }

@Composable
fun QuelleApp() {
    var current by rememberSaveable { mutableStateOf(Destination.Today) }

    // Odak zamanlayıcısı hangi ekranda olunursa olsun süresi dolunca kapanır ve kaydedilir.
    val store = rememberStore()
    val timer by remember { store.data.map { it.timer }.distinctUntilChanged() }.collectAsState(initial = store.data.value.timer)
    LaunchedEffect(timer) {
        val t = timer ?: return@LaunchedEffect
        if (t.isPaused) return@LaunchedEffect
        delay((t.endAt - System.currentTimeMillis()).coerceAtLeast(0) + 200)
        store.completeTimerIfDone(LocalDate.now().toString())
    }

    Row(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
    ) {
        NavigationRail(
            containerColor = Midnight,
            header = {
                Box(
                    Modifier
                        .padding(top = 12.dp)
                        .size(52.dp)
                        .border(1.dp, GoldBright, RoundedCornerShape(18.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.WaterDrop, contentDescription = "Quelle", tint = GoldBright)
                }
                Spacer(Modifier.height(16.dp))
            },
        ) {
            mainDestinations.forEach { dest ->
                NavigationRailItem(
                    selected = current == dest,
                    onClick = { current = dest },
                    icon = { Icon(dest.icon, contentDescription = null) },
                    label = { Text(dest.label) },
                    colors = railItemColors(),
                )
            }
            Spacer(Modifier.weight(1f))
            NavigationRailItem(
                selected = current == Destination.Admin,
                onClick = { current = Destination.Admin },
                icon = { Icon(Destination.Admin.icon, contentDescription = null) },
                label = { Text(Destination.Admin.label) },
                colors = railItemColors(),
            )
            AccountItem()
            Spacer(Modifier.height(12.dp))
        }

        Box(Modifier.fillMaxSize().padding(horizontal = 32.dp, vertical = 28.dp)) {
            when (current) {
                Destination.Today -> TodayScreen()
                Destination.Calendar -> CalendarScreen()
                Destination.Focus -> FocusScreen()
                Destination.Lessons -> LessonsScreen()
                Destination.Week -> WeekScreen()
                Destination.Admin -> AdminScreen()
            }
        }
    }
}

/** Gece mavisi menüde altın vurgulu öğe renkleri. */
@Composable
fun railItemColors() = NavigationRailItemDefaults.colors(
    indicatorColor = MidnightSoft,
    selectedIconColor = GoldBright,
    selectedTextColor = GoldBright,
    unselectedIconColor = Color(0xFFA9AEBB),
    unselectedTextColor = Color(0xFFA9AEBB),
    disabledIconColor = Color(0xFF5E6577),
    disabledTextColor = Color(0xFF5E6577),
)
