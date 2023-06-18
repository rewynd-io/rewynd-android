package io.rewynd.android.component.browser

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Card
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import io.rewynd.android.BrowserState
import io.rewynd.android.BrowserViewModel

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun HomeBrowser(
    mainViewModel: BrowserViewModel
) {
    val libraries by mainViewModel.libraries.observeAsState()
    val latestEpisodes by mainViewModel.latestEpisodes.observeAsState()
    Column {
        Row {
            libraries!!.forEach {
                Card(onClick = {
                    mainViewModel.putBrowserState(BrowserState.LibraryState(it))
                }) {
                    Text(text = it.name)
                }
            }
        }
        LazyRow {
            items(latestEpisodes!!) {
                Card(onClick = {
                    mainViewModel.putBrowserState(BrowserState.EpisodeState(it))
                }) {
                    Text(text = (it.showName ?: "") + it.title)
                }
            }
        }
    }
}