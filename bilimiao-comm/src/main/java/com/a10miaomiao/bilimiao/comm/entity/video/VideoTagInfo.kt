package com.a10miaomiao.bilimiao.comm.entity.video

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable


@Parcelize
@Serializable
data class VideoTagInfo (
    val attribute: Int,
    val cover: String,
    val hated: Int,
    val hates: Int,
    val is_activity: Int,
    val liked: Int,
    val likes: Int,
    val tag_id: Double,
    val tag_name: String
) : Parcelable