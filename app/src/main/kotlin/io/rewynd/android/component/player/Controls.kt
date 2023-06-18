package io.rewynd.android.component.player

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Slider
import androidx.compose.material.SliderDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import kotlin.math.roundToLong
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun PlayerControls(
    modifier: Modifier = Modifier,
    isVisible: Boolean,
    isPlaying: Boolean,
    title: String,
    runTime: Duration,
    currentPlayerTime: Duration,
    bufferedPosition: Duration,
    onPause: () -> Unit,
    onPlay: () -> Unit,
    onSeek: (Duration) -> Unit,
    onNext: (() -> Unit)?,
    onPrev: (() -> Unit)?
) {
    Log.i("PlayerControls", "$runTime, $currentPlayerTime, $bufferedPosition, $title")

    AnimatedVisibility(
        modifier = modifier,
        visible = isVisible,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(modifier = Modifier.background(Color.Black.copy(alpha = 0.6f))) {
            TopControl(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth(),
                title = title
            )

            CenterControls(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(),
                isPlaying = isPlaying,
                onNext = onNext,
                onPause = onPause,
                onPlay = onPlay,
                onPrev = onPrev,
                onSeek = onSeek,
                currentTime = currentPlayerTime
            )

            BottomControls(
                modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .animateEnterExit(
                        enter =
                        slideInVertically(
                            initialOffsetY = { fullHeight: Int ->
                                fullHeight
                            }
                        ),
                        exit =
                        slideOutVertically(
                            targetOffsetY = { fullHeight: Int ->
                                fullHeight
                            }
                        )
                    ),
                totalDuration = runTime,
                currentTime = currentPlayerTime,
                bufferedPosition = bufferedPosition,
                onSeekChanged = onSeek
            )
        }
    }
}

@Composable
private fun TopControl(modifier: Modifier = Modifier, title: String) {
    Text(
        modifier = modifier.padding(16.dp).background(Color.White),
        text = title,
        style = MaterialTheme.typography.h6,
    )
}

@Composable
private fun CenterControls(
    modifier: Modifier = Modifier,
    isPlaying: Boolean,
    currentTime: Duration,
    onPause: () -> Unit,
    onPlay: () -> Unit,
    onSeek: (desiredTime: Duration) -> Unit,
    onNext: (() -> Unit)?,
    onPrev: (() -> Unit)?
) {

    Row(modifier = modifier, horizontalArrangement = Arrangement.SpaceEvenly) {
        IconButton(modifier = Modifier.size(40.dp), onClick = onPrev ?: {  }, enabled = onPrev != null) {
            Image(
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                painter = painterResource(id = com.google.android.exoplayer2.R.drawable.exo_controls_previous),
                contentDescription = "Next"
            )
        }
        IconButton(modifier = Modifier.size(40.dp), onClick = { onSeek(currentTime - 10.seconds) }) {
            Image(
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                painter = painterResource(id = com.google.android.exoplayer2.R.drawable.exo_icon_rewind),
                contentDescription = "Replay 10 seconds"
            )
        }

        IconButton(modifier = Modifier.size(40.dp), onClick = if (isPlaying) onPause else onPlay) {
            Image(
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                painter =
                when {
                    isPlaying -> {
                        painterResource(id = com.google.android.exoplayer2.ui.R.drawable.exo_icon_pause)
                    }

                    else -> {
                        painterResource(id = com.google.android.exoplayer2.R.drawable.exo_icon_play)
                    }
                },
                contentDescription = "Play/Pause"
            )
        }

        IconButton(modifier = Modifier.size(40.dp), onClick = { onSeek(currentTime + 10.seconds) }) {
            Image(
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                painter = painterResource(id = com.google.android.exoplayer2.R.drawable.exo_icon_fastforward),
                contentDescription = "Skip 10 seconds"
            )
        }

        IconButton(modifier = Modifier.size(40.dp), onClick = onNext ?: {  }, enabled = onNext != null) {
            Image(
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                painter = painterResource(id = com.google.android.exoplayer2.R.drawable.exo_controls_next),
                contentDescription = "Next"
            )
        }
    }
}

@Composable
private fun BottomControls(
    modifier: Modifier = Modifier,
    totalDuration: Duration,
    currentTime: Duration,
    bufferedPosition: Duration,
    onSeekChanged: (desiredTime: Duration) -> Unit
) {

    Column(modifier = modifier.padding(bottom = 32.dp)) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Slider(
                value = bufferedPosition.inWholeMilliseconds.toFloat(),
                enabled = false,
                onValueChange = { /*do nothing*/ },
                valueRange = 0f..totalDuration.inWholeMilliseconds.toFloat(),
                colors =
                SliderDefaults.colors(
                    disabledThumbColor = Color.Transparent,
                    disabledActiveTrackColor = Color.Gray
                )
            )

            Slider(
                modifier = Modifier.fillMaxWidth(),
                value = currentTime.inWholeMilliseconds.toFloat(),
                onValueChange = {onSeekChanged(it.roundToLong().milliseconds)},
                valueRange = 0f..totalDuration.inWholeMilliseconds.toFloat(),
            )
        }
    }
}
