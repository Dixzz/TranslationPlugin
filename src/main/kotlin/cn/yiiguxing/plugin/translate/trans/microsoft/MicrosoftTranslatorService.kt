package cn.yiiguxing.plugin.translate.trans.microsoft

import cn.yiiguxing.plugin.translate.trans.Lang
import cn.yiiguxing.plugin.translate.trans.microsoft.data.MicrosoftSourceText
import cn.yiiguxing.plugin.translate.trans.microsoft.data.TextType
import cn.yiiguxing.plugin.translate.util.*
import cn.yiiguxing.plugin.translate.util.Http.userAgent
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import com.intellij.util.io.RequestBuilder
import org.jetbrains.concurrency.Promise
import org.jetbrains.concurrency.runAsync
import java.io.IOException
import java.util.*
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeoutException


/**
 * Service for the Microsoft Translator API.
 */
@Service
internal class MicrosoftTranslatorService {

    private var accessToken: String? = null

    private var expireAt: Long = -1

    private var tokenPromise: Promise<String>? = null

    private fun updateAccessToken(token: String) {
        val expirationTime = getExpirationTimeFromToken(token)
        LOG.d("Update access token: ********, Expiration time: ${Date(expirationTime)}")
        synchronized(this) {
            accessToken = token
            expireAt = expirationTime - 60000
            tokenPromise = null
        }
    }

    @Synchronized
    private fun getValidAccessToken(): String? {
        val token = accessToken
        return if (token != null && System.currentTimeMillis() < expireAt) {
            token
        } else {
            null
        }
    }

    @Synchronized
    private fun getTokenPromise(): Promise<String> {
        synchronized(this) {
            tokenPromise?.let {
                return@getTokenPromise it
            }
        }

        val promise = runAsync { Http.get(AUTH_URL) { userAgent() } }
            .onError { LOG.w("Failed to get access token", it) }
            .onSuccess(::updateAccessToken)

        synchronized(this) {
            tokenPromise = promise
        }

        return promise
    }

    /**
     * Returns the access token. If the token has expired, it will be refreshed.
     */
    @RequiresBackgroundThread
    fun getAccessToken(): String {
        getValidAccessToken()?.let { token ->
            return token
        }

        val promise = getTokenPromise()
        val token = try {
            promise.blockingGet(TIMEOUT)
        } catch (e: TimeoutException) {
            LOG.warn("Authentication failed: timeout", e)
            throw MicrosoftAuthenticationException("Authentication failed: timeout", e)
        } catch (e: Throwable) {
            clearTokenPromise(promise)

            LOG.warn("Authentication failed", e)
            val ex = if (e is ExecutionException) e.cause ?: e else e
            throw if (ex is IOException) {
                MicrosoftAuthenticationException("Authentication failed: ${ex.getCommonMessage()}", ex)
            } else ex
        }

        if (token == null) {
            clearTokenPromise(promise)
            LOG.warn("Authentication failed: cannot get access token")
            throw MicrosoftAuthenticationException("Authentication failed: cannot get access token")
        }

        return token
    }

    @Synchronized
    private fun clearTokenPromise(whosePromise: Promise<*>) {
        if (whosePromise == tokenPromise) {
            tokenPromise = null
        }
    }


    companion object {

        private const val TIMEOUT = 10 * 1000 // 10 seconds

        private const val AUTH_URL = "https://edge.microsoft.com/translate/auth"

        private const val TRANSLATE_URL = "https://api.cognitive.microsofttranslator.com/translate"

        private val GSON: Gson = Gson()
        private val LOG: Logger = logger<MicrosoftTranslatorService>()

        /**
         * Returns the [MicrosoftTranslatorService] instance.
         */
        private val service: MicrosoftTranslatorService get() = service()

        @RequiresBackgroundThread
        fun translate(
            text: String,
            from: Lang,
            to: Lang,
            textType: TextType = TextType.PLAIN,
            separator: String
        ): String {
            val translateUrl = UrlBuilder(TRANSLATE_URL)
                .addQueryParameter("api-version", "3.0")
                .apply { if (from != Lang.AUTO) addQueryParameter("from", from.microsoftLanguageCode) }
                .addQueryParameter("to", to.microsoftLanguageCode)
                .addQueryParameter("textType", textType.value)
                .build()

            val items = mutableListOf<MicrosoftSourceText>()
            if (separator.isNotEmpty())
                items.addAll(text.split(separator).map {
                    MicrosoftSourceText(it)
                })
            else
                items.add(MicrosoftSourceText(text))
            println("Translating $items")
            return MicrosoftHttp.post(translateUrl, items) { auth() }
        }

        private fun RequestBuilder.auth() {
            val accessToken = service.getAccessToken()
            tuner { it.setRequestProperty("Authorization", "Bearer $accessToken") }
        }

        private data class JwtPayload(@SerializedName("exp") val expirationTime: Long)

        private fun getExpirationTimeFromToken(token: String): Long {
            val payloadChunk = token.split('.')[1]
            val decoder = Base64.getUrlDecoder()
            val payload = String(decoder.decode(payloadChunk))
            return GSON.fromJson(payload, JwtPayload::class.java).expirationTime * 1000
        }
    }
}