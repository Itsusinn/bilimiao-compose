package com.a10miaomiao.bilimiao.comm.apis

import android.os.Parcelable
import android.os.SystemClock
import com.a10miaomiao.bilimiao.comm.entity.ResultInfo
import com.a10miaomiao.bilimiao.comm.entity.player.PlayerV2Info
import com.a10miaomiao.bilimiao.comm.exception.AreaLimitException
import com.a10miaomiao.bilimiao.comm.network.ApiHelper
import com.a10miaomiao.bilimiao.comm.network.BiliApiService
import com.a10miaomiao.bilimiao.comm.network.MiaoHttp
import com.a10miaomiao.bilimiao.comm.network.MiaoHttp.Companion.json
import com.a10miaomiao.bilimiao.comm.proxy.ProxyServerInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

class PlayerAPI {

    private fun getVideoHeaders(avid: Long) = mapOf(
        "Referer" to "https://www.bilibili.com/av$avid",
        "User-Agent" to "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_12_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/63.0.3239.84 Safari/537.36"
    )


    fun getPlayerV2Info(
        aid: Long,
        cid: String,
    ) = MiaoHttp.request {
        url = BiliApiService.biliApi(
            "x/player/v2",
            "aid" to aid.toString(),
            "cid" to cid,
        )
    }
    suspend fun getPlayerInfoAsync(
        aid: Long,
        cid: String,
    ):ResultInfo<PlayerV2Info> = withContext(Dispatchers.IO) {
        return@withContext getPlayerV2Info(aid, cid).awaitCall().json<ResultInfo<PlayerV2Info>>()
    }

    fun getPlayerV2Info(
        aid: String,
        cid: String,
        epId: String,
        seasonId: String,
    ) = MiaoHttp.request {
        url = BiliApiService.biliApi(
            "x/player/v2",
            "aid" to aid,
            "cid" to cid,
            "ep_id" to epId,
            "season_id" to seasonId,
        )
    }

    /**
     * 获取视频播放地址
     * fnval: 976:flv,1:mp4,4048:dash
     * quality https://github.com/SocialSisterYi/bilibili-API-collect/blob/master/docs/video/videostream_url.md#qn%E8%A7%86%E9%A2%91%E6%B8%85%E6%99%B0%E5%BA%A6%E6%A0%87%E8%AF%86
     */
    suspend fun getVideoPlayUrl(
        avid: Long,
        cid: String,
        quality: Int = 64,
        // fnval: Int = 4048,
    ): Result<PlayurlData> {
        val fnval = 4048 // only support dash for now
        val params = mutableMapOf<String, String?>(
            "avid" to avid.toString(),
            "cid" to cid,
            "qn" to quality.toString(),
            "fnval" to fnval.toString(),
            "fnver" to "0",
            "force_host" to "2", // 强制音视频返回 https
            "type" to "",
            "otype" to "json",
        )
        if (fnval > 2) {
            params["fourk"] = "1"
        }
        val res = MiaoHttp.request {
            url = BiliApiService.biliApi("x/player/playurl", *params.toList().toTypedArray())
            headers.putAll(getVideoHeaders(avid))
        }.awaitCall().json<ResultInfo<PlayurlData>>()
        return if (res.code == 0 && res.data != null) {
            Result.success(res.data)
        } else {
            Result.failure(Exception(res.message))
        }
    }
    /**
     * 获取番剧播放地址
     */
    suspend fun getBangumiUrl(
        epid: String,
        cid: String,
        qn: Int = 64,
        fnval: Int = 4048
    ): PlayurlData {
        val params = mutableMapOf<String, String?>(
            "ep_id" to epid,
            "cid" to cid,
            "fnval" to fnval.toString(),
            "fnver" to "0",
            "force_host" to "2", // 强制音视频返回 https
            "module" to "bangumi",
            "qn" to qn.toString(),
            "season_type" to "1",
            "session" to ApiHelper.getMD5((System.currentTimeMillis() - SystemClock.currentThreadTimeMillis()).toString()),
            "track_path" to "",
            "device" to "android",
            "mobi_app" to "android",
            "platform" to "android"
        )
        if (fnval > 2) {
            params["fourk"] = "1"
        }
        val res = MiaoHttp.request {
            url = BiliApiService.biliApi(
                "pgc/player/api/playurl",
                *params.toList().toTypedArray()
            )
        }.awaitCall().json<PlayurlData>()
        if (res.code == 0) {
            return res
        } else if (res.code == -10403) {
            throw AreaLimitException()
        } else {
            throw Exception(res.message)
        }
    }

