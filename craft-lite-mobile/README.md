# Craft-lite Mobile

Craft-lite Mobile is an Android-first, offline-capable coding assistant inspired by Craft Agents.

This project is intentionally **not** a port of the Electron desktop app. It is a new mobile architecture optimized for:

- local Android execution
- local model inference
- sandboxed workspace access
- safe, structured tool execution

## Phase 1 Status

Phase 1 establishes the project scaffold:

- Android Gradle project skeleton
- Compose app shell
- navigation and placeholder screens
- dependency injection entry points
- core interfaces for agent, LLM, tools, storage, and permissions
- architecture and safety docs

## Planned Phases

- Phase 1: foundation and architecture scaffold
- Phase 2: local LLM integration and session orchestration
- Phase 3: workspace browsing and read-only tools
- Phase 4: write tools, patching, and permissions
- Phase 5: persistence, skills, and product polish
- Phase 6: stabilization, evaluation, and release hardening

## Repository Layout

- `docs/` architecture and planning docs
- `android/` Android Studio project
- `sample-workspaces/` small test repositories for future manual QA

## Key Docs

- `docs/phase-roadmap.md`
- `docs/code-standards.md`
- `docs/dev-memo.md`
