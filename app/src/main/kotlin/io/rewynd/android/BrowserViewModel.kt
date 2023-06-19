package io.rewynd.android

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.cookies.*
import io.ktor.client.plugins.logging.*
import io.ktor.serialization.kotlinx.json.*
import io.rewynd.model.EpisodeInfo
import io.rewynd.model.Library
import io.rewynd.model.Progress
import io.rewynd.model.SeasonInfo
import io.rewynd.model.ShowInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.runningFold
import kotlinx.coroutines.launch

class BrowserViewModel(
    application: Application,
    val serverUrl: ServerUrl,
    private val client: RewyndClient = RewyndClient(serverUrl.value, httpClientConfig = {
        it.install(ContentNegotiation) {
            json()
        }
        it.install(Logging) {
            logger = object : Logger {
                override fun log(message: String) {
                    Log.v("RewyndClient", message)
                }
            }
            level = LogLevel.ALL
        }
        it.install(HttpCookies) {
            this.storage = PersistentCookiesStorage.INSTANCE
        }
    }),
) : AndroidViewModel(application) {

    fun popBrowserState() {
        this.browserState.value =
            if (this.browserState.value.size <= 1) {
                listOf(BrowserState.HomeState)
            } else {
                this.browserState.value.dropLast(
                    1
                )
            }
    }

    val browserState: MutableStateFlow<List<BrowserState>> =
        MutableStateFlow(emptyList())


    fun initBrowserState(browserState: List<BrowserState>) {
        this.browserState.value = browserState
    }

    fun putBrowserState(browserState: BrowserState) {
        this.browserState.value = this.browserState.value + listOf(browserState)
        when (browserState) {
            is BrowserState.HomeState -> {
                loadLibraries()
                loadLatestEpisodes()
            }

            is BrowserState.LibraryState -> loadShows(browserState.library.name)
            is BrowserState.ShowState -> loadSeasons(browserState.showInfo.id)
            is BrowserState.SeasonState -> loadEpisodes(browserState.seasonInfo.id)
            is BrowserState.EpisodeState -> {
                loadPreviousEpisode(browserState.episodeInfo.id)
                loadNextEpisode(browserState.episodeInfo.id)
                loadUserProgress(browserState.episodeInfo.id)
            }
        }
    }

    val libraries = MutableLiveData<List<Library>>(emptyList<Library>())
    fun loadLibraries() {
        Log.i("LibraryLoader", "Loading Libs")
        this.viewModelScope.launch(Dispatchers.IO) {
            libraries.postValue(requireNotNull(client.listLibraries().body()))
            Log.i("LibraryLoader", "Loaded ${libraries.value}")

        }
    }

    val latestEpisodes = MutableLiveData<List<EpisodeInfo>>(emptyList<EpisodeInfo>())

    @OptIn(FlowPreview::class)
    fun loadLatestEpisodes() {
        Log.i("LibraryLoader", "Loading Libs")
        this.viewModelScope.launch(Dispatchers.IO) {
            client.listLatestUserProgress().body().sortedBy { it.timestamp }.reversed().asFlow().flatMapMerge {
                kotlin.runCatching { flowOf(it to client.getEpisode(it.id).body()) }.getOrNull()
                    ?: emptyFlow()
            }.runningFold(emptyList<Pair<Progress, EpisodeInfo>>()) { accumulator, value ->
                accumulator + listOf(value)
            }.collect { pair ->
                latestEpisodes.postValue(pair.map { it.second })
            }
        }
    }

    val shows = MutableLiveData<List<ShowInfo>>(emptyList<ShowInfo>())
    fun loadShows(libraryName: String) {
        this.viewModelScope.launch(Dispatchers.IO) {
            shows.postValue(requireNotNull(client.listShows(libraryName).body()))
        }
    }

    val seasons = MutableLiveData<List<SeasonInfo>>(emptyList<SeasonInfo>())
    fun loadSeasons(showId: String) {
        this.viewModelScope.launch(Dispatchers.IO) {
            seasons.postValue(requireNotNull(client.listSeasons(showId).body()))
        }
    }

    val episodes = MutableLiveData<List<EpisodeInfo>>(emptyList<EpisodeInfo>())
    fun loadEpisodes(seasonId: String) {
        this.viewModelScope.launch(Dispatchers.IO) {
            episodes.postValue(requireNotNull(client.listEpisodes(seasonId).body()))
        }
    }

    val nextEpisode = MutableLiveData<EpisodeInfo?>(null)
    fun loadNextEpisode(episodeId: String) {
        nextEpisode.postValue(null)
        this.viewModelScope.launch(Dispatchers.IO) {
            nextEpisode.postValue(kotlin.runCatching { client.getNextEpisode(episodeId).body() }
                .getOrNull())
        }
    }

    val userProgress = MutableLiveData<Progress?>(null)
    fun loadUserProgress(episodeId: String) {
        userProgress.postValue(null)
        this.viewModelScope.launch(Dispatchers.IO) {
            userProgress.postValue(kotlin.runCatching { client.getUserProgress(episodeId).body() }
                .getOrNull())
        }
    }

    val previousEpisode = MutableLiveData<EpisodeInfo?>(null)
    fun loadPreviousEpisode(episodeId: String) {3
        previousEpisode.postValue(null)
        this.viewModelScope.launch(Dispatchers.IO) {
            previousEpisode.postValue(
                kotlin.runCatching { client.getPreviousEpisode(episodeId).body() }
                    .getOrNull())
        }
    }

    companion object {}
}