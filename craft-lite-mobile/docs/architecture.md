# Architecture

## Core Idea

Craft-lite Mobile is a native Android application with a local-first agent runtime.

The runtime is split into six layers:

1. Compose UI
2. ViewModels
3. Conversation orchestration
4. Local LLM engine adapter
5. Structured tool router
6. Sandboxed storage and workspace access

## Module Responsibilities

- `ui/`
  User-facing screens and reusable Compose components.
- `navigation/`
  App routes and top-level navigation shell.
- `agent/`
  Conversation coordination, prompt building, event reduction, and tool routing.
- `llm/`
  Local model lifecycle and inference adapters.
- `data/`
  Persistence, repositories, and storage abstractions.
- `tools/`
  Sandboxed file and workspace tools.
- `domain/`
  Shared business models and policies.

## Non-Goals

- no Electron runtime
- no Node or Bun server
- no shell execution
- no MCP subprocess support
- no direct parity with desktop Craft runtime
