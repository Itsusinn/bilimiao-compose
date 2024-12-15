package com.a10miaomiao.bilimiao.comm.entity.video

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class VideoStatInfo(
    val aid: Long,
    var coin: Long,
    val danmaku: Long,
    val dislike: Long,
    var favorite: Long,
    val his_rank: Long,
    var like: Long,
    val now_rank: Long,
    val reply: Long,
    val share: Long,
    val view: Long
) : Parcelable