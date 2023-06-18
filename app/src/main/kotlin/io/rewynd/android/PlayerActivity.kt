package io.rewynd.android

import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Rect
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.exoplayer2.ui.StyledPlayerView
import io.rewynd.android.component.player.PlayerControls
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json


class PlayerActivity : AppCompatActivity() {

    private var lastProps: PlayerActivityProps? = null
    private lateinit var viewModel: PlayerViewModel

    private val playerService: PlayerServiceInterface?
        get() = PlayerService.INSTANCE.value

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }


    private fun updatePictureInPictureParams() {
        val next =
            if (playerService?.next?.value != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                playerService?.nextPendingIntent?.let { nextPendingIntent ->
                    RemoteAction(
                        Icon.createWithResource(
                            this@PlayerActivity,
                            com.google.android.exoplayer2.R.drawable.exo_icon_next
                        ),
                        "Next",
                        "Next",
                        nextPendingIntent
                    )
                }
            } else null

        val prev =
            if (playerService?.prev?.value != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                playerService?.prevPendingIntent?.let { prevPendingIntent ->
                    RemoteAction(
                        Icon.createWithResource(
                            this@PlayerActivity,
                            com.google.android.exoplayer2.R.drawable.exo_icon_previous
                        ),
                        "Prev",
                        "Prev",
                        prevPendingIntent
                    )
                }
            } else null

        val pausePendingIntent = playerService?.pausePendingIntent
        val playPendingIntent = playerService?.playPendingIntent
        val playPause = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (pausePendingIntent != null) {
                RemoteAction(
                    Icon.createWithResource(
                        this@PlayerActivity,
                        com.google.android.exoplayer2.R.drawable.exo_icon_pause
                    ),
                    "Pause",
                    "Pause",
                    pausePendingIntent
                )
            } else if (playPendingIntent != null) {
                RemoteAction(
                    Icon.createWithResource(
                        this@PlayerActivity,
                        com.google.android.exoplayer2.R.drawable.exo_icon_play
                    ),
                    "Play",
                    "Play",
                    playPendingIntent
                )
            } else null
        } else null

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            this.setPictureInPictureParams(
                PictureInPictureParams.Builder()
                    .setActions(
                        listOfNotNull(
                            prev,
                            playPause,
                            next
                        )
                    )
                    .setSourceRectHint(rect)
                    .setAutoEnterEnabled(true)
                    .build()
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            this.setPictureInPictureParams(
                PictureInPictureParams.Builder()
                    .setSourceRectHint(rect)
                    .build()
            )
        }
    }

    private var rect: Rect? = null
        set(value) {
            field = value
            updatePictureInPictureParams()
        }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        enterPip()
    }

    private fun enterPip() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            this.enterPictureInPictureMode(
                PictureInPictureParams.Builder()
                    .setSourceRectHint(rect)
                    .build()
            )
        } else {
            this.enterPictureInPictureMode()
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        }
        if (isInPictureInPictureMode) {
            this.viewModel.areControlsVisible.value = false
        } else {
            // Restore the full-screen UI.
        }
    }

    override fun onNewIntent(intent: Intent?) {
        this.intent = intent
        super.onNewIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        val props = parseProps(intent.extras)
        if (this.lastProps?.playerProps?.media != props.playerProps.media) {
            this.lastProps = props
            props.let {
                viewModel = PlayerViewModel(
                    this,
                    it.serverUrl
                ).apply {
                    startPlayerService(
                        PlayerServiceProps.Start(
                            it.playerProps,
                            it.serverUrl,
                            it.interruptService
                        )
                    )
                }
            }

            onBackPressedDispatcher.addCallback {
                enterPip()
                startActivity(Intent(this@PlayerActivity, BrowserActivity::class.java))
//            }
            }

            setContent {
                val service by PlayerService.INSTANCE.collectAsState()
                service?.let {
                    val isPlaying by it.isPlayingState.collectAsState()
                    LaunchedEffect(isPlaying) {
                        updatePictureInPictureParams()
                        if (isPlaying) {
                            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                        } else {
                            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                        }
                    }

                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        PlayerWrapper(viewModel, it, { rect = it }) { updatePictureInPictureParams() }
                    }
                } ?: CircularProgressIndicator()
            }
            if (playerService?.player?.isPlaying == true) {
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }

    companion object {
        const val PLAYER_ACTIVITY_PROPS_EXTRA_NAME = "PlayerActivityProps"
        private fun parseProps(savedInstanceState: Bundle?): PlayerActivityProps =
            Json.decodeFromString<PlayerActivityProps>(
                requireNotNull(
                    savedInstanceState?.getString(PLAYER_ACTIVITY_PROPS_EXTRA_NAME)
                        .also { Log.d("PlayerActivity", "$it") }) {
                    "Cannot Start PlayerActivity without $PLAYER_ACTIVITY_PROPS_EXTRA_NAME"
                }
            )
    }
}

@Composable
fun PlayerWrapper(
    viewModel: PlayerViewModel,
    serviceInterface: PlayerServiceInterface,
    setBoundingRect: (Rect) -> Unit,
    updateMedia: () -> Unit
) {
    fun View.useRect() {
        val rect = Rect()
        this.getGlobalVisibleRect(
            rect
        )
        setBoundingRect(rect)
    }

    val areControlsVisible by viewModel.areControlsVisible.collectAsState()
    val nullableMedia by serviceInterface.media.collectAsState()
    val isLoading by serviceInterface.isLoading.collectAsState()
    if (isLoading) {
        CircularProgressIndicator(modifier = Modifier.background(Color.Transparent))
    }
    nullableMedia?.let { media ->
        Log.d("PlayerActivity", media.toString())
        LaunchedEffect(key1 = media) {
            updateMedia()
        }
        val prev by serviceInterface.prev.collectAsState()
        val next by serviceInterface.next.collectAsState()
        val bufferedPosition by serviceInterface.bufferedPosition.collectAsState()
        val currentPlayerTime by serviceInterface.currentPlayerTime.collectAsState()
        AndroidView(
            modifier = Modifier.clickable {
                viewModel.setAreControlsVisible(areControlsVisible.not())
            },
            factory = { context ->
                StyledPlayerView(context).apply {
                    this.player = serviceInterface.player
                    useController = false
                    this.addOnLayoutChangeListener { view: View, _: Int, _: Int, _: Int, _: Int, _: Int, _: Int, _: Int, _: Int ->
                        view.useRect()
                    }
                    this.useRect()
                }
            })
        PlayerControls(
            modifier = Modifier.fillMaxSize(),
            isVisible = areControlsVisible,
            isPlaying = serviceInterface.isPlayingState.value,
            title = media.title,
            onPrev = if (prev == null) null else {
                { serviceInterface.playPrev() }
            },
            onNext = if (next == null) null else {
                { serviceInterface.playNext() }
            },
            onPlay = { serviceInterface.play() },
            onPause = { serviceInterface.pause() },
            onSeek = { serviceInterface.seek(it) },
            bufferedPosition = media.startOffset + bufferedPosition,
            currentPlayerTime = media.startOffset + currentPlayerTime,
            runTime = media.runTime,
        )
    }
}