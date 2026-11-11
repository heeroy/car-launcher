package tw.camry.launcher.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import tw.camry.launcher.LauncherViewModel
import tw.camry.launcher.Screen
import tw.camry.launcher.data.Slots

@Composable
fun HomeScreen(vm: LauncherViewModel) {
    val c = LocalCamry.current
    Row(Modifier.fillMaxSize().padding(start = 24.dp, end = 24.dp, bottom = 24.dp, top = StatusBarHeight), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
        NavigationCard(vm, Modifier.weight(1.45f).fillMaxHeight())
        Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            MiniPlayer(vm, Modifier.weight(1f).fillMaxWidth())
            FavoritesPanel(vm, Modifier.weight(1f).fillMaxWidth())
        }
    }
}

@Composable
private fun NavigationCard(vm: LauncherViewModel, modifier: Modifier) {
    val c = LocalCamry.current
    val nav = vm.appFor(Slots.NAV)
    Panel(modifier, onClick = { vm.open(Slots.NAV) }) {
        Column(Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Navigation, null, Modifier.size(40.dp), tint = c.accent)
                Spacer(Modifier.width(14.dp))
                Text("導航", color = c.text, fontSize = 32.sp, fontWeight = FontWeight.Medium)
            }
            Column {
                if (nav != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(nav.icon, nav.label, Modifier.size(72.dp))
                        Spacer(Modifier.width(18.dp))
                        Column {
                            Text(nav.label, color = c.text, fontSize = 28.sp, fontWeight = FontWeight.Medium)
                            Text("點選開啟地圖", color = c.textDim, fontSize = 18.sp)
                        }
                    }
                } else {
                    Text("尚未綁定導航 App", color = c.text, fontSize = 26.sp)
                    Spacer(Modifier.height(6.dp))
                    Text("設定 → App 綁定 → 導航", color = c.textDim, fontSize = 18.sp)
                }
            }
            // 靠右放，避開左下角的浮動麥克風
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { PillButton("開啟導航", filled = true) { vm.open(Slots.NAV) } }
        }
    }
}

@Composable
private fun MiniPlayer(vm: LauncherViewModel, modifier: Modifier) {
    val c = LocalCamry.current
    val np by vm.media.state.collectAsStateWithLifecycle()
    Panel(modifier, onClick = { vm.go(Screen.AUDIO) }) {
        Row(Modifier.fillMaxSize().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.fillMaxHeight().aspectRatio(1f).clip(RoundedCornerShape(16.dp)).background(c.surfaceHi),
                contentAlignment = Alignment.Center,
            ) {
                val art = np.art
                if (art != null) Image(art, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                else Icon(Icons.Rounded.MusicNote, null, Modifier.size(44.dp), tint = c.textDim)
            }
            Spacer(Modifier.width(18.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                Text(
                    if (np.hasSession && np.title.isNotBlank()) np.title
                    else if (!np.hasAccess) "需要通知存取" else "音訊",
                    color = c.text, fontSize = 22.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                Text(
                    if (np.hasSession) np.artist.ifBlank { "—" }
                    else if (!np.hasAccess) "設定 → 權限與系統" else "尚無播放中的音樂",
                    color = c.textDim, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(10.dp))
                TransportControls(np, vm, big = 52.dp)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FavoritesPanel(vm: LauncherViewModel, modifier: Modifier) {
    val c = LocalCamry.current
    var pickerFor by remember { mutableStateOf<String?>(null) }
    Panel(modifier) {
        Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text("常用 App（長按更換）", color = c.textDim, fontSize = 15.sp, modifier = Modifier.padding(start = 8.dp, bottom = 4.dp))
            Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                Slots.FAVORITES.forEach { key ->
                    val app = vm.appFor(key)
                    Column(
                        Modifier.weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .combinedClickable(
                                onClick = { if (app != null) vm.launchPackage(app.pkg) else pickerFor = key },
                                onLongClick = { pickerFor = key },
                            )
                            .padding(6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        if (app != null) {
                            Image(app.icon, app.label, Modifier.size(56.dp))
                            Text(app.label, color = c.text, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        } else {
                            Box(Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).background(c.surfaceHi), contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.Add, "新增", Modifier.size(28.dp), tint = c.textDim)
                            }
                            Text("新增", color = c.textDim, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
    pickerFor?.let { key ->
        AppPickerDialog(
            title = Slots.labels[key] ?: "選擇 App",
            apps = vm.apps,
            onPick = { vm.bind(key, it); pickerFor = null },
            onDismiss = { pickerFor = null },
        )
    }
}