    suspend fun getProxyBangumiUrl(
        epid: String,
        cid: String,
        qn: Int = 64,
        fnval: Int = 4048,
        proxyServer: ProxyServerInfo,
    ): PlayurlData {
        val params = mutableMapOf<String, String?>(
            "ep_id" to epid,
            "cid" to cid,
            "fnval" to fnval.toString(),
            "fnver" to "0",
            "force_host" to "2", // 强制音视频返回 https
            "module" to "bangumi",
            "qn" to qn.toString(),
            "season_type" to "1",
            "session" to ApiHelper.getMD5((System.currentTimeMillis() - SystemClock.currentThreadTimeMillis()).toString()),
            "track_path" to "",
            "device" to "android",
            "mobi_app" to "android",
            "platform" to "android",
        )
        if (fnval > 2) {
            params["fourk"] = "1"
        }
        if (!proxyServer.isTrust) {
            params["notoken"] = "1"
        }
        val res = MiaoHttp.request {
            if (proxyServer.enableAdvanced == true) {
                // 启用高级设置，自定义请求参数和请求头
//                headers["x-from-biliroaming"] = "1.6.12"
//                params["area"] = "hk"
                proxyServer.queryArgs?.forEach {
                    if (it.enable && it.key.isNotBlank()) {
                        params[it.key] = it.value
                    }
                }
                proxyServer.headers?.forEach {
                    if (it.enable
                        && it.name.isNotBlank()
                        && it.value.isNotBlank()) {
                        headers[it.name] = it.value
                    }
                }
            }
            url = BiliApiService.createUrl(
                "https://${proxyServer.host}/pgc/player/api/playurl",
                *params.toList().toTypedArray()
            )
        }.awaitCall().json<PlayurlData>()
        if (res.code == 0) {
            return res
        } else if (res.code == -10403) {
            throw AreaLimitException()
        } else {
            throw Exception(res.message)
        }
    }

    fun getDanmakuList(cid: String): MiaoHttp {
        return MiaoHttp.request {
            url = "https://comment.bilibili.com/$cid.xml"
        }
    }

    fun sendDamaku(
        msg: String,
        aid: String,
        oid: String,
        progress: Long,
        color: Int,
        fontsize: Int,
        mode: Int, // 1：普通弹幕, 4：底部弹幕, 5：顶部弹幕, 7：高级弹幕, 9：BAS弹幕（pool必须为2）
    ) = MiaoHttp.request {
        url = BiliApiService.biliApi(
            "x/v2/dm/post"
        )
        formBody = mapOf(
            "msg" to msg,
            "type" to "1",
            "aid" to aid,
            "oid" to oid,
            "progress" to progress.toString(),
            "color" to color.toString(),
            "fontsize" to fontsize.toString(),
            "mode" to mode.toString(),
            "rnd" to System.currentTimeMillis().toString(),
        )
        method = MiaoHttp.POST
    }

    @Parcelize
    @Serializable
    data class PlayurlData(
        val accept_description: List<String>,
        val accept_format: String,
        val accept_quality: List<Int>,
        val format: String,
        val from: String,
        val message: String,
        val quality: Int,
        val result: String,
        val seek_param: String,
        val seek_type: String,
        // 时长，毫秒
        val timelength: Int,
        val videoCodecid: Int,
        val durl: List<Durl>?,
        val dash: Dash?,
        val code: Int?,
        val support_formats: List<SupportFormats>,
        val last_play_time: Long?,
        val last_play_cid: Long?,
    ) : Parcelable

    @Parcelize
    @Serializable
    data class Durl(
        val ahead: String,
        val length: Long,
        val order: Int,
        val size: Long,
        val url: String,
        val vhead: String
    ) : Parcelable

    @Parcelize
    @Serializable
    data class SupportFormats(
        val quality: Int,
        val format: String,
        val new_description: String,
        val display_desc: String,
        val superscript: String
    ) : Parcelable

    @Parcelize
    @Serializable
    data class Dash(
        // 时长，秒
        val duration: Long,
        val min_buffer_time: Double,
        val video: List<VideoDashItem>,
        val audio: List<AudioDashItem>?,
    ) : Parcelable

    @Parcelize
    @Serializable
    data class VideoDashItem(
        val id: Quality,
        val bandwidth: Int,
        val base_url: String,
        val backup_url: List<String>,
        val mime_type: String,
        val codecid: Int,
        val codecs: String,
        val width: Int,
        val height: Int,
        val frame_rate: String,
        val segment_base: SegmentBase,
    ) : Parcelable
    @Parcelize
    @Serializable
    data class AudioDashItem(
        val id: Quality,
        val bandwidth: Int,
        val base_url: String,
        val backup_url: List<String>,
        val mime_type: String,
        val codecid: Int,
        val codecs: String,
        val segment_base: SegmentBase,
    ) : Parcelable

    @Parcelize
    @Serializable
    data class SegmentBase(
        val initialization: String,
        val index_range: String,
    ) : Parcelable
}

@JvmInline
@Parcelize
@Serializable
value class Quality(
     val code:Int
) : Parcelable {

//    @JvmInline
//    value class R240P(6):VideoQuality(6),
//    R360P(16),
//    R480P(32),
//    R720P(64),
//    R720P60F(74),
//    R1080P(80), // Default
//    R1080P60F(116),
//    R4K(120),
//    HDR(124), // DASH only
//    DOLBY_VISION(126), // DASH only
//    R8K(127), // DASH only
}
enum class AudioQuality(code:Int) {

}