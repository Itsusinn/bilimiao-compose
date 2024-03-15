package com.a10miaomiao.bilimiao.comm.entity.video

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class VideoStatInfo(
    val aid: Int,
    var coin: Int,
    val danmaku: Int,
    val dislike: Int,
    var favorite: Int,
    val his_rank: Int,
    var like: Int,
    val now_rank: Int,
    val reply: Int,
    val share: Int,
    val view: Long
) : Parcelable