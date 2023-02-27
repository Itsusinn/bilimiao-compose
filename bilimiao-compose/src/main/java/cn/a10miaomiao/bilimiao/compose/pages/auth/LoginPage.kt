package cn.a10miaomiao.bilimiao.compose.pages.auth

import android.app.ProgressDialog
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import bilibili.main.community.reply.v1.ReplyOuterClass.Url
import cn.a10miaomiao.bilimiao.compose.R
import cn.a10miaomiao.bilimiao.compose.comm.diViewModel
import cn.a10miaomiao.bilimiao.compose.comm.localNavController
import cn.a10miaomiao.bilimiao.compose.comm.mypage.PageConfig
import com.a10miaomiao.bilimiao.comm.BilimiaoCommApp
import com.a10miaomiao.bilimiao.comm.entity.ResultInfo
import com.a10miaomiao.bilimiao.comm.entity.auth.LoginInfo
import com.a10miaomiao.bilimiao.comm.entity.auth.WebKeyInfo
import com.a10miaomiao.bilimiao.comm.entity.user.UserInfo
import com.a10miaomiao.bilimiao.comm.network.BiliApiService
import com.a10miaomiao.bilimiao.comm.network.MiaoHttp.Companion.gson
import com.a10miaomiao.bilimiao.comm.store.UserStore
import com.a10miaomiao.bilimiao.comm.utils.BiliGeetestUtil
import com.a10miaomiao.bilimiao.comm.utils.DebugMiao
import com.a10miaomiao.bilimiao.comm.utils.UrlUtil
import com.a10miaomiao.bilimiao.store.WindowStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.compose.rememberInstance
import org.kodein.di.instance
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

