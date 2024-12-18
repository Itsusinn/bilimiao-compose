package com.a10miaomiao.bilimiao.comm.network

import android.webkit.CookieManager
import com.a10miaomiao.bilimiao.comm.BilimiaoCommApp
import com.a10miaomiao.bilimiao.comm.utils.Log
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy
import kotlinx.serialization.json.decodeFromStream
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class MiaoHttp(var url: String? = null) {
    private val TAG = "MiaoHttp"
    private val cookieManager = CookieManager.getInstance()

    var client = OkHttpClient()
    val requestBuilder = Request.Builder()
    val headers = mutableMapOf<String, String>()
    var method = GET

    var body: RequestBody? = null
    var formBody: Map<String, String?>? = null

    private fun buildRequest(): Request {
        for (key in headers.keys) {
            requestBuilder.addHeader(key, headers[key]!!)
        }
        requestBuilder.addHeader("user-agent", ApiHelper.USER_AGENT)
        requestBuilder.addHeader("referer",ApiHelper.REFERER)
        requestBuilder.addHeader("build", ApiHelper.BUILD_VERSION.toString())

        if (url?.let { "bilibili.com" in it } == true) {
            requestBuilder.addHeader("env", "prod")
            requestBuilder.addHeader("app-key", "android")
//            requestBuilder.addHeader("X-Requested-With", "tv.danmaku.bilibilihd")
            requestBuilder.addHeader("x-bili-aurora-eid", "UlMFQVcABlAH")
            requestBuilder.addHeader("x-bili-aurora-zone", "sh001")
            BilimiaoCommApp.commApp.loginInfo?.token_info?.let{
                requestBuilder.addHeader("x-bili-mid", it.mid.toString())
            }
        }

        requestBuilder.addHeader("cookie", (cookieManager.getCookie(url) ?: ""))
        if (body == null && formBody != null) {
            val bodyStr = ApiHelper.urlencode(formBody!!)
            body = bodyStr.toRequestBody(
                "application/x-www-form-urlencoded".toMediaType()
            )
        }
        if (Log.isDebug) {
            Log.debug { "-----START-$method-----" }
            Log.debug { "URL = $url" }
            formBody?.let {
                Log.debug { "BODY = $it" }
            }
            Log.debug { "------END-$method------" }
        }
        val req = requestBuilder.method(method, body)
            .url(url!!)
            .build()
        return req
    }

    fun call(): Response {
        val req = buildRequest()
        return client.newCall(req).execute()
    }

    suspend fun awaitCall(): Response{
        return suspendCancellableCoroutine { continuation ->
            val req = buildRequest()
            val call = client.newCall(req)
            continuation.invokeOnCancellation {
                call.cancel()
            }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    continuation.resumeWithException(e)
                }
                override fun onResponse(call: Call, response: Response) {
                    continuation.resume(response)
                }
            })
        }
    }

    fun get(): Response {
        method = GET
        return call()
    }

    fun post(): Response {
        method = POST
        return call()
    }

    companion object {
        fun request(url: String? = null, init: (MiaoHttp.() -> Unit)? = null) = MiaoHttp(url).apply {
            init?.invoke(this)
        }

        @OptIn(ExperimentalSerializationApi::class)
        val JSON = Json {
            ignoreUnknownKeys = true
            explicitNulls = false

            namingStrategy = JsonNamingStrategy.SnakeCase
        }
        @OptIn(ExperimentalSerializationApi::class)
        inline fun <reified T> Response.json(): T {
            return JSON.decodeFromStream(body!!.byteStream())
        }

        const val GET = "GET"
        const val POST = "POST"

    }
}
