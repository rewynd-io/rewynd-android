package io.rewynd.android

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.preference.PreferenceManager
import io.ktor.client.plugins.cookies.AcceptAllCookiesStorage
import io.ktor.client.plugins.cookies.CookiesStorage
import io.ktor.http.Cookie
import io.ktor.http.CookieEncoding
import io.ktor.http.Url
import io.ktor.util.date.GMTDate
import io.rewynd.android.PersistentCookiesStorage.SerializableCookie.Companion.COOKIES_STORE_PREF
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentSkipListSet
import kotlin.time.Duration.Companion.seconds

// TODO rework this with in-memory caching and periodic flushes to disk
class PersistentCookiesStorage(
    private val prefs: SharedPreferences,
) : CookiesStorage {

    constructor(context: Context): this(PreferenceManager.getDefaultSharedPreferences(context))

    private val store: AcceptAllCookiesStorage = AcceptAllCookiesStorage()
    private val urls: ConcurrentSkipListSet<String>

    init {
        val stored = prefs.getString(COOKIES_STORE_PREF, null)?.let {
            Json.decodeFromString<HashMap<String, HashSet<SerializableCookie>>>(it)
        } ?: emptyMap()

        urls = ConcurrentSkipListSet(stored.keys.map { it })
        runBlocking {
            stored.entries.flatMap { it.value.map { serializableCookie -> it.key to serializableCookie.toCookie() } }
                .forEach { store.addCookie(Url(it.first), it.second) }
        }
    }

    override suspend fun addCookie(requestUrl: Url, cookie: Cookie) {
        Log.d("Cookie", "Added $cookie for $requestUrl")
        this.store.addCookie(requestUrl, cookie)
        urls.add(requestUrl.toString())
    }

    override fun close() {
        runBlocking {
            this@PersistentCookiesStorage.flush()
        }
    }

    override suspend fun get(requestUrl: Url): List<Cookie> {
        Log.d("Cookie", "Fetching for $requestUrl")

        return this.store.get(requestUrl)
    }

    fun startFlush() = MainScope().launch {
        while (true) {
            flush()
            delay(10.seconds)
        }
    }

    suspend fun flush() {
        val serialized = serializeCookies()

        Log.d("Cookie", "Persisted $serialized")
        prefs.edit().apply {
            putString(COOKIES_STORE_PREF, serialized)
        }.apply()
    }

    private suspend fun serializeCookies() = Json.encodeToString(this.urls.associate {
        it.toString() to this.store.get(Url(it))
            .map { cookie -> SerializableCookie.fromCookie(cookie) }
    })


    companion object {
        val INSTANCE by lazy {
            PersistentCookiesStorage(PreferenceManager.getDefaultSharedPreferences(App.context)).apply { startFlush() }
        }
    }


    @kotlinx.serialization.Serializable
    data class SerializableCookie(
        val name: String,
        val value: String,
        val encoding: CookieEncoding = CookieEncoding.URI_ENCODING,
        val maxAge: Int = 0,
        val expires: Long? = null,
        val domain: String? = null,
        val path: String? = null,
        val secure: Boolean = false,
        val httpOnly: Boolean = false,
        val extensions: Map<String, String?> = emptyMap()
    ) {
        fun toCookie() = Cookie(
            name = this.name,
            value = this.value,
            encoding = this.encoding,
            maxAge = this.maxAge,
            expires = GMTDate(this.expires),
            domain = this.domain,
            path = this.path,
            secure = this.secure,
            httpOnly = this.httpOnly,
            extensions = this.extensions,
        )

        companion object {
            fun fromCookie(cookie: Cookie) = SerializableCookie(
                name = cookie.name,
                value = cookie.value,
                encoding = cookie.encoding,
                maxAge = cookie.maxAge,
                expires = cookie.expires?.timestamp,
                domain = cookie.domain,
                path = cookie.path,
                secure = cookie.secure,
                httpOnly = cookie.httpOnly,
                extensions = cookie.extensions,
            )

            const val COOKIES_STORE_PREF = "CookieStore"
        }
    }
}


