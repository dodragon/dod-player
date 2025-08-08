# DodPlayer

**Jetpack Compose를 위한 간단하고 커스터마이징 가능한 비디오 플레이어 라이브러리**

`DodPlayer`는 Google의 [ExoPlayer](https://exoplayer.dev/)를 기반으로, Jetpack Compose 환경에서 손쉽게 비디오 플레이어를 구현할 수 있도록 제작된 라이브러리입니다. 재생목록, 자막 선택, PIP 모드 등 필수적인 기능들을 복잡한 설정 없이 바로 사용할 수 있으며, 간단한 옵션 변경만으로 UI를 커스터마이징할 수 있습니다.

## ✨ 주요 기능

- **손쉬운 사용법**: Composable 함수 하나로 플레이어 UI를 즉시 생성합니다.
- **재생 목록 (Playlist)**: 여러 개의 비디오를 순차적으로 재생할 수 있습니다.
- **다국어 자막 지원**: 각 비디오마다 여러 개의 자막 파일을 연결하고, UI에서 손쉽게 선택할 수 있습니다.
- **PIP (Picture-in-Picture) 모드**: 간단한 설정만으로 화면 속 화면 기능을 지원합니다.
- **직관적인 UI 컨트롤**: 더블 탭으로 빨리감기, 전체화면 전환, 음소거 등 필수 컨트롤러 기능을 제공합니다.
- **간단한 커스터마이징**: 아이콘, 색상, 컨트롤러 구성 등 다양한 UI 요소를 직접 변경할 수 있습니다.
- **버퍼링 상태 표시**: 네트워크 상태가 불안정할 때 자동으로 로딩 아이콘을 표시하여 사용자 경험을 향상시킵니다.

## 🚀 사용법
```kotlin
implementation("com.github.dodragon:dod-player:${NEW_VERSION}")
```
`DodPlayerView` Composable을 화면에 추가하고, 재생할 미디어 정보를 `mediaItems` 파라미터로 전달하기만 하면 됩니다.

```kotlin
import com.dod.player.DodPlayerView
import com.dod.player.DodPlayerItem
import com.dod.player.SubtitleItem

// 1. 재생할 미디어 아이템 리스트를 생성합니다.
val mediaItems = listOf(
    DodPlayerItem(
        videoUrl = "https://your-video-url.com/video1.mp4",
        subtitles = listOf(
            SubtitleItem(
                url = "https://your-subtitle-url.com/en.vtt",
                displayName = "English"
            ),
            SubtitleItem(
                url = "https://your-subtitle-url.com/ko.srt",
                displayName = "한국어"
            )
        )
    ),
    DodPlayerItem(
        videoUrl = "https://your-video-url.com/video2.mp4"
        // 자막이 없는 경우, subtitles 파라미터를 생략하거나 빈 리스트를 전달합니다.
    )
)

// 2. DodPlayerView Composable에 리스트를 전달합니다.
DodPlayerView(mediaItems = mediaItems)
```

### 데이터 클래스 설명

- `DodPlayerItem`: 하나의 비디오 정보를 담는 클래스입니다.
    - `videoUrl`: `String` (필수) - 재생할 비디오의 URL입니다.
    - `subtitles`: `List<SubtitleItem>` (선택) - 해당 비디오에 포함될 자막 리스트입니다.

- `SubtitleItem`: 하나의 자막 정보를 담는 클래스입니다.
    - `url`: `String` (필수) - 자막 파일의 URL입니다. (vtt, srt 등 지원)
    - `language`: `String` (필수) - 자막의 언어 코드입니다. (예: "en", "ko")
    - `displayName`: `String` (필수) - 자막 선택 UI에 표시될 이름입니다. (예: "English", "한국어")

## 🎨 커스터마이징

글로벌 설정 객체인 `DodPlayerSetting`의 프로퍼티 값을 변경하여 플레이어의 전체적인 디자인과 동작을 손쉽게 커스터마이징할 수 있습니다. 앱의 `Application` 클래스나 초기 실행 시점에서 값을 변경하는 것을 권장합니다.

```kotlin
// 예시: 앱의 테마에 맞게 아이콘과 색상 변경
DodPlayerSetting.playIconId = R.drawable.my_custom_play_icon
DodPlayerSetting.enableColor = Color(0xFF1DB954) // Spotify Green
DodPlayerSetting.controllerBackgroundColorAlpha = 0.6f
```

### 아이콘 관련 설정

| 프로퍼티 | 설명 | 기본값 |
|----------|------|--------|
| `playIconId` | 재생 버튼 아이콘 | `R.drawable.play_circle` |
| `pauseIconId` | 일시정지 버튼 아이콘 | `R.drawable.pause_circle` |
| `closeIconId` | 닫기 버튼 아이콘 | `R.drawable.window_close` |
| `subtitleIconId` | 자막 버튼 아이콘 | `R.drawable.subtitles` |
| `skipBackwardIconId` | 뒤로 빨리감기 아이콘 | `R.drawable.skip_backward` |
| `skipForwardIconId` | 앞으로 빨리감기 아이콘 | `R.drawable.skip_forward` |
| `volumeOnIconId` | 볼륨 켜기 아이콘 | `R.drawable.volume_high` |
| `volumeOffIconId` | 볼륨 끄기 아이콘 | `R.drawable.volume_off` |
| `fullscreenOnIconId` | 전체화면 켜기 아이콘 | `R.drawable.fullscreen` |
| `fullscreenOffIconId` | 전체화면 끄기 아이콘 | `R.drawable.fullscreen_exit` |

### 색상 관련 설정

| 프로퍼티 | 설명 | 기본값 |
|----------|------|--------|
| `enableColor` | 활성화된 아이콘 및 UI 요소 색상 | `Color.White` |
| `disableColor` | 비활성화된 아이콘 색상 | `Color.DarkGray` |
| `timeTextColor` | 재생 시간 텍스트 색상 | `Color.White` |
| `controllerBackgroundColor` | 컨트롤러 배경 색상 | `Color.Black` |
| `controllerBackgroundColorAlpha` | 컨트롤러 배경 투명도 (0.0f ~ 1.0f) | `0.4f` |

### 기능 및 동작 설정

| 프로퍼티 | 설명 | 기본값 |
|----------|------|--------|
| `needClose` | 컨트롤러에 닫기 버튼 표시 여부 | `true` |
| `needFullscreen` | 전체화면 버튼 표시 여부 | `true` |
| `needVolumeControl` | 볼륨 버튼 표시 여부 | `true` |
| `needPip` | PIP 모드 사용 여부 | `true` |
| `needTimeText` | 재생 시간 텍스트 표시 여부 | `true` |
| `timeFormat` | 재생 시간 텍스트 형식 (람다식) | `"${duration}/${wholeTime}"` |
| `backwardSec` | 뒤로 빨리감기 시간 (초) | `10` |
| `forwardSec` | 앞으로 빨리감기 시간 (초) | `10` |
| `screenRatioWidth / screenRatioHeight` | 플레이어 가로/세로 비율 | `16 / 9` |

## ⚙️ PIP 모드 설정

### 1. Activity 코드 추가

플레이어를 사용하는 Activity에 아래 코드를 추가하여, 사용자가 앱을 나갈 때 PIP 모드로 전환되도록 합니다.

```kotlin
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
```

### 2. AndroidManifest.xml 설정

Activity의 매니페스트 태그에 아래 속성을 추가하여 PIP 모드를 지원하고, 화면 방향 변경 시 Activity가 재생성되지 않도록 합니다.

```xml
<activity
    android:name=".MainActivity"
    android:supportsPictureInPicture="true"
    android:configChanges="screenSize|smallestScreenSize|screenLayout|orientation">
    ...
</activity>
```


---


# DodPlayer

**A simple and customizable video player library for Jetpack Compose**

`DodPlayer` is a library based on Google's [ExoPlayer](https://exoplayer.dev/), designed to help you easily implement a video player in Jetpack Compose. It supports essential features like playlists, subtitle selection, and PIP mode out of the box, with simple customization options for UI.

## ✨ Features

- **Easy to use**: Instantly render the player UI with a single composable function.
- **Playlist support**: Play multiple videos sequentially.
- **Multilingual subtitles**: Link multiple subtitle files per video, and switch between them easily in the UI.
- **PIP (Picture-in-Picture) mode**: Enable floating video with minimal setup.
- **Intuitive UI controls**: Provides essential control functions like double-tap to seek, fullscreen toggle, mute, etc.
- **Simple customization**: Modify UI elements like icons, colors, and layout configuration easily.
- **Buffering indicator**: Displays a loading indicator automatically during unstable network conditions.

## 🚀 Usage
```kotlin
implementation("com.github.dodragon:dod-player:${NEW_VERSION}")
```
Use the `DodPlayerView` composable and provide a list of media items through the `mediaItems` parameter.

```kotlin
import com.dod.player.DodPlayerView
import com.dod.player.DodPlayerItem
import com.dod.player.SubtitleItem

// 1. Create a list of media items to play
val mediaItems = listOf(
    DodPlayerItem(
        videoUrl = "https://your-video-url.com/video1.mp4",
        subtitles = listOf(
            SubtitleItem(
                url = "https://your-subtitle-url.com/en.vtt",
                displayName = "English"
            ),
            SubtitleItem(
                url = "https://your-subtitle-url.com/ko.srt",
                displayName = "Korean"
            )
        )
    ),
    DodPlayerItem(
        videoUrl = "https://your-video-url.com/video2.mp4"
        // If no subtitles, you can omit the `subtitles` parameter or pass an empty list.
    )
)

// 2. Pass the list to the DodPlayerView composable
DodPlayerView(mediaItems = mediaItems)
```

### Data class overview

- `DodPlayerItem`: Represents a single video item.
    - `videoUrl`: `String` (required) - URL of the video to play.
    - `subtitles`: `List<SubtitleItem>` (optional) - Subtitles associated with the video.

- `SubtitleItem`: Represents a single subtitle track.
    - `url`: `String` (required) - Subtitle file URL (supports vtt, srt, etc).
    - `language`: `String` (required) - Language code for subtitle (e.g., "en", "ko").
    - `displayName`: `String` (required) - Display name in the subtitle selector.

## 🎨 Customization

You can customize the player's design and behavior globally by modifying the properties of the `DodPlayerSetting` object. It’s recommended to set these in your Application class or during app initialization.

```kotlin
// Example: Customize icons and colors to match your app theme
DodPlayerSetting.playIconId = R.drawable.my_custom_play_icon
DodPlayerSetting.enableColor = Color(0xFF1DB954) // Spotify Green
DodPlayerSetting.controllerBackgroundColorAlpha = 0.6f
```

### Icon customization

| Property | Description | Default |
|----------|-------------|---------|
| `playIconId` | Play button icon | `R.drawable.play_circle` |
| `pauseIconId` | Pause button icon | `R.drawable.pause_circle` |
| `closeIconId` | Close button icon | `R.drawable.window_close` |
| `subtitleIconId` | Subtitle button icon | `R.drawable.subtitles` |
| `skipBackwardIconId` | Rewind button icon | `R.drawable.skip_backward` |
| `skipForwardIconId` | Fast forward button icon | `R.drawable.skip_forward` |
| `volumeOnIconId` | Volume on icon | `R.drawable.volume_high` |
| `volumeOffIconId` | Volume off icon | `R.drawable.volume_off` |
| `fullscreenOnIconId` | Fullscreen on icon | `R.drawable.fullscreen` |
| `fullscreenOffIconId` | Fullscreen off icon | `R.drawable.fullscreen_exit` |

### Color customization

| Property | Description | Default |
|----------|-------------|---------|
| `enableColor` | Active icon and UI color | `Color.White` |
| `disableColor` | Inactive icon color | `Color.DarkGray` |
| `timeTextColor` | Playback time text color | `Color.White` |
| `controllerBackgroundColor` | Background color of the controller | `Color.Black` |
| `controllerBackgroundColorAlpha` | Controller background transparency (0.0f ~ 1.0f) | `0.4f` |

### Functional settings

| Property | Description | Default |
|----------|-------------|---------|
| `needClose` | Show close button | `true` |
| `needFullscreen` | Show fullscreen button | `true` |
| `needVolumeControl` | Show volume control button | `true` |
| `needPip` | Enable PIP mode | `true` |
| `needTimeText` | Show playback time text | `true` |
| `timeFormat` | Playback time format (lambda) | `"${duration}/${wholeTime}"` |
| `backwardSec` | Seek backward seconds | `10` |
| `forwardSec` | Seek forward seconds | `10` |
| `screenRatioWidth / screenRatioHeight` | Default player aspect ratio | `16 / 9` |

## ⚙️ PIP Mode Setup

### 1. Activity code

Add the following to your Activity to enable PIP mode when the user leaves the app.

```kotlin
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
```

### 2. AndroidManifest.xml

Add the following attributes to the `<activity>` tag to support PIP and prevent recreation on orientation change.

```xml
<activity
    android:name=".MainActivity"
    android:supportsPictureInPicture="true"
    android:configChanges="screenSize|smallestScreenSize|screenLayout|orientation">
    ...
</activity>
```

## ⚖️ License

`DodPlayer` is licensed under the Apache License, Version 2.0. See the [LICENSE](LICENSE) file for more details.

```kotlin
Copyright 2024 [Your Name or Company Name]

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.