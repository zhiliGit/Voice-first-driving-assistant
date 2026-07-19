# Android app

## Run in mock mode

Open this `android` directory in Android Studio and run the `app` configuration. With no backend URL, the app uses a deterministic local mock so the confirmation flow is immediately demonstrable.

## Run with the GPT-5.6 backend

Start the backend on the development computer, then build with:

```bash
./gradlew installDebug -PagentBaseUrl=http://10.0.2.2:8080
```

`10.0.2.2` maps the Android emulator to the host machine. For a physical device, provide a reachable HTTPS endpoint or the development computer's LAN address.

The OpenAI API key stays in the backend environment; it is never placed in the Android app.
