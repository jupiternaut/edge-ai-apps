# TextPlay - Integration Guide

## Architecture Overview

```
User Input (voice/text)
    │
    ▼
┌─────────────────────┐
│  FunctionGemma 270M  │  ← On-device, offline
│  (LiteRT-LM Engine)  │
└────────┬────────────┘
         │  @Tool function calls
         ▼
┌─────────────────────┐
│  TextPlayTools.kt    │  → Emits TextPlayAction
│  TextPlayViewModel   │  → Converts to WebViewCommand
└────────┬────────────┘
         │  evaluateJavascript()
         ▼
┌─────────────────────┐
│  WebView (game.js)   │  ← HTML5 Canvas game
│  textPlay.runCommands│
└─────────────────────┘
```

## Prerequisites

1. **Android Studio** (Ladybug+)
2. **JDK 17**
3. **Android SDK 35** with min SDK 31
4. **A device with 8GB+ RAM** (for Gemma 4 E2B / FunctionGemma)

## Step 1: Clone AI Edge Gallery

```bash
git clone https://github.com/google-ai-edge/gallery.git
cd gallery/Android/src
```

## Step 2: Copy TextPlay Files

```bash
# Copy Kotlin source files
cp -r /path/to/textplay/android/src/main/java/com/edgeai/textplay \
  app/src/main/java/com/google/ai/edge/gallery/customtasks/textplay

# Copy frontend assets
cp -r /path/to/textplay/frontend/* \
  app/src/main/assets/textplay/
```

## Step 3: Update Package References

In the copied Kotlin files, change:
```kotlin
// From:
package com.edgeai.textplay
// To:
package com.google.ai.edge.gallery.customtasks.textplay
```

## Step 4: Register Task Type in Model Allowlist

Add `"llm_textplay"` to the `taskTypes` array for FunctionGemma in `model_allowlist.json`:

```json
{
  "name": "FunctionGemma-270m-ft-textplay",
  "taskTypes": [
    "llm_textplay"
  ]
}
```

## Step 5: Build and Install

```bash
./gradlew installDebug
```

## Step 6: Download Models

In the app:
1. Sign in to HuggingFace
2. Download **FunctionGemma 270M** (for game actions)
3. Optionally download **Gemma 4 E2B** (for fallback dialogue)

## Browser Testing (No Android Required)

Open `frontend/index.html` directly in a browser to test the game.
The built-in command parser simulates FunctionGemma's function calling.

Supported test commands:
- `walk north` / `go east 3` / arrow keys
- `look around`
- `pick up berries` / `open chest`
- `talk to farmer` / `chat with elder`
- `use key on gate`
- `craft berry with campfire`
- `draw water` / `read sign`
- `inventory`

## FunctionGemma Fine-tuning (Optional)

To create a TextPlay-specific model:

1. Export training data from game sessions
2. Use the Tiny Garden fine-tuning approach as reference:
   - Base: `google/functiongemma-270m-it`
   - Format: input/output pairs mapping natural language to function calls
3. Upload to HuggingFace and add to model allowlist
