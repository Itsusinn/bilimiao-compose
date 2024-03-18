package com.a10miaomiao.bilimiao.comm.entity.video

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable


@Parcelize
@Serializable
data class VideoStaffInfo (
    var mid: Int,
    var title: String,
    var face: String,
    var name: String
) : Parcelable