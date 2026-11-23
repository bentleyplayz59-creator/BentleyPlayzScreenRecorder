# BentleyPlayz Screen Recorder

Native Android screen recorder by BentleyPlayz.

## Implemented

- Android MediaProjection screen capture.
- H.264 video encoding to MP4 using MediaCodec.
- AAC audio encoding.
- Microphone capture through AudioRecord.
- Android 10+ playback capture for supported media/game audio.
- Mixed microphone and internal playback audio in one mono AAC track.
- Draggable floating BP control with Start/Open, Pause/Resume, Stop and Hide actions.
- Pause and resume capture.
- Foreground recording notification with Stop action.
- 720p/30, 1080p/30 and 1080p/60 quality presets.
- Recording library with play, share and delete actions.
- Copy completed MP4 recordings to Movies/BentleyPlayz Screen Recorder.
- Pillow-generated custom app icon.

## Audio capture compatibility

Internal playback capture is subject to Android's AudioPlaybackCapture rules. The source app must allow capture, and protected audio, voice calls, and apps that opt out cannot be recorded internally. Microphone capture remains independent. The app combines the two captured PCM inputs into one AAC audio track.

## Build

GitHub Actions builds the debug APK on pushes to `main` and manual workflow dispatch. The artifact is named `BentleyPlayzScreenRecorder-debug`.

## Device verification

The CI build verifies compilation and APK packaging. Real-device testing is still required for OEM-specific projection behavior, audio-source policies, pause/resume synchronization, and playback compatibility.
