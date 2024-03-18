package com.a10miaomiao.bilimiao.comm.entity.home

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class HomeRecommendInfo (
    val items: List<RecommendCardInfo>,
) : Parcelable

@Parcelize
@Serializable
data class RecommendCardInfo (
    // small_cover_v2
    val card_type: String,
    // av/bv?/picture/bangumi/live
    val card_goto: String,
    val goto: String?,
    val param: String,
    val cover: String,
    val title: String,
    val uri: String,
    val idx: Long,
    val track_id: String?,
    val talk_back: String,
    val cover_left_text_1: String,
    val cover_left_icon_1: Int,
    val cover_left_1_content_description: String,
    val cover_left_text_2: String?,
    val cover_left_icon_2: Int?,
    val cover_left_2_content_description: String?,
    val cover_right_text: String?, // "0:19"
    val cover_right_content_description: String?, // "0分钟19秒"
    val can_play: Int?,
    val three_point_v2: List<ThreePointV2Info>,

    val desc_button: DescButtonInfo?,
    val goto_icon: GotoIconInfo?,
    val args: RecommendCardArgsInfo,
    val player_args: RecommendCardPlayerArgsInfo?,
) : Parcelable {
    @Parcelize
    @Serializable
    data class ThreePointV2Info(
        val title: String,
        /**
         * watch_later:稍后再看. feedback:反馈. dislike:不喜欢.
         */
        val type: String,
        val icon: String?,
        val reasons: List<DislikeReasonInfo>?
    ) : Parcelable

    @Parcelize
    @Serializable
    data class DislikeReasonInfo(
        val id: Int,
        val name: String,
        val toast: String,
    ) : Parcelable

    @Parcelize
    @Serializable
    data class DescButtonInfo(
        val type: Int,
        val text: String,
        val uri: String?,
        val event: String,
        val event_v2: String?,
    ) : Parcelable

    @Parcelize
    @Serializable
    data class GotoIconInfo(
        val icon_url: String,
        val icon_night_url: String,
        val icon_width: Int,
        val icon_height: Int,
    ) : Parcelable
}