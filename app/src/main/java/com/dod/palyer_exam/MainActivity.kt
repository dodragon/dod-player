package com.dod.palyer_exam

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.dod.palyer_exam.ui.theme.Palyer_examTheme
import com.dod.player.DodPlayerSetting
import com.dod.player.DodPlayerView

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        /*VideoPlayerSetting.apply {
            needClose = true
            onClose = {
                finish()
            }
        }*/

        enableEdgeToEdge()
        setContent {
            Palyer_examTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        DodPlayerView(
                            activity = this@MainActivity,
                            videoUrl = "https://storage.googleapis.com/exoplayer-test-media-1/mp4/dizzy-with-tx3g.mp4",
                            subtitleUrl = "https://storage.googleapis.com/exoplayer-test-media-1/webvtt/japanese.vtt"
                        )
                    }
                }
            }
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        DodPlayerSetting.enterPipMode(this)
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        DodPlayerSetting.setPipMode(isInPictureInPictureMode)
    }
}