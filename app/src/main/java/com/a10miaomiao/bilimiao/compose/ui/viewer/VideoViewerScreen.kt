@file:Suppress("NAME_SHADOWING")

package com.a10miaomiao.bilimiao.compose.ui.viewer

import androidx.annotation.OptIn
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.ui.PlayerView
import com.a10miaomiao.bilimiao.comm.apis.PlayerAPI
import com.a10miaomiao.bilimiao.comm.entity.player.PlayerV2Info
import com.a10miaomiao.bilimiao.comm.entity.video.VideoInfo
import com.a10miaomiao.bilimiao.comm.network.BiliApiService
import com.a10miaomiao.bilimiao.comm.utils.Log
import com.a10miaomiao.bilimiao.compose.ui.tool.customDataSourceFactory
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.async


@OptIn(UnstableApi::class)
@Composable
@Destination
// com/a10miaomiao/bilimiao/page/video/VideoPagesFragment.kt:40
// com/a10miaomiao/bilimiao/widget/player/DanmakuVideoPlayer.kt:51
fun VideoViewerScreen(
    navigator: DestinationsNavigator,
    type: String,
    id: String
) = Box (modifier = Modifier.fillMaxSize()){
    Text(text = "$type $id")
    if (type!="av") return@Box
    val context = LocalContext.current
    val exoDataSourceFactory = remember {
        customDataSourceFactory(context)
    }
    val exoPlayer = remember {
        val audioAttributes: AudioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .build()
        ExoPlayer.Builder(context, exoDataSourceFactory).setAudioAttributes(audioAttributes, true).build()
    }
    var videoInfo by rememberSaveable {
        mutableStateOf<VideoInfo?>(null)
    }
    var playerInfo by rememberSaveable {
        mutableStateOf<PlayerV2Info?>(null)
    }
    var dash by rememberSaveable {
        mutableStateOf<PlayerAPI.Dash?>(null)
    }
    LaunchedEffect(Unit){
        videoInfo = BiliApiService.videoAPI.infoAwait(id, type).data
        val videoInfo = videoInfo!!
        val info = async {
            BiliApiService.playerAPI.getPlayerInfoAsync(videoInfo.aid, videoInfo.cid.toString()).data
        }
        val url = async {
            BiliApiService.playerAPI.getVideoPlayUrl(videoInfo.aid, videoInfo.cid.toString())
        }
        playerInfo = info.await()

        val playUrl = url.await().getOrThrow()
        dash = playUrl.dash
        Log.info { dash.toString() }
    }
    LaunchedEffect(dash) {
        if (dash == null) return@LaunchedEffect
        val dash = dash!!
        val video = dash.video.first()
        val audio = dash.audio?.first()
        Log.debug { "Playing $video" }
        val media = if (audio == null){
            exoDataSourceFactory.createMediaSource(MediaItem.fromUri(video.base_url))
        } else {
            // ProgressiveMediaSource
            val video = exoDataSourceFactory.createMediaSource(MediaItem.fromUri(video.base_url))
            val audio = exoDataSourceFactory.createMediaSource(MediaItem.fromUri(audio.base_url))
            MergingMediaSource(video, audio)
        }
        exoPlayer.setMediaSource(media)

        exoPlayer.playWhenReady = true
        exoPlayer.prepare()
    }
    var lifecycle by remember {
        mutableStateOf(Lifecycle.Event.ON_CREATE)
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            lifecycle = event
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    if (videoInfo != null && playerInfo != null && dash != null) {
        val videoInfo = videoInfo!!
        val playerInfo = playerInfo!!
        val playUrl = dash!!
        AndroidView(
            factory = { context ->
                PlayerView(context).also {
                    it.player = exoPlayer
                }
            },
            update = {
                when (lifecycle) {
                    Lifecycle.Event.ON_PAUSE -> {

                        it.onPause()
                        it.player?.pause()
                    }
                    Lifecycle.Event.ON_RESUME -> {
                        it.onResume()
                    }
                    else -> Unit
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16 / 9f)
        )

    }

}


