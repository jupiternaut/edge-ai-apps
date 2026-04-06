# EdgeAI Apps - On-Device AI Application Suite

Two mobile AI applications powered by Google Gemma models, running **100% offline** on-device via LiteRT-LM.

## Products

### TextPlay - AI-Driven Text Adventure Game
Natural language sandbox game where players control a character through text commands. Powered by FunctionGemma 270M for action parsing + Gemma 4 for NPC dialogue.

- 16x12 tile-based game world with village, lake, cave, NPCs
- 8 game actions: move, look, pickup, use, talk, craft, interact, inventory
- 4-quest storyline with crafting system
- WebView frontend (HTML5 Canvas) + Kotlin backend

### EdgeCodex - On-Device Code Assistant
Mobile IDE with AI code analysis, powered by Gemma 4 E2B. Zero cloud dependency.

- Code editor with line numbers, syntax themes
- 6 quick actions: Explain, Fix Bug, Refactor, Complete, Gen Tests, Optimize
- Prompt Lab for model parameter tuning (temperature, top-k, top-p)
- 8 languages: Python, JavaScript, Kotlin, Java, Rust, Go, C++, TypeScript

## Architecture

```
User Input (voice/text)
        │
        ▼
┌──────────────────────┐
│   FunctionGemma 270M │ ← TextPlay: structured actions
│   or Gemma 4 E2B     │ ← EdgeCodex: free-form analysis
│   (LiteRT-LM Engine) │
└──────────┬───────────┘
           │ @Tool calls / streaming text
           ▼
┌──────────────────────┐
│ Kotlin ViewModel     │ → WebViewCommand / response text
└──────────┬───────────┘
           │ evaluateJavascript()
           ▼
┌──────────────────────┐
│ WebView (HTML5/JS)   │ ← Game canvas / Code editor
└──────────────────────┘
```

## Quick Start (Browser Preview)

```bash
npx serve textplay/frontend -l 3000   # Game: http://localhost:3000
npx serve edgecodex/frontend -l 3001  # IDE:  http://localhost:3001
```

## Android Build

See [REQUIREMENTS.md](REQUIREMENTS.md) for full setup instructions.

## Project Structure

```
edge-ai-apps/
├── textplay/                    # TextPlay game prototype
│   ├── frontend/                # HTML5 game (browser-runnable)
│   │   ├── index.html
│   │   ├── style.css
│   │   └── game.js
│   ├── android/                 # Kotlin integration (standalone pkg)
│   └── INTEGRATION_GUIDE.md
├── edgecodex/                   # EdgeCodex IDE prototype
│   ├── frontend/                # Web IDE (browser-runnable)
│   │   ├── index.html
│   │   ├── style.css
│   │   └── app.js
│   └── android/                 # Kotlin integration (standalone pkg)
├── gallery-integration/         # Pre-adapted files for AI Edge Gallery
│   ├── customtasks/textplay/    # Gallery-namespaced Kotlin
│   ├── customtasks/edgecodex/
│   └── model_allowlist_*.json   # Patched allowlist with new task types
├── REQUIREMENTS.md              # Build environment & setup guide
├── INTEGRATION_GUIDE.md         # Step-by-step Gallery integration
└── README.md
```

## Tech Stack

- **Models**: FunctionGemma 270M, Gemma 4 E2B/E4B (Apache 2.0)
- **Runtime**: LiteRT-LM 0.10.0
- **Android**: Kotlin + Jetpack Compose + Hilt + WebView
- **Frontend**: Vanilla HTML5/CSS/JS (no build step)
- **Base**: [google-ai-edge/gallery](https://github.com/google-ai-edge/gallery)
