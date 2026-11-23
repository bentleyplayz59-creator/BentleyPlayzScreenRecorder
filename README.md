# BentleyPlayz Video Voiceover

Android MP4 voiceover editor by BentleyPlayz. This replaces the former screen-recorder app while retaining the same Android application ID and release-signing configuration, so signed releases can update the existing installation.

## Features

- Import MP4 video through Android's file picker.
- Preview video inside the app.
- Record microphone narration while the video plays from the beginning.
- Review the recorded voiceover.
- Independently adjust original video audio and voiceover gain.
- Export a new MP4 with the original video and mixed audio.
- Save exports to Movies/BentleyPlayz Video Voiceover.
- Keep the imported source file unchanged.

## Build

GitHub Actions builds the debug APK on pushes to `main` and manual workflow dispatch. Signed releases use the existing repository secrets:

- `RELEASE_KEYSTORE_BASE64`
- `RELEASE_KEYSTORE_PASSWORD`
- `RELEASE_KEY_ALIAS`
- `RELEASE_KEY_PASSWORD`

The Android application ID remains `com.bentleyplayz.screenrecorder`. Version 2.0.0 uses version code 4.

## Use

1. Import an MP4.
2. Use the player controls to preview it.
3. Select Record Voiceover. Recording starts at the beginning and video playback starts.
4. Stop the recording when narration is complete.
5. Review the narration if needed.
6. Set the original/game audio and voiceover levels independently.
7. Export Mixed MP4. The finished file is saved in the Movies folder listed above.

Use headphones while recording to reduce the chance that the video's speaker audio is picked up by the microphone. The source MP4 is not overwritten.

## Validation

The GitHub Actions workflow compiles the debug app, builds a signed release APK, and verifies its signature. Actual playback, microphone quality, audio alignment, and export compatibility still require testing on an Android device.
