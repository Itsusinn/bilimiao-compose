package com.a10miaomiao.bilimiao.comm.apis

import com.a10miaomiao.bilimiao.comm.network.ApiHelper
import com.a10miaomiao.bilimiao.comm.network.BiliApiService
import com.a10miaomiao.bilimiao.comm.network.MiaoHttp

class CommentApi() {

    /**
     * 视频评论
     */
    fun mainList(
        aid: String,
        sort: Int,
        type: Int,
        pageNum: Int,
        pageSize: Int
    ) = MiaoHttp.request {
        url = BiliApiService.biliApi(
            "x/v2/reply/main",
            "oid" to aid,
            "plat" to "2",
            "sort" to sort.toString(),
            "pn" to pageNum.toString(),
            "ps" to pageSize.toString(),
            "type" to type.toString(),
        )
    }

    /**
     * 评论回复列表
     */
    fun replyList(
        oid: String,
        rpid: String,
        type: Int,
        pageNum: Int,
        pageSize: Int
    ) = MiaoHttp.request {
        url = BiliApiService.biliApi(
            "x/v2/reply/main",
            "oid" to oid,
            "plat" to "2",
            "root" to rpid,
            "sort" to "0",
            "type" to type.toString(),
            "pn" to pageNum.toString(),
            "ps" to pageSize.toString(),
        )
    }

    /**
     * 点赞/取消赞
     */
    fun action(
        type: Int, // 评论区类型代码
        oid: String, // 目标评论区id
        rpid: String, // 目标评论rpid
        action: Int, // 操作代码 0：取消赞 1：点赞
    ) = MiaoHttp.request {
        url = BiliApiService.biliApi("x/v2/reply/action")
        method = MiaoHttp.POST
        formBody = ApiHelper.createParams(
            "type" to type.toString(),
            "oid" to oid,
            "rpid" to rpid,
            "action" to action.toString(),
        )
    }


}