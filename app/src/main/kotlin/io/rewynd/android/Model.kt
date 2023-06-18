package io.rewynd.android

import io.rewynd.model.EpisodeInfo
import io.rewynd.model.Library
import io.rewynd.model.SeasonInfo
import io.rewynd.model.ShowInfo
import io.rewynd.api.DefaultApi
import kotlin.time.Duration

typealias RewyndClient = DefaultApi

@JvmInline
@kotlinx.serialization.Serializable
value class ServerUrl(val value: String)

sealed interface ViewState {
    object Browser : ViewState
}

@kotlinx.serialization.Serializable
sealed interface BrowserState {
    @kotlinx.serialization.Serializable

    class EpisodeState(val episodeInfo: EpisodeInfo) : BrowserState

    @kotlinx.serialization.Serializable

    class SeasonState(val seasonInfo: SeasonInfo) : BrowserState

    @kotlinx.serialization.Serializable

    class ShowState(val showInfo: ShowInfo) : BrowserState

    @kotlinx.serialization.Serializable

    class LibraryState(val library: Library) : BrowserState

    @kotlinx.serialization.Serializable

    object HomeState : BrowserState
}

sealed interface LoginState {
    val serverUrl: ServerUrl

    data class ServerSelect(override val serverUrl: ServerUrl) : LoginState
    data class LoggedOut(override val serverUrl: ServerUrl) : LoginState
    data class LoggedOutVerificationFailed(override val serverUrl: ServerUrl) : LoginState
    data class PendingLogin(override val serverUrl: ServerUrl) : LoginState
    data class PendingVerification(override val serverUrl: ServerUrl) : LoginState
    data class LoggedIn(override val serverUrl: ServerUrl) : LoginState
}


@kotlinx.serialization.Serializable
sealed interface PlayerMedia {
    val startOffset: Duration
    val runTime: Duration

    @kotlinx.serialization.Serializable
    data class Episode(
        val playbackMethod: EpisodePlaybackMethod,
        val info: EpisodeInfo,
        override val runTime: Duration,
        override val startOffset: Duration = Duration.ZERO
    ) : PlayerMedia {
        @kotlinx.serialization.Serializable
        sealed interface EpisodePlaybackMethod {
            @kotlinx.serialization.Serializable
            object Sequential : EpisodePlaybackMethod
        }
    }

    val title: String
        get() = when (this) {
            is Episode -> info.title
        }

    val details: String
        get() = when (this) {
            is Episode -> "${info.showName} - S${info.season}E${info.episode}" // TODO proper formatting
        }
}


@kotlinx.serialization.Serializable
data class PlayerProps(val media: PlayerMedia, val browserState: List<BrowserState>)

@kotlinx.serialization.Serializable
data class PlayerActivityProps(
    val playerProps: PlayerProps,
    val serverUrl: ServerUrl,
    val interruptService: Boolean = true
)

@kotlinx.serialization.Serializable
sealed interface PlayerServiceProps {
    @kotlinx.serialization.Serializable

    data class Start(
        val playerProps: PlayerProps,
        val serverUrl: ServerUrl,
        val interruptPlayback: Boolean = true
    ) : PlayerServiceProps

    @kotlinx.serialization.Serializable
    object Pause : PlayerServiceProps

    @kotlinx.serialization.Serializable
    object Play : PlayerServiceProps

    @kotlinx.serialization.Serializable
    object Next : PlayerServiceProps

    @kotlinx.serialization.Serializable
    object Prev : PlayerServiceProps

    @kotlinx.serialization.Serializable
    object Stop : PlayerServiceProps

    val requestCode: Int
        get() = when (this) {
            is Start -> 0
            is Stop -> 1
            is Pause -> 2
            is Play -> 3
            is Prev -> 4
            is Next -> 5
        }
}