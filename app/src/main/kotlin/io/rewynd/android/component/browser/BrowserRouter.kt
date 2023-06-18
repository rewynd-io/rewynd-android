package io.rewynd.android.component.browser

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import io.rewynd.android.BrowserState
import io.rewynd.android.BrowserViewModel
import io.rewynd.android.component.EpisodeBrowser

@Composable
fun BrowserRouter(
    mainViewModel: BrowserViewModel
) {
    val mutableState by mainViewModel.browserState.collectAsState()
    when (val state = mutableState.last()) { // TODO deal with nullable
        is BrowserState.HomeState -> HomeBrowser(mainViewModel)
        is BrowserState.LibraryState -> LibraryBrowser(mainViewModel)
        is BrowserState.ShowState -> ShowBrowser(mainViewModel)
        is BrowserState.SeasonState -> SeasonBrowser(mainViewModel)
        is BrowserState.EpisodeState -> EpisodeBrowser(mainViewModel, state.episodeInfo)
    }
}