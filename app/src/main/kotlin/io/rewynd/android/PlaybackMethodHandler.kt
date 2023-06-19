package io.rewynd.android

import kotlin.time.Duration.Companion.seconds

// TODO figure out the nice inheritance/sealed interface way of doing this
object PlaybackMethodHandler {
    suspend fun next(client: RewyndClient, playerMedia: PlayerMedia): PlayerMedia? = when(playerMedia) {
        is PlayerMedia.Episode -> EpisodePlaybackMethodHandler.next(client, playerMedia)
    }
    suspend fun prev(client: RewyndClient, playerMedia: PlayerMedia): PlayerMedia? = when(playerMedia) {
        is PlayerMedia.Episode -> EpisodePlaybackMethodHandler.prev(client, playerMedia)
    }
}

private object EpisodePlaybackMethodHandler : KLog()  {
    suspend fun next(
        client: RewyndClient,
        playerMedia: PlayerMedia.Episode
    ): PlayerMedia.Episode? = try {
        client.getNextEpisode(playerMedia.info.id).body().let {
            val progress = client.getUserProgress(it.id).body()
            PlayerMedia.Episode(
                playbackMethod = playerMedia.playbackMethod,
                info = it,
                runTime = it.runTime.seconds,
                startOffset = (progress.percent * it.runTime).seconds
            )
        }
    } catch (e: Exception) {
        log.error(e) { "Failed to find next episode for $playerMedia" }
        null
    }

    suspend fun prev(
        client: RewyndClient,
        playerMedia: PlayerMedia.Episode
    ): PlayerMedia.Episode? = try {
        client.getPreviousEpisode(playerMedia.info.id).body().let {
            val progress = client.getUserProgress(it.id).body()
            PlayerMedia.Episode(
                playbackMethod = playerMedia.playbackMethod,
                info = it,
                runTime = it.runTime.seconds,
                startOffset = (progress.percent * it.runTime).seconds
            )
        }
    } catch (e: Exception) {
        log.error(e) { "Failed to find previous episode for $playerMedia" }
        null
    }
}