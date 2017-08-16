package org.abhijitsarkar

import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.core.Method
import com.github.kittinunf.fuel.core.interceptors.loggingRequestInterceptor
import com.github.kittinunf.fuel.core.interceptors.loggingResponseInterceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.internal.http.RequestLine
import okhttp3.internal.http.StatusLine
import java.net.Proxy
import java.util.zip.GZIPInputStream

/**
 * @author Abhijit Sarkar
 */

// https://stackoverflow.com/q/45702466
class Q45702466 {
    private val url = "https://fantasy.premierleague.com/drf/elements/"

    fun syncGetOkHttp() {
        println("\n===")
        println("OkHttp")
        println("===")
        val client = OkHttpClient().newBuilder()
                .addNetworkInterceptor { chain ->
                    val (request, response) = chain.request().let {
                        Pair(it, chain.proceed(it))
                    }

                    println("--> ${RequestLine.get(request, Proxy.Type.HTTP)})")
                    println("Headers: (${request.headers().size()})")
                    request.headers().toMultimap().forEach { k, v -> println("$k : $v") }

                    println("<-- ${response.code()} (${request.url()})")

                    val body = if (response.body() != null)
                        GZIPInputStream(response.body()!!.byteStream()).use {
                            it.readBytes(50000)
                        } else null

                    println("Response: ${StatusLine.get(response)}")
                    println("Length: (${body?.size ?: 0})")

                    println("""Body: ${if (body != null && body.isNotEmpty()) String(body) else "(empty)"}""")
                    println("Headers: (${response.headers().size()})")
                    response.headers().toMultimap().forEach { k, v -> println("$k : $v") }

                    response
                }
                .build()

        Request.Builder()
                .url(url)
                .header("Accept", "application/json")
                .header("User-Agent", "Mozilla/5.0")
                .build()
                .let { client.newCall(it).execute() }
    }

    fun syncGetFuel() {
        println("\n===")
        println("Fuel")
        println("===")

        FuelManager()
                .apply {
                    addRequestInterceptor(loggingRequestInterceptor())
                    addResponseInterceptor { loggingResponseInterceptor() }
                }
                .let {
                    it.request(Method.GET, url)
                            .responseString(Charsets.UTF_8)
                }
    }
}

fun main(args: Array<String>) {
    Q45702466().apply {
        syncGetOkHttp()
        syncGetFuel()
    }
}

