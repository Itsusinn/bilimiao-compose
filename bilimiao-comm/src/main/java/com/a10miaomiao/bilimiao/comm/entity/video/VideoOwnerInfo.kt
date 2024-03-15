package com.a10miaomiao.bilimiao.comm.entity.video

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class VideoOwnerInfo(
    val face: String,
    val mid: Long,
    val name: String
) : Parcelable