package tw.camry.launcher.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import tw.camry.launcher.LauncherViewModel
import tw.camry.launcher.Screen

@Composable
fun AppsScreen(vm: LauncherViewModel) {
    Column(Modifier.fillMaxSize()) {
        ScreenHeader(Icons.Rounded.Apps, Screen.APPS.title)
        LazyVerticalGrid(
            GridCells.Adaptive(140.dp),
            Modifier.weight(1f).padding(horizontal = 16.dp),
            // 底部留白：最後一列可捲到浮動麥克風上方
            contentPadding = PaddingValues(bottom = MicClearance),
        ) {
            items(vm.apps, key = { it.pkg }) { app ->
                AppIconTile(app, iconSize = 72.dp) { vm.launchPackage(app.pkg) }
            }
        }
    }
}
