# Code Standards

This document defines the coding rules for Craft-lite Mobile.

## 1. Architectural Rules

- UI code must stay in `ui/` and `navigation/`.
- ViewModels orchestrate UI state only. They must not directly access raw filesystem APIs.
- Business logic belongs in `agent/`, `tools/`, `data/`, or `domain/`.
- `domain/` models must be Android-free whenever possible.
- Tool implementations must not reach into Compose or ViewModel classes.
- `LlmEngine` implementations must not depend on screen-level UI classes.
- Prompt construction must flow through `PromptContextAssembler -> PromptBuilder`.
- Conversation execution must flow through `MobileAgentBackend -> ConversationCoordinator`.

## 2. Kotlin Rules

- Use Kotlin data classes for immutable UI and domain state.
- Prefer constructor injection with Hilt.
- Prefer small focused classes over multi-purpose utility files.
- Avoid global mutable state.
- Use `sealed class` or `enum class` for closed state machines.
- Prefer explicit return types on public functions.
- Use `suspend` and `Flow` for async boundaries instead of callbacks.

## 3. Compose Rules

- UI state exposed from ViewModels must use `StateFlow`.
- Composables should be stateless where practical.
- Keep side effects in `LaunchedEffect` or `DisposableEffect`.
- Avoid putting business rules inside composables.
- Keep screen composables thin and delegate repeated pieces into reusable components.

## 4. Repository Rules

- Repositories define interfaces first.
- Start with in-memory or fake implementations only when they unblock architecture work.
- Replace fakes with persistent implementations before calling a phase complete.
- Repositories must enforce workspace boundaries if they expose filesystem-backed data.
- Session repositories must preserve message order and keep session history append-only unless an explicit compaction feature exists.

## 5. Tool Rules

- Every tool must have a narrow single responsibility.
- Every tool must validate inputs before execution.
- Every tool must return structured failure information, not just generic strings.
- Tools must be deterministic for the same input and workspace state when practical.
- Tools must not assume shell access or desktop paths.
- Tools that write must go through permission checks before mutation.
- Tool execution must be routed through `ToolRouter`, not called ad hoc from screens or ViewModels.

## 6. Permission Rules

- `safe` means read-only, no exceptions.
- `ask` means mutation requires explicit UI approval.
- `auto` means only sandboxed writes are approved automatically.
- Permission decisions must be enforced in the execution path, not only in the UI.
- Prompt text must reflect the current permission mode so the model is operating under the same constraint the runtime will enforce.

## 7. Filesystem Rules

- Always resolve canonical paths before reading or writing.
- Never trust relative paths from model output directly.
- Reject any file access that escapes the active workspace root.
- Add hard limits for file size, line count, and output size before returning content to the model.
- Detect binary files and refuse text tools on them unless a specific binary path is supported.

## 8. Prompt And LLM Rules

- Keep system prompts short and explicit.
- Prefer structured, tool-friendly instructions over verbose hidden reasoning requests.
- Do not embed full file contents when a summary or excerpt is enough.
- Make constraints explicit in prompt text:
  - no shell
  - workspace sandbox only
  - permission mode applies
- Treat model output as untrusted input until validated.
- Prompt context should be layered in this order unless there is a documented reason to change it:
  - workspace
  - permission mode
  - recent session context
  - active skills
  - relevant snippets
- Recent session context must be bounded. Do not inject unbounded conversation history into the model.

## 9. Error Handling Rules

- Do not swallow exceptions silently.
- Convert storage, tool, and LLM failures into explicit app-level error states.
- Error messages shown to users must be actionable.
- Logs may be more detailed than user-facing errors, but must not leak sensitive content unnecessarily.
- Cancellation is not an error. Treat user cancellation as a first-class turn outcome.

## 10. Naming Rules

- Class names should match their responsibility exactly.
- Use `*Repository`, `*ViewModel`, `*Tool`, `*Coordinator`, `*Manager` consistently.
- Avoid vague file names like `Utils.kt` unless the file is truly small and local in scope.
- Prefer `WorkspacePathValidator` over generic names like `PathHelper`.

## 11. Testing Rules

- Test pure reducers and validators first.
- Test path safety before expanding write capabilities.
- Add small deterministic fixtures in `sample-workspaces/`.
- Add regression tests for every bug in permission enforcement or workspace escaping.
- Add reducer tests for every new `TurnState`.
- Add cancellation tests whenever the turn lifecycle changes.

## 12. Documentation Rules

- Update roadmap docs when scope changes.
- Document new invariants the same day they are introduced.
- When a placeholder becomes real functionality, remove or rewrite the placeholder note.
- When a new orchestration step is introduced, document where it sits in the turn pipeline.
