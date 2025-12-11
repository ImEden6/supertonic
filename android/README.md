# Supertonic Android App

Native Android app for Supertonic - a lightning-fast, on-device text-to-speech system.

## Features

- **Text-to-Speech Generation**: Convert text to natural speech entirely on-device
- **Multiple Voice Styles**: 10 voice options (M1-M5, F1-F5)
- **Adjustable Parameters**: Control speech speed (0.5x-2.0x) and quality (1-20 denoising steps)
- **Audio Playback**: Built-in player with play/pause/stop controls
- **Download Audio**: Save generated speech as WAV files
- **Batch Conversion**: Convert multiple text files at once
- **Material Design 3**: Modern UI with 4 color palettes (Purple, Teal, Orange, Pink)
- **Dark Mode**: Full dark mode support
- **Preferences**: Save your preferred settings

## Requirements

- Android 8.0 (API 26) or higher
- ~300MB storage for models and app

## Setup

### 1. Copy ONNX Models

The app requires the ONNX models to be placed in the assets directory:

```bash
# From the supertonic root directory
mkdir -p android/app/src/main/assets
cp -r assets/onnx android/app/src/main/assets/
cp -r assets/voice_styles android/app/src/main/assets/
```

Or on Windows (Command Prompt):
```cmd
mkdir android\app\src\main\assets
xcopy assets\onnx android\app\src\main\assets\onnx /E /I
xcopy assets\voice_styles android\app\src\main\assets\voice_styles /E /I
```

### 2. Build the App

```bash
cd android
./gradlew assembleDebug
```

The APK will be located at `app/build/outputs/apk/debug/app-debug.apk`

### 3. Install on Device

```bash
./gradlew installDebug
```

Or install the APK manually via ADB:
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

## Project Structure

```
android/
├── app/
│   ├── src/main/
│   │   ├── java/com/supertone/supertonic/
│   │   │   ├── MainActivity.kt          # Main entry point with navigation
│   │   │   ├── PreferencesManager.kt    # User settings storage
│   │   │   ├── tts/                     # TTS engine (ported from Java)
│   │   │   │   ├── Config.kt            # Configuration data classes
│   │   │   │   ├── Style.kt             # Voice style management
│   │   │   │   ├── TextToSpeech.kt      # Main inference engine
│   │   │   │   ├── UnicodeProcessor.kt  # Text normalization
│   │   │   │   ├── TextChunker.kt       # Long text splitting
│   │   │   │   └── WavWriter.kt         # Audio file output
│   │   │   ├── viewmodel/
│   │   │   │   └── TTSViewModel.kt      # UI state management
│   │   │   └── ui/
│   │   │       ├── theme/               # Material Design 3 theming
│   │   │       └── screens/             # Compose UI screens
│   │   ├── assets/                      # ONNX models (not in git)
│   │   └── res/                         # Android resources
│   └── build.gradle.kts
├── build.gradle.kts
├── settings.gradle.kts
└── gradle.properties
```

## Usage

1. **Launch the app** - Models will load automatically on first launch
2. **Enter text** - Type or paste the text you want to convert to speech
3. **Select voice** - Choose from 10 available voice styles
4. **Adjust parameters** - Set speech speed and quality
5. **Generate** - Tap the Generate button to create speech
6. **Play/Download** - Listen to the audio or save it as a WAV file

## License

This project follows the same license as the main Supertonic repository.
See the root LICENSE file for details.
