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
import com.dod.player.DodPlayerItem
import com.dod.player.DodPlayerSetting
import com.dod.player.DodPlayerView
import com.dod.player.SubtitleItem

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
                            mediaItems = createTestMediaItems()
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

    private fun createTestMediaItems(): List<DodPlayerItem> {
        return listOf(
            // --- Case 1: 여러 개의 자막을 가진 비디오 (Sintel) ---
            // BigBuckBunny -> Sintel 영상으로 교체
            DodPlayerItem(
                videoUrl = "http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4",
                subtitles = listOf(
                    SubtitleItem(
                        url = "https://raw.githubusercontent.com/yash-prajapati/media-files/master/subtitles/sintel/en.vtt",
                        displayName = "English (영어)"
                    ),
                    SubtitleItem(
                        url = "https://raw.githubusercontent.com/yash-prajapati/media-files/master/subtitles/sintel/fr.vtt",
                        displayName = "Français (프랑스어)"
                    ),
                    SubtitleItem(
                        url = "https://raw.githubusercontent.com/yash-prajapati/media-files/master/subtitles/sintel/es.vtt",
                        displayName = "Español (스페인어)"
                    )
                )
            ),

            // --- Case 2: 자막이 없는 비디오 (ForBiggerFun) ---
            // ElephantsDream -> ForBiggerFun 영상으로 교체
            DodPlayerItem(
                videoUrl = "http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4",
                subtitles = emptyList() // 자막 리스트를 비워둠
            ),

            // --- Case 3: 하나의 자막만 가진 비디오 (유지) ---
            // 이 케이스는 요청에 따라 그대로 유지합니다.
            DodPlayerItem(
                videoUrl = "https://storage.googleapis.com/exoplayer-test-media-1/mp4/dizzy-with-tx3g.mp4",
                subtitles = listOf(
                    SubtitleItem(
                        url = "https://storage.googleapis.com/exoplayer-test-media-1/webvtt/japanese.vtt",
                        displayName = "日本語 (일본어)"
                    )
                )
            ),

            // --- Case 4: 또 다른 영상, 자막 없음 (ForBiggerBlazes) ---
            // TearsOfSteel -> ForBiggerBlazes 영상으로 교체
            DodPlayerItem(
                videoUrl = "http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4"
            )
        )
    }
}