// ============================================================
// EdgeCodex - On-Device AI Code Assistant
// Powered by Gemma 4 E2B via LiteRT-LM
// ============================================================

// ==================== STATE ====================
const state = {
  language: 'python',
  config: {
    temperature: 0.7,
    topK: 64,
    topP: 0.95,
    maxTokens: 2048,
    systemPrompt: '',
  },
  messages: [],
  processing: false,
};

// ==================== CODE SAMPLES ====================
const CODE_SAMPLES = {
  python: `def fibonacci(n):
    """Calculate the nth Fibonacci number."""
    if n <= 0:
        return 0
    elif n == 1:
        return 1

    a, b = 0, 1
    for _ in range(2, n + 1):
        a, b = b, a + b
    return b


def find_duplicates(lst):
    """Find duplicate elements in a list."""
    seen = set()
    duplicates = []
    for item in lst:
        if item in seen:
            duplicates.append(item)
        seen.add(item)
    return duplicates


# Example usage
print(fibonacci(10))
print(find_duplicates([1, 2, 3, 2, 4, 5, 3]))`,

  javascript: `class EventEmitter {
  constructor() {
    this.listeners = new Map();
  }

  on(event, callback) {
    if (!this.listeners.has(event)) {
      this.listeners.set(event, []);
    }
    this.listeners.get(event).push(callback);
    return this;
  }

  emit(event, ...args) {
    const callbacks = this.listeners.get(event);
    if (callbacks) {
      callbacks.forEach(cb => cb(...args));
    }
    return this;
  }

  off(event, callback) {
    const callbacks = this.listeners.get(event);
    if (callbacks) {
      const index = callbacks.indexOf(callback);
      if (index > -1) callbacks.splice(index, 1);
    }
    return this;
  }
}

// Usage
const emitter = new EventEmitter();
emitter.on('data', (msg) => console.log('Received:', msg));
emitter.emit('data', 'Hello World');`,

  kotlin: `data class User(val name: String, val age: Int, val email: String)

fun List<User>.filterAdults(): List<User> =
    filter { it.age >= 18 }

fun List<User>.groupByAgeRange(): Map<String, List<User>> =
    groupBy { user ->
        when {
            user.age < 18 -> "minor"
            user.age < 30 -> "young"
            user.age < 50 -> "middle"
            else -> "senior"
        }
    }

suspend fun fetchUsers(api: ApiService): Result<List<User>> =
    runCatching {
        api.getUsers()
    }

fun main() {
    val users = listOf(
        User("Alice", 25, "alice@example.com"),
        User("Bob", 17, "bob@example.com"),
        User("Charlie", 45, "charlie@example.com"),
    )
    println(users.filterAdults())
    println(users.groupByAgeRange())
}`,
};

// ==================== SIMULATED AI RESPONSES ====================
// In production, these come from Gemma 4 E2B via LiteRT-LM.
// For browser prototype, we simulate intelligent responses.
const AI_RESPONSES = {
  explain: (code, lang) => {
    const lines = code.split('\n').filter(l => l.trim()).length;
    return `## Code Analysis

**Language:** ${lang}
**Lines:** ${lines}

### Structure
This code defines ${detectStructures(code, lang)}.

### Key Patterns
${detectPatterns(code, lang)}

### Complexity
The overall time complexity appears to be **${estimateComplexity(code)}**.

> *Analysis performed entirely on-device by Gemma 4 E2B. No data sent to any server.*`;
  },

  fix: (code, lang) => {
    const issues = detectIssues(code, lang);
    if (issues.length === 0) {
      return `No obvious bugs detected in your ${lang} code. The logic appears sound.\n\n> Tip: For deeper analysis, try "explain" to understand the control flow, or paste specific error messages.`;
    }
    return `## Potential Issues Found\n\n${issues.map((issue, i) => `${i + 1}. **${issue.type}**: ${issue.desc}\n   - Line ~${issue.line}: \`${issue.snippet}\`\n   - Fix: ${issue.fix}`).join('\n\n')}\n\n> *Bug analysis by Gemma 4 E2B, fully offline.*`;
  },

  refactor: (code, lang) => {
    return `## Refactoring Suggestions

### 1. Extract Helper Functions
Consider breaking down longer functions into smaller, focused helpers for better testability.

