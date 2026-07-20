# Android app

## Huawei-compatible first version

Version `0.2.1-huawei` uses standard Android APIs and does not require Google Play Services. It supports:

- Android and Huawei manufacturer detection
- direct Android `SpeechRecognizer` service integration with microphone permission handling
- typed input when no speech service is installed
- local mock action planning
- connection to a separate agent backend
- explicit confirmation before any action is accepted

Huawei speech recognition availability depends on the voice service installed and enabled on the device. Typed input always remains available.

## Run in mock mode

Open this `android` directory in Android Studio and run the `app` configuration. With no backend URL, the app uses a deterministic local mock.

## Run with the backend

Start the backend on the development computer, then build with:

```bash
./gradlew installDebug -PagentBaseUrl=http://10.0.2.2:8080
```

`10.0.2.2` maps an Android Studio emulator to the host computer. For Huawei Cloud Debugging or a physical Huawei phone, provide a reachable HTTPS endpoint.

The OpenAI API key stays in the backend environment and is never placed in the APK.

## Huawei testing

Use the Android emulator for the initial UI and mock-flow test. Upload the GitHub Actions artifact `voice-first-driving-assistant-huawei-debug` to Huawei AppGallery Connect Cloud Debugging to validate it on a real remote Huawei device.
