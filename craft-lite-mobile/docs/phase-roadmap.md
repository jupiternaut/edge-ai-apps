# Phase Roadmap

This document continues the implementation plan after Phase 1.

## Current Status

Phase 1 is complete as a scaffold.

Completed outcomes:

- Android project shell exists
- Compose navigation shell exists
- placeholder screens exist
- in-memory repositories exist
- core agent, tool, and LLM interfaces exist
- basic streaming stub exists through `SessionViewModel -> MobileAgentBackend -> ConversationCoordinator -> LlmEngine`

## Phase 2: Local LLM Integration And Session Orchestration

### Goal

Replace the current placeholder LLM stream with a real local inference adapter and establish a stable turn lifecycle.

### Work Items

- Replace `GemmaLiteRtEngine` placeholder output with a real LiteRT-LM-backed implementation.
- Add model preparation and release hooks to app lifecycle.
- Introduce explicit session turn states:
  - idle
  - preparing
  - streaming
  - running_tool
  - awaiting_permission
  - complete
  - failed
- Persist draft message and last active session in memory first, storage later.
- Add cancellation hooks for in-flight turns.
- Extend `ConversationCoordinator` so model output can emit:
  - plain token stream
  - structured tool call
  - final assistant text
- Add prompt sections for:
  - system prompt
  - workspace summary
  - relevant file snippets
  - prior session context

### Acceptance Criteria

- The app can load one real local model on a supported device.
- A user can send a message and receive streamed output.
- A failed inference does not crash the app or leave the session stuck.
- Session screen reflects turn state transitions correctly.

### Deliverables

- real `LlmEngine` implementation
- `SessionUiState` turn-state model
- cancellation path
- prompt assembly v1

## Phase 3: Workspace Browsing And Read-Only Tools

### Goal

Move from placeholder repositories to real sandboxed workspace access and make read-only tools useful.

### Work Items

- Replace `InMemoryWorkspaceRepository` with a real filesystem-backed repository.
- Define workspace root selection and normalization rules.
- Add path validation utilities:
  - canonical path resolution
  - workspace-root enforcement
  - max file size guard
  - text/binary detection
- Implement real tool behavior for:
  - `list_files`
  - `read_file`
  - `search_text`
- Add workspace screen sections:
  - recent workspaces
  - file tree
  - recent files
- Add a small search index or bounded recursive text scan fallback.

### Acceptance Criteria

- A user can open a workspace and browse files.
- The model can list files and read specific files using tools.
- Text search returns stable results on a medium-sized sample workspace.
- No file outside the selected workspace root can be read.

### Deliverables

- real workspace repository
- path safety utility package
- read-only tools v1
- workspace UI connected to real data

## Phase 4: Write Tools, Patching, And Permissions

### Goal

Introduce safe editing primitives without introducing shell execution.

### Work Items

- Implement real `write_file`.
- Implement patch request and patch apply flow.
- Add `PatchPreview` generation and a preview screen.
- Introduce permission policy checks for write operations.
- Add `safe`, `ask`, and `auto` enforcement in the tool execution path.
- Show permission prompts in session UI.
- Ensure failed writes are reported with enough context for retry.

### Acceptance Criteria

- `safe` mode blocks all writes.
- `ask` mode prompts before writes and applies only after approval.
- `auto` mode allows sandboxed writes without prompt.
- The user can preview a patch before final apply.
- Write operations cannot escape the active workspace.

### Deliverables

- write tool implementation
- patch preview model and UI
- permission flow v1
- write-safe path guardrail package

## Phase 5: Persistence, Skills, And Product Polish

### Goal

Replace transient state with durable storage and add the first reusable skill system.

### Work Items

- Replace in-memory repositories with Room-backed repositories where appropriate.
- Persist:
  - sessions
  - messages
  - tool events
  - settings
  - active workspace
- Add skill discovery from workspace-local markdown files.
- Add settings UI for:
  - permission mode
  - selected model
  - experiments
- Add session list improvements:
  - rename
  - archive
  - last updated sort
- Add crash-safe loading and recovery for partial turns.

### Acceptance Criteria

- Sessions survive app restart.
- Settings survive app restart.
- Skills can be loaded from disk and injected into prompts.
- Session list and workspace state restore correctly after process death.

### Deliverables

- Room schema v1
- settings persistence
- skills v1
- session recovery path

## Phase 6: Stabilization, Evaluation, And Release Hardening

### Goal

Reduce failure cases and make behavior measurable.

### Work Items

- Add test workspaces and scripted QA scenarios.
- Measure tool latency and model latency.
- Add bounded context compaction or summarization policy.
- Add large-file and large-repo behavior limits.
- Add structured error classes across agent/tool/storage layers.
- Add instrumentation logs behind a debug flag.
- Validate memory behavior on representative devices.

### Acceptance Criteria

- The app completes a standard QA scenario on sample repos without manual recovery.
- Error states are understandable and actionable.
- Large-file and low-memory behavior degrade safely.
- Release build is reproducible with documented prerequisites.

### Deliverables

- QA checklist
- performance notes
- error taxonomy
- release checklist

## Cross-Phase Rules

- No arbitrary command execution.
- No path access outside active workspace.
- Prefer structured tool calls over free-form edits.
- Keep mobile-first constraints ahead of desktop parity.
