# Tool Safety

## Rules

- all file access must stay inside the active workspace root
- write tools must be policy-gated
- binary and oversized files should be rejected or truncated
- no arbitrary command execution
- no path traversal beyond app-approved roots

## Permission Modes

- `safe`: read-only
- `ask`: confirm write actions
- `auto`: auto-approve sandboxed writes
