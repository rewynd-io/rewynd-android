package io.rewynd.android

import io.ktor.client.plugins.cookies.*
import io.ktor.http.*
import io.ktor.util.date.*
import kotlinx.coroutines.runBlocking
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

class CookieStorageCookieJar(private val cookiesStorage: CookiesStorage) : CookieJar {
    override fun loadForRequest(url: HttpUrl): List<Cookie> = runBlocking {
        cookiesStorage.get(Url(url.toUri())).map {
            with(Cookie.Builder()) {
                name(it.name)
                value(it.value)
                if (it.httpOnly) httpOnly()
                it.path?.also(this::path)
                it.expires?.timestamp?.apply(this::expiresAt)
                if (it.maxAge > 0) this.expiresAt((System.currentTimeMillis() / 1000) + it.maxAge)
                if (it.secure) secure()
                it.domain?.also(this::domain)
                // TODO it.encoding?
                // TODO it.extensions?
                this
            }.build()
        }
    }

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) = runBlocking {
        cookies.forEach {
            cookiesStorage.addCookie(
                Url(url.toUri()), Cookie(
                    name = it.name,
                    value = it.value,
                    encoding = CookieEncoding.RAW, // Hopefully this plays nice...
                    maxAge = 0,
                    expires = GMTDate(it.expiresAt),
                    domain = it.domain,
                    path = it.path,
                    secure = it.secure,
                    httpOnly = it.httpOnly,
                )
            )
        }
    }

}