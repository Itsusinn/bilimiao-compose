package com.a10miaomiao.bilimiao.comm.apis

import android.os.Build
import com.a10miaomiao.bilimiao.comm.entity.ResultInfo
import com.a10miaomiao.bilimiao.comm.entity.home.HomeRecommendInfo
import com.a10miaomiao.bilimiao.comm.network.BiliApiService
import com.a10miaomiao.bilimiao.comm.network.MiaoHttp
import com.a10miaomiao.bilimiao.comm.network.MiaoHttp.Companion.json
import java.util.Locale

class HomeApi {

    /**
     * 视频推荐
     */

    suspend fun recommendList(
        idx: Long,
        flush: String = "5",
        column: String = "4",
        device: String = "pad",
        deviceName: String = Build.DEVICE,
    ) = MiaoHttp.request {
        val isPull = idx == 0L
        url = BiliApiService.biliApp("x/v2/feed/index",
            "idx" to idx.toString(),
            "flush" to flush,
            "column" to column,
            "device" to device,
            "device_name" to deviceName,
            "device_type" to "0",
            "pull" to isPull.toString().lowercase(Locale.getDefault()),
        )
    }.awaitCall().json<ResultInfo<HomeRecommendInfo>>().let {
        if (it.isSuccess && it.data != null){
            Result.success(it.data)
        } else {
            Result.failure(Exception(it.message))
        }
    }

}