package com.a10miaomiao.bilimiao.comm.entity.home

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class RecommendCardPlayerArgsInfo (
    val aid: Long,
    val cid: Long,
    val duration: Long,
    val type: String,
) : Parcelable