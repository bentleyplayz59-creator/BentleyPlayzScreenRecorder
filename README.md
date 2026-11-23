# BentleyPlayz Screen Recorder

A native Android screen recorder project by BentleyPlayz.

## Planned capabilities

- Android MediaProjection screen capture
- Internal playback audio capture where the source app permits it
- Microphone capture
- Synchronized audio/video MP4 output
- Draggable floating recording control
- Recording pause, resume and stop
- Resolution and frame-rate preferences
- Local recording library

## Build

GitHub Actions builds a debug APK on every push to `main` and on manual workflow dispatch.

The app currently contains the branded dashboard and draggable floating-ball foundation. The MediaProjection and synchronized audio/video recording engine is the next implementation milestone.

## Branding

The app icon is generated during CI using Python Pillow from `tools/generate_logo.py`.
