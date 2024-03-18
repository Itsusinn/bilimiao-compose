package com.a10miaomiao.bilimiao.compose.state

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.a10miaomiao.bilimiao.comm.BilimiaoCommApp
import com.a10miaomiao.bilimiao.comm.entity.auth.LoginInfo

object MaxQualityState: MutableState<LoginInfo?> by mutableStateOf(null) {
    // TODO check out when the following code will be exec
    init {
        var self by this
        self = BilimiaoCommApp.commApp.loginInfo
    }
}