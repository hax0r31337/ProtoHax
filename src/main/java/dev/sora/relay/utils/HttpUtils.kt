package dev.sora.relay.utils

import java.io.DataOutputStream
import java.net.HttpURLConnection
import java.net.URL

object HttpUtils {
    private const val DEFAULT_AGENT = "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/67.0.3396.99 Safari/537.36"

    fun make(url: String, method: String, data: String = "", header: Map<String, String> = emptyMap(), agent: String = DEFAULT_AGENT): HttpURLConnection {
        val httpConnection = URL(url).openConnection() as HttpURLConnection

        httpConnection.requestMethod = method
        httpConnection.connectTimeout = 2000
        httpConnection.readTimeout = 10000

        httpConnection.setRequestProperty("User-Agent", agent)
        header.forEach { (key, value) -> httpConnection.setRequestProperty(key, value) }

        httpConnection.instanceFollowRedirects = true
        httpConnection.doOutput = true

        if (data.isNotEmpty()) {
            val dataOutputStream = DataOutputStream(httpConnection.outputStream)
            dataOutputStream.writeBytes(data)
            dataOutputStream.flush()
        }

        httpConnection.connect()

        return httpConnection
    }

    fun request(url: String, method: String, data: String = "", header: Map<String, String> = emptyMap(), agent: String = DEFAULT_AGENT): String {
        val connection = make(url, method, data, header, agent)

        return connection.inputStream.reader().readText()
    }

    fun get(url: String, header: Map<String, String> = emptyMap()) = request(url, "GET", header = header)

    fun post(url: String, data: String, header: Map<String, String> = emptyMap()) = request(url, "POST", data, header)
}