class LoginPageViewModel(
    override val di: DI,
): ViewModel(), DIAware, BiliGeetestUtil.GTCallBack {

    private var verifyUrl = ""
    private var recaptchaToken = ""

    private val fragment by instance<Fragment>()
    private val userStore by instance<UserStore>()

    private val biliGeetestUtil = BiliGeetestUtil(fragment.requireActivity(), fragment.lifecycle, this)
    private var tmpNavController: NavController? = null

    val loading = MutableStateFlow(false)
    val userName = MutableStateFlow("")
    val password = MutableStateFlow("")

    fun setUserName(value: String) {
        userName.value = value
    }

    fun setPassword(value: String) {
        password.value = value
    }

    fun stringLogin(
        navController: NavController,
        gt3Result: BiliGeetestUtil.GT3ResultBean? = null,
    ) = viewModelScope.launch(Dispatchers.IO) {
        try {
            if (userName.value.isBlank()) {
                withContext(Dispatchers.Main) {
                    alert("请输入用户名/邮箱/手机号")
                }
                return@launch
            }
            if (password.value.isBlank()) {
                withContext(Dispatchers.Main) {
                    alert("请输入密码")
                }
                return@launch
            }
            loading.value = true
            val webKey = getWebKey()
            val res = if (gt3Result == null) {
                // 不带验证码
                BiliApiService.authApi.oauth2Login(
                    username = userName.value,
                    passport = password.value,
                    key = webKey.key,
                    rhash = webKey.hash
                )
            } else {
                // 带验证码
                BiliApiService.authApi.oauth2Login(
                    username = userName.value,
                    passport = password.value,
                    key = webKey.key,
                    rhash = webKey.hash,
                    recaptchaToken = recaptchaToken,
                    geeValidate = gt3Result.geetest_validate,
                    geeSeccode = gt3Result.geetest_seccode,
                    geeChallenge = gt3Result.geetest_challenge,
                )
            }.awaitCall().gson<ResultInfo<LoginInfo>>()
            DebugMiao.log(res)
            withContext(Dispatchers.Main) {
                if (res.isSuccess) {
                    val loginInfo = res.data
                    if (loginInfo.status == 0) {
                        BilimiaoCommApp.commApp.saveAuthInfo(loginInfo)
                        authInfo()
                    } else if (loginInfo.status == 2){
                        alert("提示") {
                            setMessage(loginInfo.message)
                            setNegativeButton("取消", null)
                            setPositiveButton("请往验证") {_, _ ->
                                val map = UrlUtil.getQueryKeyValueMap(Uri.parse(loginInfo.url))
                                navController.navigate("bilimiao://auth/tel_verify?code=${map["tmp_token"]}&request_id=${map["request_id"]}&source=${map["source"]}")
                            }
                        }
                    } else {
                        alert(loginInfo.message ?: "登录失败：未知错误" + loginInfo.status) {
                            if (loginInfo.url != null) {
                                setNegativeButton("取消", null)
                                setPositiveButton("查看") {_, _ ->
                                    val intent = Intent(Intent.ACTION_VIEW)
                                    intent.data = Uri.parse(loginInfo.url)
                                    fragment.requireActivity().startActivity(intent)
                                }
                            }
                        }
                    }
                } else if (res.code == -105 && gt3Result == null) {
                    tmpNavController = navController
                    verifyUrl = res.data.url ?: ""
                    biliGeetestUtil.startCustomFlow()
                } else {
                    alert(res.message)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                alert(e.message ?: e.toString())
            }
        } finally {
            loading.value = false
        }
    }

    private suspend fun getWebKey(): WebKeyInfo {
        val res = BiliApiService.authApi
            .webKey()
            .awaitCall()
            .gson<ResultInfo<WebKeyInfo>>()
        if (res.isSuccess) {
            return res.data
        }
        throw Exception(res.message)
    }

    private suspend fun authInfo() {
        val res = withContext(Dispatchers.IO) {
            BiliApiService.authApi
                .account()
                .awaitCall()
                .gson<ResultInfo<UserInfo>>()
        }
        if (res.isSuccess) {
            userStore.setUserInfo(res.data)
            fragment.findNavController().popBackStack()
        } else {
            throw Exception(res.message)
        }
    }

    private fun alert(title: String) {
        alert(title) {}
    }

    @OptIn(ExperimentalContracts::class)
    private inline fun alert(title: String, initBuilder: (AlertDialog.Builder.() -> Unit)) {
        contract {
            callsInPlace(initBuilder, InvocationKind.EXACTLY_ONCE)
        }
        AlertDialog.Builder(fragment.requireActivity()).apply {
            setTitle(title)
            setNegativeButton("关闭", null)
            initBuilder()
        }.show()
    }

    override suspend fun onGTDialogResult(
        result: BiliGeetestUtil.GT3ResultBean
    ): Boolean {
        tmpNavController?.let {
            stringLogin(it, result)
        }
        return true
    }

    override suspend fun getGTApiJson(): JSONObject? {
        val queryMap = UrlUtil.getQueryKeyValueMap(Uri.parse(verifyUrl))
        if (queryMap.containsKey("recaptcha_token")) {
            recaptchaToken = queryMap["recaptcha_token"] ?: ""
            return JSONObject().apply {
                put("success", 1)
                put("challenge", queryMap["gee_challenge"] ?: "")
                put("gt", queryMap["gee_gt"] ?: "")
            }
        } else {
            alert("加载验证码出现错误")
            return null
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginPage(

) {
    PageConfig(title = "登录BILIBILI")

    val viewModel: LoginPageViewModel = diViewModel()
    val userStore: UserStore by rememberInstance()
    val windowStore: WindowStore by rememberInstance()
    val windowState = windowStore.stateFlow.collectAsState().value
    val windowInsets = windowState.getContentInsets(LocalView.current)

    val loading by viewModel.loading.collectAsState()
    val userName by viewModel.userName.collectAsState()
    val password by viewModel.password.collectAsState()

    val scrollState = rememberScrollState()
    val passwordFocusRequester = remember { FocusRequester() }
    var passwordIsFocus by remember { mutableStateOf(false) }

    val nav = localNavController()
    val startLoginClick = remember(nav) {
        {
            viewModel.stringLogin(nav)
            Unit
        }
    }
    val usernameKeyboardActions = remember(passwordFocusRequester) {
        KeyboardActions(
            onNext = {
                passwordFocusRequester.requestFocus()
            }
        )
    }
    val passwordKeyboardActions = remember(startLoginClick) {
        KeyboardActions(
            onDone = {
                startLoginClick()
            }
        )
    }

//    val test = remember(nav) {
//        {
//            nav.navigate("bilimiao://auth/tel_verify?code=147f6f734894caf16b9a5812c0f29111&request_id=9363b39b672348b6ab110808348601b9&source=risk")
//            Unit
//        }
//    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .height(80.dp)
                .padding(
                    top = windowInsets.topDp.dp,
                    bottom = windowInsets.bottomDp.dp,
                    start = windowInsets.leftDp.dp,
                    end = windowInsets.rightDp.dp,
                ),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            if (passwordIsFocus) {
                Image(
                    painter = painterResource(id = R.drawable.ic_22_hide),
                    contentDescription = "22娘遮眼",
                )
                Image(
                    painter = painterResource(id = R.drawable.ic_33_hide),
                    contentDescription = "33娘遮眼",
                )
            } else {
                Image(
                    painter = painterResource(id = R.drawable.ic_22),
                    contentDescription = "22娘",
                )
                Image(
                    painter = painterResource(id = R.drawable.ic_33),
                    contentDescription = "33娘",
                )
            }
        }
        Column(
            modifier = Modifier
                .widthIn(max = 600.dp)
                .verticalScroll(scrollState)
                .padding(horizontal = 10.dp)
        ) {
            Spacer(modifier = Modifier.height(windowInsets.topDp.dp))
            Box(
                modifier = Modifier
                    .height(90.dp)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "登录Bilibili",
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            TextField(
                label = {
                        Text(text = "用户名/邮箱/手机号")
                },
                value = userName,
                onValueChange = viewModel::setUserName,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = usernameKeyboardActions,
            )
            Spacer(modifier = Modifier.height(10.dp))
            TextField(
                label = {
                    Text(text = "密码")
                },
                value = password,
                onValueChange = viewModel::setPassword,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(passwordFocusRequester)
                    .onFocusChanged { passwordIsFocus = it.isFocused },
                singleLine = true,
                // 显示密码样式
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                // 显示密码样式
                visualTransformation = PasswordVisualTransformation(),
                keyboardActions = passwordKeyboardActions,
            )
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = startLoginClick,
                modifier = Modifier.fillMaxWidth(),
                enabled = !loading,
            ) {
                if (loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(12.dp),
                        strokeWidth = 1.dp,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
                Text(
                    modifier = Modifier.padding(horizontal = 5.dp),
                    text = "登录"
                )
            }
//            Button(onClick = test) {
//                Text(text = "测试")
//            }
            Spacer(modifier = Modifier.height(windowInsets.bottomDp.dp))

        }
    }

}