# Developer Memo

This memo captures the current assumptions and guardrails for ongoing implementation.

## Product Positioning

- This is a mobile-native coding assistant.
- It is inspired by Craft Agents, not a feature-for-feature port.
- Desktop parity is not a release goal.

## Hard Constraints

- Android-only for now.
- Offline-first.
- No Node, Bun, or Electron runtime inside the app.
- No shell execution.
- No local MCP subprocesses.

## Architectural Invariants

- The active workspace root is the security boundary.
- Model output is untrusted and must be validated before tool execution.
- Tool permissions are enforced below the UI layer.
- Session orchestration flows through `MobileAgentBackend`.
- LLM integration must remain behind `LlmEngine`.

## What Must Stay Simple

- Tool list for MVP
- session model
- prompt assembly
- permission model

If any feature pressures the design toward a desktop-like runtime, stop and reconsider.

## Known Shortcuts In Phase 1

- repositories are in-memory
- `GemmaLiteRtEngine` is still a stub
- `ToolCallParser` is still a stub
- Room schema is declared but not used
- Workspace screen and diff screen are placeholders

These are intentional scaffolding shortcuts, not final architecture.

## Phase 2 Progress Snapshot

The branch now includes an actual turn orchestration skeleton beyond the initial scaffold.

Implemented so far:

- explicit `TurnState`
- cancellation path in `SessionViewModel`
- prompt context assembly
- system prompt layering with workspace, permission mode, recent messages, and skills
- model warmup hook from home screen
- assistant message persistence on completed turns

Still placeholder:

- actual LiteRT-LM integration
- real tool-call parsing
- persistent repositories
- real workspace filesystem access
- permission approval UI

## Next Implementation Order

1. real workspace repository
2. path validation
3. read-only tools
4. real local model adapter
5. write permissions and patch preview
6. persistence migration from in-memory to Room

## Things To Avoid Early

- adding too many tools before read-only flows are solid
- mixing Room entities into domain models
- direct file I/O from ViewModels
- UI-owned permission enforcement
- prompt bloat from full-file injection
- hiding turn-state transitions inside opaque helper functions
- treating cancellation as a generic failure

## Decision Notes

- `craft-lite-mobile/` is intentionally separate from `gallery-integration/`
  because this project is a standalone mobile application, not just a Gallery task sample.
- Current package name uses `com.yourorg.craftlite` as a placeholder and must be replaced before distribution.
- The fake LLM stream is useful only for UI integration and should be removed once real inference is connected.
- `PromptContextAssembler` is the current boundary for prompt inputs. New prompt sources should be added there first unless there is a strong reason not to.
- `ConversationCoordinator` is now the main turn pipeline and should remain the single place where model output is converted into agent events.

## Immediate Follow-Up

- replace in-memory workspace listing with real sandbox-aware access
- add path validator utilities before file read/write expansion
- keep model integration behind `LlmEngine`
- defer Room migration until read-only workspace flow is stable

## Rename Before Shipping

- package name
- application id
- app label
- icon assets
- sample workspace paths
