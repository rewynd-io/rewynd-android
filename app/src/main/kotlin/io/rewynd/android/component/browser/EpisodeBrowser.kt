package io.rewynd.android.component

import android.content.Intent
import androidx.compose.material.Card
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.LocalContext
import io.rewynd.model.EpisodeInfo
import io.rewynd.android.*
import io.rewynd.android.BrowserViewModel
import io.rewynd.android.PlayerActivity
import io.rewynd.android.PlayerActivityProps
import io.rewynd.android.PlayerMedia
import io.rewynd.android.PlayerProps
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun EpisodeBrowser(
    mainViewModel: BrowserViewModel,
    episodeInfo: EpisodeInfo
) {
    val nextEpisode by mainViewModel.nextEpisode.observeAsState()
    val previousEpisode by mainViewModel.previousEpisode.observeAsState()
    val progress by mainViewModel.userProgress.observeAsState()
    val context = LocalContext.current
    Card(onClick = {
        context.startActivity(Intent(context, PlayerActivity::class.java).apply {
            putExtra(
                PlayerActivity.PLAYER_ACTIVITY_PROPS_EXTRA_NAME, Json.encodeToString(
                    PlayerActivityProps(
                        PlayerProps(
                            PlayerMedia.Episode(
                                PlayerMedia.Episode.EpisodePlaybackMethod.Sequential,
                                episodeInfo,
                                runTime = episodeInfo.runTime.seconds,
                                startOffset =  episodeInfo.runTime.seconds.times((progress?.percent ?: 0.0))
                            ),
                            mainViewModel.browserState.value
                        ),
                        serverUrl = mainViewModel.serverUrl,
                        interruptService = true
                    )
                )
            )
        })
    }) {
        Text(text = episodeInfo.title)
    }
}