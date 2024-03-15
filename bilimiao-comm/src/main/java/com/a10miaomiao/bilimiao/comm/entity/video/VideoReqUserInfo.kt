package com.a10miaomiao.bilimiao.comm.entity.video

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class VideoReqUserInfo(
    val attention: Int,
    var coin: Int?,
    var dislike: Int?,
    var favorite: Int?,
    var like: Int?
) : Parcelable