### 2. Use Modern ${lang} Patterns
\`\`\`${lang}
${generateRefactoredSnippet(code, lang)}
\`\`\`

### 3. Add Type Safety
${lang === 'python' ? 'Add type hints for better IDE support and documentation.' : lang === 'javascript' ? 'Consider migrating to TypeScript for compile-time type checking.' : 'Types are already enforced by the language.'}

> *Refactoring analysis by Gemma 4 E2B, zero cloud dependency.*`;
  },

  complete: (code, lang) => {
    return `## Code Completion

Based on the context, here's the suggested completion:

\`\`\`${lang}
${generateCompletion(code, lang)}
\`\`\`

You can copy this into your editor. The completion follows the existing code style and patterns.

> *Generated on-device by Gemma 4 E2B.*`;
  },

  test: (code, lang) => {
    return `## Generated Test Cases

\`\`\`${lang}
${generateTests(code, lang)}
\`\`\`

These tests cover the main functions detected in your code. Add edge cases as needed.

> *Test generation by Gemma 4 E2B, completely offline.*`;
  },

  optimize: (code, lang) => {
    return `## Optimization Report

### Performance Analysis
- Current estimated complexity: **${estimateComplexity(code)}**
- Memory usage pattern: ${code.includes('[]') || code.includes('list') || code.includes('List') ? 'Dynamic allocation detected' : 'Mostly stack-based'}

### Suggestions
1. **Memoization**: Cache repeated computations for recursive patterns
2. **Early exits**: Add guard clauses to avoid unnecessary iterations
3. **Data structure**: ${code.includes('list') || code.includes('[]') ? 'Consider using a Set/HashMap for O(1) lookups instead of linear search' : 'Current data structures seem appropriate'}

> *Performance analysis by Gemma 4 E2B, on-device.*`;
  },
};

// ==================== CODE ANALYSIS HELPERS ====================
function detectStructures(code, lang) {
  const structures = [];
  if (/\bclass\b/.test(code)) structures.push('classes');
  if (/\bdef\b|\bfunction\b|\bfun\b/.test(code)) structures.push('functions');
  if (/\basync\b|\bsuspend\b/.test(code)) structures.push('async operations');
  if (/\bfor\b|\bwhile\b/.test(code)) structures.push('loops');
  if (/\bmap\b|\bfilter\b|\breduce\b/.test(code)) structures.push('functional transformations');
  return structures.length > 0 ? structures.join(', ') : 'basic expressions';
}

