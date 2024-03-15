package com.a10miaomiao.bilimiao.comm.entity.home

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class RecommendCardArgsInfo (
    val up_id: Long?,
    val up_name: String?,
    val rid: Int,
    val rname: String,
    val tid: Long,
    val tname: String,
    val aid: Long,
) : Parcelable