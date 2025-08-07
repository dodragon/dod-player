package com.dod.player

import android.app.Activity
import android.app.PictureInPictureParams
import android.os.Build
import android.util.Rational
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object DodPlayerSetting {

    //재생버튼 아이콘
    var playIconId = R.drawable.play_circle
    //중지버튼 아이콘
    var pauseIconId = R.drawable.pause_circle
    //닫기버튼 아이콘
    var closeIconId = R.drawable.window_close
    //자막 on/off 버튼 아이콘
    var subtitleIconId = R.drawable.subtitles
    //뒤로 빨리감기 버튼 아이콘
    var skipBackwardIconId = R.drawable.skip_backward
    //앞으로 빨리감기 버튼 아이콘
    var skipForwardIconId = R.drawable.skip_forward
    //볼륨 on 아이콘
    var volumeOnIconId = R.drawable.volume_high
    //볼륨 off 아이콘
    var volumeOffIconId = R.drawable.volume_off
    //전체화면 on 아이콘
    var fullscreenOnIconId = R.drawable.fullscreen
    //전체화면 off 아이콘
    var fullscreenOffIconId = R.drawable.fullscreen_exit

    //color
    var enableColor = Color.White
    var disableColor = Color.DarkGray
    var timeTextColor = Color.White
    var controllerBackgroundColor = Color.Black
    var controllerBackgroundColorAlpha = 0.4f

    //닫기 버튼 필요한 경우
    var needClose = true

    //전체화면 필요한 경우
    var needFullscreen = true
    //볼륨 on/off 필요한 경우
    var needVolumeControl = true

    //Pip 필요한 경우
    var needPip = true

    //타이머 포멧
    var needTimeText = true
    var timeFormat: (duration: String, wholeTime: String) -> String = { duration, wholeTime ->
        "${duration}/${wholeTime}"
    }

    //뒤로 빨리감기 초
    var backwardSec = 10
    //앞으로 빨리감기 초
    var forwardSec = 10

    //가로 비율
    var screenRatioWidth = 16
    //세로 비율
    var screenRatioHeight = 9

    //ExoPlayer isPlaying
    var isPlaying = false

    private val _isPipMode = MutableStateFlow(false)
    val isPipMode = _isPipMode.asStateFlow()

    fun setPipMode(
        isPipMode: Boolean
    ) {
        _isPipMode.value = isPipMode
    }

    fun enterPipMode(
        activity: Activity
    ) {
        //오레오(26) 미만 버전에서는 pip 지원하지 않음
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if(needPip && isPlaying) {
                val params =  PictureInPictureParams.Builder()
                    .setAspectRatio(Rational(screenRatioWidth, screenRatioHeight))
                    .build()

                activity.enterPictureInPictureMode(params)
            }
        }
    }
}