# Craft Agents - Integration Guide

## What This Integration Is

This integration adds Craft Agents to AI Edge Gallery as a `CustomTask` that opens the Craft Agents Web UI inside an Android `WebView`.

It does **not** run the Craft Agents runtime on-device.

The Android app acts as a thin client:

- Android `CustomTask`
- `WebView`
- remote Craft Agents server with Web UI enabled

That matches the real architecture of `craft-agents-oss`, where the agent runtime lives in the server and the browser UI can connect remotely over WebSocket.

## Why This Approach

`craft-agents-oss` is primarily:

- an Electron desktop app
- a Bun/Node headless server
- a browser Web UI that connects to that server

It is not an Android project and cannot be turned into an APK directly without a portability layer.

The practical Android path is:

1. run Craft Agents server on a reachable machine
2. enable the bundled Web UI
3. load that UI inside a Gallery `CustomTask`

## Craft Server Prerequisites

From the `craft-agents-oss` repo:

```bash
bun install
bun run server:build:subprocess
bun run webui:build
CRAFT_SERVER_TOKEN=your-token \
CRAFT_WEBUI_DIR=apps/webui/dist \
bun run packages/server/src/index.ts
```

For a production-like launch:

```bash
CRAFT_SERVER_TOKEN=your-token \
CRAFT_WEBUI_DIR=apps/webui/dist \
bun run server:prod
```

You need an Android-reachable URL such as:

- `http://192.168.x.x:9100`
- `https://your-domain.example`

## Included Files

This repo adds a Gallery-ready scaffold under:

- `gallery-integration/customtasks/craftagents/CraftAgentsTask.kt`
- `gallery-integration/customtasks/craftagents/CraftAgentsTaskModule.kt`
- `gallery-integration/customtasks/craftagents/CraftAgentsScreen.kt`
- `gallery-integration/customtasks/craftagents/CraftAgentsViewModel.kt`

## Gallery Integration Steps

1. Copy `gallery-integration/customtasks/craftagents` into:
   `Android/src/app/src/main/java/com/google/ai/edge/gallery/customtasks/craftagents`
2. Ensure the package remains:
   `com.google.ai.edge.gallery.customtasks.craftagents`
3. Rebuild Gallery:

```bash
./gradlew installDebug
```

No `model_allowlist.json` patch is required for this task because it is not tied to an on-device LiteRT-LM model family.

## Current Limitations

- This is a remote-first client, not an offline task.
- The user must provide a reachable Craft Agents Web UI URL.
- Self-signed TLS may fail in Android `WebView` unless the host app explicitly trusts that certificate.
- Native Android integration with Craft tools is not implemented; the remote web app handles the session UX.

## Recommended Next Step

If you want this to feel like a first-class plugin, the next iteration should add:

- persisted server profiles
- optional token/bootstrap flow
- connection health checks
- per-server labels/icons
- a dedicated launch screen instead of a plain URL form