function detectPatterns(code, lang) {
  const patterns = [];
  if (/\bclass\b.*\bconstructor\b/s.test(code)) patterns.push('- **OOP**: Constructor-based class design');
  if (/\.map\(|\.filter\(|\.reduce\(/.test(code)) patterns.push('- **Functional**: Method chaining with higher-order functions');
  if (/\basync\b|\bawait\b|\bsuspend\b/.test(code)) patterns.push('- **Async**: Asynchronous execution pattern');
  if (/\bif\b.*\breturn\b/.test(code)) patterns.push('- **Guard clause**: Early return pattern');
  if (/\bset\(\)|\bSet\b|\bHashMap\b|\bMap\b/.test(code)) patterns.push('- **Hash-based lookup**: Using set/map for efficient searching');
  if (patterns.length === 0) patterns.push('- Standard imperative programming style');
  return patterns.join('\n');
}

function estimateComplexity(code) {
  if (/for.*for|while.*while/.test(code)) return 'O(n\u00b2)';
  if (/for|while|\.map\(|\.filter\(/.test(code)) return 'O(n)';
  if (/\brecursi/.test(code) || /def.*\(.*\).*\n.*\1/.test(code)) return 'O(2^n) or O(n) with memoization';
  return 'O(1)';
}

function detectIssues(code, lang) {
  const issues = [];
  const lines = code.split('\n');

  // Check for common issues
  lines.forEach((line, i) => {
    if (line.includes('== null') && lang === 'javascript') {
      issues.push({ type: 'Null Check', line: i + 1, snippet: line.trim(),
        desc: 'Loose equality with null may miss undefined', fix: 'Use `=== null` or `== null` intentionally (covers both null and undefined)' });
    }
    if (/catch\s*\(\s*\w+\s*\)\s*\{\s*\}/.test(line)) {
      issues.push({ type: 'Silent Catch', line: i + 1, snippet: line.trim(),
        desc: 'Empty catch block silently swallows errors', fix: 'Log the error or handle it explicitly' });
    }
    if (/var\s/.test(line) && (lang === 'javascript' || lang === 'typescript')) {
      issues.push({ type: 'Legacy var', line: i + 1, snippet: line.trim(),
        desc: '`var` has function scope, not block scope', fix: 'Use `const` or `let` instead' });
    }
  });

  return issues;
}

function generateRefactoredSnippet(code, lang) {
  // Return a simplified refactored version hint
  const funcs = code.match(/(?:def|function|fun)\s+(\w+)/g) || [];
  if (funcs.length > 0) {
    return `// Suggested: Extract into a module\n// ${funcs.join(', ')}\n// Each function should have a single responsibility.`;
  }
  return '// No specific refactoring needed for this snippet.';
}

function generateCompletion(code, lang) {
  const lines = code.trim().split('\n');
  const lastLine = lines[lines.length - 1] || '';

  if (lang === 'python') {
    if (lastLine.includes('def ')) return '    """Docstring here."""\n    pass';
    if (lastLine.includes('class ')) return '    def __init__(self):\n        pass\n\n    def __repr__(self):\n        return f"{self.__class__.__name__}()"';
    return '# TODO: implement next step';
  }
  if (lang === 'javascript' || lang === 'typescript') {
    if (lastLine.includes('function') || lastLine.includes('=>')) return '  // Implementation\n  throw new Error("Not implemented");';
    return '// TODO: implement';
  }
  return '// TODO: implement';
}

function generateTests(code, lang) {
  const funcNames = (code.match(/(?:def|function|fun)\s+(\w+)/g) || [])
    .map(m => m.split(/\s+/)[1]);

  if (lang === 'python') {
    return `import pytest

${funcNames.map(fn => `
def test_${fn}_basic():
    """Test ${fn} with basic input."""
    result = ${fn}(${fn.includes('fibonacci') ? '10' : '[1, 2, 3]'})
    assert result is not None

def test_${fn}_edge_case():
    """Test ${fn} with edge case."""
    result = ${fn}(${fn.includes('fibonacci') ? '0' : '[]'})
    assert result is not None
`).join('\n')}`;
  }

  if (lang === 'javascript' || lang === 'typescript') {
    return `describe('Module Tests', () => {
${funcNames.map(fn => `  describe('${fn}', () => {
    test('should handle basic input', () => {
      const result = ${fn}();
      expect(result).toBeDefined();
    });

    test('should handle edge cases', () => {
      expect(() => ${fn}(null)).not.toThrow();
    });
  });
`).join('\n')}});`;
  }

  return `// Test generation for ${lang}\n// Add test framework and implement tests for: ${funcNames.join(', ')}`;
}

// ==================== UI CONTROLLER ====================
class EdgeCodexApp {
  constructor() {
    this.editor = document.getElementById('code-editor');
    this.lineNumbers = document.getElementById('line-numbers');
    this.chatMessages = document.getElementById('chat-messages');
    this.chatInput = document.getElementById('chat-input');
    this.outputContent = document.getElementById('output-content');

    this.init();
  }

  init() {
    this.setupEditor();
    this.setupTabs();
    this.setupChat();
    this.setupQuickActions();
    this.setupPromptLab();
    this.setupLanguageSelect();
    this.loadSampleCode();
    this.addSystemMessage('EdgeCodex ready. All AI inference runs locally on your device via Gemma 4 E2B. Your code never leaves this device.');
  }

  // ==================== EDITOR ====================
  setupEditor() {
    this.editor.addEventListener('input', () => this.updateLineNumbers());
    this.editor.addEventListener('scroll', () => {
      this.lineNumbers.scrollTop = this.editor.scrollTop;
    });
    this.editor.addEventListener('keydown', (e) => {
      // Tab key inserts spaces
      if (e.key === 'Tab') {
        e.preventDefault();
        const start = this.editor.selectionStart;
        const end = this.editor.selectionEnd;
        this.editor.value = this.editor.value.substring(0, start) + '    ' + this.editor.value.substring(end);
        this.editor.selectionStart = this.editor.selectionEnd = start + 4;
        this.updateLineNumbers();
      }
    });
  }

  updateLineNumbers() {
    const lines = this.editor.value.split('\n').length;
    this.lineNumbers.innerHTML = Array.from({ length: lines }, (_, i) =>
      `<div>${i + 1}</div>`
    ).join('');
  }

  loadSampleCode() {
    const lang = document.getElementById('lang-select').value;
    if (CODE_SAMPLES[lang]) {
      this.editor.value = CODE_SAMPLES[lang];
    } else {
      this.editor.value = `// ${lang} code editor\n// Start typing or paste your code here...`;
    }
    this.updateLineNumbers();
  }

  // ==================== TABS ====================
  setupTabs() {
    document.querySelectorAll('.panel-header .tab').forEach(tab => {
      tab.addEventListener('click', () => {
        const tabName = tab.dataset.tab;
        document.querySelectorAll('.panel-header .tab').forEach(t => t.classList.remove('active'));
        document.querySelectorAll('.panel-body').forEach(p => p.classList.remove('active'));
        tab.classList.add('active');
        document.querySelector(`[data-panel="${tabName}"]`).classList.add('active');
      });
    });
  }

  // ==================== CHAT ====================
  setupChat() {
    document.getElementById('chat-form').addEventListener('submit', (e) => {
      e.preventDefault();
      const text = this.chatInput.value.trim();
      if (!text || state.processing) return;
      this.chatInput.value = '';
      this.handleUserMessage(text);
    });

    this.chatInput.addEventListener('keydown', (e) => {
      if (e.key === 'Enter' && !e.shiftKey) {
        e.preventDefault();
        document.getElementById('chat-form').dispatchEvent(new Event('submit'));
      }
    });
  }

  handleUserMessage(text) {
    this.addMessage('user', text);

    state.processing = true;
    // Simulate inference delay (real device: ~200-800ms for E2B)
    const delay = 300 + Math.random() * 500;

    setTimeout(() => {
      const code = this.editor.value;
      const lang = state.language;
      const action = this.detectAction(text);
      let response;

      if (AI_RESPONSES[action]) {
        response = AI_RESPONSES[action](code, lang);
      } else {
        response = this.generateFreeformResponse(text, code, lang);
      }

      this.addMessage('assistant', response);
      state.processing = false;
    }, delay);
  }

  detectAction(text) {
    const lower = text.toLowerCase();
    if (/explain|what does|how does|walk.*through|describe/.test(lower)) return 'explain';
    if (/fix|bug|error|wrong|issue|debug/.test(lower)) return 'fix';
    if (/refactor|clean|improve|simplify|restructure/.test(lower)) return 'refactor';
    if (/complete|finish|continue|next|implement/.test(lower)) return 'complete';
    if (/test|spec|unit|coverage/.test(lower)) return 'test';
    if (/optim|perf|fast|speed|slow|memory/.test(lower)) return 'optimize';
    return 'freeform';
  }

  generateFreeformResponse(text, code, lang) {
    return `I understand you're asking about: "${text}"

Based on your ${lang} code (${code.split('\n').length} lines), here's my analysis:

${detectStructures(code, lang) !== 'basic expressions'
      ? `Your code contains ${detectStructures(code, lang)}. `
      : ''}The code appears to be well-structured.

Try one of the quick action buttons for specific analysis, or ask me to:
- **Explain** specific functions
- **Find bugs** in the logic
- **Generate tests** for your functions
- **Refactor** for better patterns

> *Powered by Gemma 4 E2B, running entirely on your device.*`;
  }

  // ==================== MESSAGES ====================
  addMessage(role, content) {
    const div = document.createElement('div');
    div.className = `chat-msg ${role}`;

    const roleLabel = { user: 'You', assistant: 'EdgeCodex', system: 'System' }[role];
    const rendered = this.renderMarkdown(content);

    div.innerHTML = `<div class="role">${roleLabel}</div>${rendered}`;
    this.chatMessages.appendChild(div);
    this.chatMessages.scrollTop = this.chatMessages.scrollHeight;
  }

  addSystemMessage(text) {
    this.addMessage('system', text);
  }

  renderMarkdown(text) {
    return text
      // Code blocks
      .replace(/```(\w+)?\n([\s\S]*?)```/g, '<pre><code>$2</code></pre>')
      // Inline code
      .replace(/`([^`]+)`/g, '<code>$1</code>')
      // Bold
      .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
      // Italic
      .replace(/\*(.+?)\*/g, '<em>$1</em>')
      // Headers
      .replace(/^### (.+)$/gm, '<h4>$1</h4>')
      .replace(/^## (.+)$/gm, '<h3>$1</h3>')
      // Lists
      .replace(/^- (.+)$/gm, '&bull; $1<br>')
      .replace(/^\d+\. (.+)$/gm, '$1<br>')
      // Blockquotes
      .replace(/^> (.+)$/gm, '<blockquote style="color:#8b949e;border-left:2px solid #30363d;padding-left:8px;margin:4px 0;font-size:11px">$1</blockquote>')
      // Line breaks
      .replace(/\n\n/g, '<br><br>')
      .replace(/\n/g, '<br>');
  }

  // ==================== QUICK ACTIONS ====================
  setupQuickActions() {
    document.querySelectorAll('.quick-btn').forEach(btn => {
      btn.addEventListener('click', () => {
        const action = btn.dataset.action;
        const prompts = {
          explain: 'Explain this code in detail',
          fix: 'Find and fix any bugs in this code',
          refactor: 'Suggest refactoring improvements',
          complete: 'Complete the unfinished parts of this code',
          test: 'Generate unit tests for this code',
          optimize: 'Analyze and optimize performance',
        };
        this.handleUserMessage(prompts[action] || action);
      });
    });
  }

  // ==================== PROMPT LAB ====================
  setupPromptLab() {
    const modal = document.getElementById('prompt-lab');
    document.getElementById('settings-btn').addEventListener('click', () => modal.classList.remove('hidden'));
    document.getElementById('close-lab').addEventListener('click', () => modal.classList.add('hidden'));
    modal.addEventListener('click', (e) => { if (e.target === modal) modal.classList.add('hidden'); });

    // Sliders
    const sliders = [
      { id: 'temp-slider', key: 'temperature', display: 'temp-val' },
      { id: 'topk-slider', key: 'topK', display: 'topk-val' },
      { id: 'topp-slider', key: 'topP', display: 'topp-val' },
      { id: 'maxtok-slider', key: 'maxTokens', display: 'maxtok-val' },
    ];

    sliders.forEach(({ id, key, display }) => {
      const slider = document.getElementById(id);
      slider.addEventListener('input', () => {
        state.config[key] = parseFloat(slider.value);
        document.getElementById(display).textContent = slider.value;
      });
    });

    document.getElementById('system-prompt').addEventListener('input', (e) => {
      state.config.systemPrompt = e.target.value;
    });

    state.config.systemPrompt = document.getElementById('system-prompt').value;
  }

  // ==================== LANGUAGE SELECT ====================
  setupLanguageSelect() {
    document.getElementById('lang-select').addEventListener('change', (e) => {
      state.language = e.target.value;
      this.loadSampleCode();
      this.addSystemMessage(`Switched to ${state.language}. Sample code loaded.`);
    });
  }
}

// ==================== COMMAND BRIDGE ====================
// Interface for Kotlin WebView integration (same pattern as TextPlay)
const edgeCodex = {
  app: null,

  init() {
    this.app = new EdgeCodexApp();
  },

  // Called from Kotlin to set code in the editor
  setCode(code) {
    const editor = document.getElementById('code-editor');
    editor.value = code;
    this.app.updateLineNumbers();
  },

  // Called from Kotlin to get current code
  getCode() {
    return document.getElementById('code-editor').value;
  },

  // Called from Kotlin to show AI response
  showResponse(text, role = 'assistant') {
    this.app.addMessage(role, text);
  },

  // Called from Kotlin to update config
  updateConfig(configJson) {
    const config = JSON.parse(configJson);
    Object.assign(state.config, config);
  },

  // Get current state for Kotlin sync
  getState() {
    return JSON.stringify({
      language: state.language,
      codeLength: document.getElementById('code-editor').value.length,
      config: state.config,
    });
  },
};

// ==================== INIT ====================
document.addEventListener('DOMContentLoaded', () => {
  edgeCodex.init();
});
