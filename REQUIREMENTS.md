# EdgeAI Apps - Build Requirements

## 1. Development Environment

### JDK 17 (Required)
```bash
# Windows (winget)
winget install --id EclipseAdoptium.Temurin.17.JDK

# Verify
java -version
# Expected: openjdk version "17.x.x"
```

### Android SDK (Required)
```bash
# Set ANDROID_HOME
export ANDROID_HOME=~/android-sdk

# Download command-line tools
# https://developer.android.com/studio#command-line-tools-only
curl -L -o cmdline-tools.zip https://dl.google.com/android/repository/commandlinetools-win-11076708_latest.zip
unzip cmdline-tools.zip -d $ANDROID_HOME/cmdline-tools/latest

# Accept licenses
yes | sdkmanager --licenses

# Install required SDK components
sdkmanager "platforms;android-35" "build-tools;35.0.0" "platform-tools"
```

### Environment Variables
```bash
# Add to ~/.bashrc or ~/.zshrc
export JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-17.0.18.8-hotspot"
export ANDROID_HOME=~/android-sdk
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH"
```

### Actual Installed Paths (Windows 11)
These are the verified paths on the development machine:
```
JAVA_HOME:     C:\Program Files\Eclipse Adoptium\jdk-17.0.18.8-hotspot
ANDROID_HOME:  C:\Users\gengr\android-sdk
SDK Platform:  C:\Users\gengr\android-sdk\platforms\android-35
Build Tools:   C:\Users\gengr\android-sdk\build-tools\35.0.0
Platform Tools:C:\Users\gengr\android-sdk\platform-tools
Gallery Clone: C:\Users\gengr\projects\edge-ai-gallery
Prototypes:    C:\Users\gengr\projects\edge-ai-apps
```

## 2. AI Edge Gallery Base Project

```bash
git clone https://github.com/google-ai-edge/gallery.git
cd gallery/Android/src
```

### HuggingFace OAuth (Required for model downloads)
1. Create a HuggingFace Developer Application: https://huggingface.co/docs/hub/oauth
2. Get `Client ID` and `Redirect URI`
3. Update `ProjectConfig.kt`:
   ```kotlin
   val clientId = "YOUR_CLIENT_ID"
   val redirectUri = "YOUR_REDIRECT_URI"
   ```
4. Update `app/build.gradle.kts`:
   ```kotlin
   manifestPlaceholders["appAuthRedirectScheme"] = "YOUR_REDIRECT_SCHEME"
   ```

## 3. Integration Steps

### Copy TextPlay files
```bash
# Kotlin source (already adapted to gallery package namespace)
cp -r gallery-integration/customtasks/textplay \
  gallery/Android/src/app/src/main/java/com/google/ai/edge/gallery/customtasks/

# Frontend assets
cp textplay/frontend/{index.html,style.css,game.js} \
  gallery/Android/src/app/src/main/assets/textplay/
```

### Copy EdgeCodex files
```bash
cp -r gallery-integration/customtasks/edgecodex \
  gallery/Android/src/app/src/main/java/com/google/ai/edge/gallery/customtasks/

cp edgecodex/frontend/{index.html,style.css,app.js} \
  gallery/Android/src/app/src/main/assets/edgecodex/
```

### Update model allowlist
```bash
cp gallery-integration/model_allowlist_1_0_11_patched.json \
  gallery/model_allowlists/1_0_11.json
```

## 4. Build

```bash
cd gallery/Android/src
./gradlew assembleDebug
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`

## 5. Known Build Issues

### KAPT annotation errors for TextPlayTools.kt
The `@Tool` / `@ToolParam` annotations must import from:
```kotlin
import com.google.ai.edge.litertlm.Tool      // NOT .tools.Tool
import com.google.ai.edge.litertlm.ToolParam  // NOT .tools.ToolParam
import com.google.ai.edge.litertlm.ToolSet    // NOT .tools.ToolSet
```
Return type must be `Map<String, Any>` (not `Map<String, String>`).

### TextPlayViewModel / EdgeCodexViewModel imports
These files reference Gallery internal APIs. Adapt imports to match the actual Gallery SDK version:
- `LlmChatModelHelper` → check `com.google.ai.edge.gallery.runtime`
- `Model` → check `com.google.ai.edge.gallery.data`
- `Engine` / `Conversation` → check `com.google.ai.edge.litertlm`

### TextPlayScreen / EdgeCodexScreen WebView
The `WebViewAssetLoader` path must match asset directory:
```kotlin
loadUrl("https://appassets.androidplatform.net/assets/textplay/index.html")
loadUrl("https://appassets.androidplatform.net/assets/edgecodex/index.html")
```

## 6. Required Models (download in-app)

| Model | Size | Usage | Min RAM |
|-------|------|-------|---------|
| FunctionGemma 270M | ~289 MB | TextPlay actions | 6 GB |
| Gemma 4 E2B | ~2.6 GB | EdgeCodex + TextPlay dialogue | 8 GB |
| Gemma 4 E4B (optional) | ~3.7 GB | Advanced code analysis | 12 GB |

## 7. Browser-Only Testing (No Android needed)

```bash
# TextPlay game
npx serve textplay/frontend -l 3000
# Open http://localhost:3000

# EdgeCodex IDE
npx serve edgecodex/frontend -l 3001
# Open http://localhost:3001
```

Both frontends are fully functional in the browser with simulated AI responses.

### Claude Code Preview Config
A `.claude/launch.json` was configured for instant preview server launching:
```json
{
  "servers": [
    {"name": "TextPlay",  "command": "npx serve textplay/frontend -l 3000",  "port": 3000},
    {"name": "EdgeCodex", "command": "npx serve edgecodex/frontend -l 3001", "port": 3001}
  ]
}
```

## 8. Gradle Configuration Reference

| Component | Version |
|-----------|---------|
| AGP | 8.8.2 |
| Kotlin | 2.2.0 |
| Compose BOM | 2026.02.00 |
| LiteRT-LM | 0.10.0 |
| Min SDK | 31 (Android 12) |
| Target SDK | 35 (Android 15) |
| Compile SDK | 35 |
