package io.rewynd.android

import android.util.Log
import io.rewynd.model.CreateStreamRequest
import io.rewynd.model.HlsStreamProps
import io.rewynd.model.StreamStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration

class StreamHeartbeat(
    private val client: RewyndClient,
    private val onCanceled: (CreateStreamRequest) -> CreateStreamRequest? = { it },
    private val onAvailable: (HlsStreamProps) -> Unit = {},
    private val onLoad: (HlsStreamProps) -> Unit
) {
    private var job: Job? = null
    private val callMutex = Mutex()

    suspend fun load(request: CreateStreamRequest) = callMutex.withLock {
        job?.cancel()
        job = startJob(request)
    }

    suspend fun unload() = callMutex.withLock {
        job?.cancel()
    }

    private fun startJob(createStreamRequest: CreateStreamRequest) = MainScope().launch {
        val props = client.createStream(createStreamRequest).body()
        var lastStatus: StreamStatus? = null
        while (true) {
            try {
                lastStatus = beat(props, createStreamRequest, lastStatus)
            } catch (e: CancellationException) {
                client.deleteStream(props.id)
                log.info { "Heartbeat stopped" }
                return@launch
            } catch (e: Exception) {
                log.error(e) { "Heartbeat error" }
            }
        }
    }

    private suspend fun beat(
        props: HlsStreamProps,
        lastCreateStreamRequest: CreateStreamRequest,
        priorStatus: StreamStatus?
    ): StreamStatus? {
        val latestStatus =
            kotlin.runCatching { client.heartbeatStream(props.id).body() }
                .onFailure {
                    log.error(it) { "Failed to heartbeat stream" }
                }
                .getOrNull()
        when (latestStatus) {
            StreamStatus.available -> {
                if (priorStatus != StreamStatus.available) {
                    onLoad(props)
                }
                onAvailable(props)
                delay(10000)
            }

            StreamStatus.canceled -> {
                MainScope().launch {
                    onCanceled(lastCreateStreamRequest)?.let {
                        load(it)
                    }
                }
                delay(15000)
            }

            StreamStatus.pending -> {
                delay(500)
            }

            null -> {
                Log.w("PlayerService", "Stream failed to heartbeat!")
                delay(5000)
            }
        }
        return latestStatus
    }

    companion object : KLog() {
        fun PlayerMedia.copy(startOffset: Duration = this.startOffset) = when (this) {
            is PlayerMedia.Episode -> this.copy(
                playbackMethod = playbackMethod,
                startOffset = startOffset
            )
        }

    }
}