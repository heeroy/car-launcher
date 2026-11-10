package tw.camry.launcher.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import tw.camry.launcher.LauncherViewModel

@Composable
fun AppsScreen(vm: LauncherViewModel) {
    Panel(Modifier.fillMaxSize().padding(start = 24.dp, end = 24.dp, bottom = 24.dp, top = 4.dp)) {
        LazyVerticalGrid(
            GridCells.Adaptive(140.dp),
            Modifier.fillMaxSize().padding(16.dp),
        ) {
            items(vm.apps, key = { it.pkg }) { app ->
                AppIconTile(app, iconSize = 72.dp) { vm.launchPackage(app.pkg) }
            }
        }
    }
}
