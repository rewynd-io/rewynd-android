package io.rewynd.android.component.browser

import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.Card
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.unit.dp
import io.rewynd.android.BrowserState
import io.rewynd.android.BrowserViewModel

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ShowBrowser(
    mainViewModel: BrowserViewModel
) {
    val seasons by mainViewModel.seasons.observeAsState()

    LazyVerticalGrid(columns = GridCells.Adaptive(minSize = 150.dp)){
        items(seasons!!) {
            Card(onClick = {
                mainViewModel.putBrowserState(BrowserState.SeasonState(it))
            }) {
                Text(text = "Season ${it.seasonNumber}")
            }
        }
    }
}