# Edge AI Apps

Edge AI Apps is a prototype package for adding two custom AI task surfaces to the Google AI Edge Gallery Android app. The repository contains Kotlin/Jetpack Compose/WebView task code, browser-preview frontends, Gallery integration files, and integration notes.

## Current Status

- Status: Android/Gallery integration prototype.
- Branch: `master`.
- Primary app surface: Google AI Edge Gallery custom tasks.
- Standalone browser previews: available for the WebView frontends.
- Standalone Android app: not provided. The Android code is intended to be copied or adapted into `google-ai-edge/gallery`.

The repository is not a generic multi-language AI project. It currently focuses on two Android task surfaces:

- `TextPlay`: a text adventure game surface using a WebView canvas frontend and Kotlin task/view-model integration.
- `EdgeCodex`: an on-device code assistant surface using a WebView editor frontend and Kotlin task/view-model integration.

## Repository Layout

```text
edge-ai-apps/
├── textplay/
│   ├── frontend/          # Browser-runnable HTML/CSS/JS game preview
│   ├── android/           # Kotlin task code for TextPlay
│   └── INTEGRATION_GUIDE.md
├── edgecodex/
│   ├── frontend/          # Browser-runnable HTML/CSS/JS editor preview
│   └── android/           # Kotlin task code for EdgeCodex
├── gallery-integration/
│   ├── customtasks/       # Gallery-namespaced Kotlin files
│   ├── model_allowlist_1_0_11_patched.json
│   └── PATTERNS.md
├── REQUIREMENTS.md
├── INTEGRATION_GUIDE.md
└── README.md
```

## Dependencies

For browser previews:

- Node.js 18 or later.
- `npx serve` or another static file server.

For Android/Gallery integration:

- Windows, macOS, or Linux development machine that can build Android projects.
- JDK 17.
- Android Studio or Android command-line tools.
- Android SDK 35 and build tools 35.0.0.
- A clone of `https://github.com/google-ai-edge/gallery`.
- Hugging Face OAuth configuration for Gallery model downloads.
- Android device or emulator capable of running the Gallery app and the selected models.

See `REQUIREMENTS.md` for detailed environment notes.

## Run Browser Previews From a Fresh Clone

```bash
git clone https://github.com/jupiternaut/edge-ai-apps.git
cd edge-ai-apps
npx serve textplay/frontend -l 3000
```

Open `http://localhost:3000` for the TextPlay preview.

In a second terminal:

```bash
cd edge-ai-apps
npx serve edgecodex/frontend -l 3001
```

Open `http://localhost:3001` for the EdgeCodex preview.

Browser previews use local JavaScript behavior and simulated AI responses. They do not run LiteRT-LM models.

## Build the Android Surface With AI Edge Gallery

This repository does not include a complete standalone Gradle root project for the Android app. Build through AI Edge Gallery:

```bash
git clone https://github.com/google-ai-edge/gallery.git
git clone https://github.com/jupiternaut/edge-ai-apps.git
```

Then copy or adapt the integration files:

```bash
cp -r edge-ai-apps/gallery-integration/customtasks/textplay \
  gallery/Android/src/app/src/main/java/com/google/ai/edge/gallery/customtasks/

cp -r edge-ai-apps/gallery-integration/customtasks/edgecodex \
  gallery/Android/src/app/src/main/java/com/google/ai/edge/gallery/customtasks/

mkdir -p gallery/Android/src/app/src/main/assets/textplay
cp edge-ai-apps/textplay/frontend/index.html \
   edge-ai-apps/textplay/frontend/style.css \
   edge-ai-apps/textplay/frontend/game.js \
   gallery/Android/src/app/src/main/assets/textplay/

mkdir -p gallery/Android/src/app/src/main/assets/edgecodex
cp edge-ai-apps/edgecodex/frontend/index.html \
   edge-ai-apps/edgecodex/frontend/style.css \
   edge-ai-apps/edgecodex/frontend/app.js \
   gallery/Android/src/app/src/main/assets/edgecodex/
```

Update Gallery's model allowlist using `gallery-integration/model_allowlist_1_0_11_patched.json`, configure Hugging Face OAuth in the Gallery project, then build Gallery:

```bash
cd gallery/Android/src
./gradlew assembleDebug
```

APK output is created by the Gallery project, not by this repository.

## Test

There is no automated test suite in this repository. Recommended checks:

1. Run both browser previews and confirm the TextPlay and EdgeCodex surfaces load.
2. Build the integrated Gallery app with `./gradlew assembleDebug`.
3. Install the debug APK on a supported Android device or emulator.
4. Download the required models inside Gallery.
5. Smoke test TextPlay commands and EdgeCodex code-assistant actions.

## UI and Data Boundaries

- Browser previews are frontend-only and use simulated AI behavior.
- Android inference depends on AI Edge Gallery, LiteRT-LM, the selected model entries, and successful in-app model downloads.
- The code is designed for on-device AI task surfaces; it is not a hosted web service.
- User prompts and code snippets should remain on-device when running through Gallery with local models.
- Model availability, Hugging Face OAuth setup, and device memory determine whether the Android AI flows work end to end.

## Screenshots

No screenshot is currently checked in. Suggested placeholder paths for future documentation:

```text
docs/screenshots/textplay-gallery-task.png
docs/screenshots/edgecodex-gallery-task.png
```

## Issues and Contributions

Use GitHub Issues for integration problems, build environment gaps, model allowlist mistakes, or documentation drift from the current Gallery APIs. Pull requests should keep `README.md`, `REQUIREMENTS.md`, `INTEGRATION_GUIDE.md`, and `gallery-integration/PATTERNS.md` consistent.
