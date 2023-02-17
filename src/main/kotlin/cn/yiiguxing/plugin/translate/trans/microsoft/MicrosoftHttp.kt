package cn.yiiguxing.plugin.translate.trans.microsoft

import cn.yiiguxing.plugin.translate.trans.microsoft.data.MicrosoftError
import cn.yiiguxing.plugin.translate.trans.microsoft.data.presentableError
import cn.yiiguxing.plugin.translate.util.d
import cn.yiiguxing.plugin.translate.util.i
import com.google.common.io.Files
import com.google.gson.Gson
import com.google.gson.JsonParseException
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.util.io.HttpRequests
import com.intellij.util.io.RequestBuilder
import org.apache.commons.io.IOUtils
import org.apache.commons.lang.StringEscapeUtils
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.util.zip.GZIPInputStream

internal object MicrosoftHttp {

    private const val JSON_MIME_TYPE = "application/json"

    private val GSON: Gson = Gson()
    private val LOG: Logger = logger<MicrosoftHttp>()

    fun post(url: String, data: Any, init: RequestBuilder.() -> Unit): String {
        println("Posting $url")
        return HttpRequests.post(url, JSON_MIME_TYPE)
            .accept(JSON_MIME_TYPE)
            .apply(init)
            .throwStatusCodeException(false)
            .connect {
                it.write(GSON.toJson(data))
                checkResponseCode(it.connection as HttpURLConnection)
                it.reader.use {
                    it.readText()
                }
            }
    }

    private fun checkResponseCode(connection: HttpURLConnection) {
        val responseCode = connection.responseCode
        if (responseCode < 400) {
            return
        }

        val statusLine = "$responseCode ${connection.responseMessage}"
        val errorText = getErrorText(connection)
        LOG.d("Request: ${connection.requestMethod} ${connection.url} : Error $statusLine body:\n$errorText")

        val jsonError = errorText?.toJsonError()
        jsonError ?: LOG.d("Request: ${connection.requestMethod} ${connection.url} : Unable to parse JSON error")

        throw if (jsonError != null) {
            MicrosoftStatusCodeException(
                "$statusLine - ${jsonError.presentableError}",
                jsonError.error,
                responseCode
            )
        } else {
            MicrosoftStatusCodeException("$statusLine - $errorText", responseCode)
        }
    }

    private fun getErrorText(connection: HttpURLConnection): String? {
        val errorStream = connection.errorStream ?: return null
        val stream = if (connection.contentEncoding == "gzip") GZIPInputStream(errorStream) else errorStream
        return InputStreamReader(stream, Charsets.UTF_8).use { it.readText() }
    }

    private fun String.toJsonError(): MicrosoftError? = try {
        GSON.fromJson(this, MicrosoftError::class.java)
    } catch (jse: JsonParseException) {
        null
    }
}