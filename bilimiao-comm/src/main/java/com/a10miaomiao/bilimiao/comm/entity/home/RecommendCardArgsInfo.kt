package com.a10miaomiao.bilimiao.comm.entity.home

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable


/*
 Empty for bangumi
 Only up_id up_name for picture
 */
@Parcelize
@Serializable
data class RecommendCardArgsInfo (
    val up_id: Long?,
    val up_name: String?,
    val rid: Int?,
    val rname: String?,
    val tid: Long?,
    val tname: String?,
    val aid: Long?,
    val roomId: Long?, // for live
    val online: Long? // for live
) : Parcelable