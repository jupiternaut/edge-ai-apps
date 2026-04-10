# Gallery CustomTask 开发范式

> 本文档是新建 Gallery CustomTask 时的 checklist，沉淀自 TextPlay + EdgeCodex 两个实现以及 Google 官方 TinyGarden 参考。
> 每条范式都附有具体文件引用。新建 CustomTask 前请先通读此文件。

---

## 范式 1 — @Tool 函数契约

**核心规则**：
- 返回类型必须是 `Map<String, Any>`（**不是** `Map<String, String>`）
- 返回值必须包含 `"result"` 字段，用于确认执行状态
- 每个参数用 `@ToolParam(description = "自然语言描述")` 注解
- 方法体通过 callback 发出结构化事件，**不要**在返回值里塞复杂对象
- `import com.google.ai.edge.litertlm.Tool`（不是 `.tools.Tool`）

**正确示例** — `customtasks/textplay/TextPlayTools.kt:21-27`：
```kotlin
@Tool(description = "Move the player character in a direction. Valid directions: north, south, east, west.")
fun movePlayer(
    @ToolParam(description = "Direction to move: north, south, east, or west") direction: String,
    @ToolParam(description = "Number of steps to move (default 1, max 5)") steps: Int = 1,
): Map<String, Any> {
    Log.d(TAG, "movePlayer called: direction=$direction, steps=$steps")
    onAction(TextPlayAction.Move(direction, steps.coerceIn(1, 5)))
    return mapOf("result" to "success")
}
```

**对标源码**：Google 官方 `TinyGardenTools.kt`

---

## 范式 2 — Sealed Class 事件流

**核心规则**：
- 所有工具输出定义为 `sealed class` 层级
- ViewModel 消费时做穷尽匹配（`when` 语句覆盖所有 case，编译期即发现遗漏）
- 每种事件是独立的 `data class` 或 `data object`

**正确示例** — `customtasks/textplay/TextPlayTools.kt:88-97`：
```kotlin
sealed class TextPlayAction {
    data class Move(val direction: String, val steps: Int = 1) : TextPlayAction()
    data object Look : TextPlayAction()
    data class Pickup(val item: String) : TextPlayAction()
    data class UseItem(val item: String, val target: String) : TextPlayAction()
    // ...
}
```

**好处**：新增工具方法时，如果忘记在 ViewModel 的 `when` 里处理，编译会失败——零运行时错误。

---

## 范式 3 — N 轮对话重置

**核心规则**：
- 维护 `turnCount` 计数器，每次 sendMessage 后 +1
- 达到 `RESET_INTERVAL` 时调用 `resetConversation()`
- 用 `MutableStateFlow<Boolean>` 作为 "正在重置" 的信号量
- 新推理开始前通过 `_isResettingConversation.first { !it }` 等待重置完成
- 重置时通过 `buildSystemPrompt(contextualData)` 注入最新上下文

**参数建议**：
| 任务类型 | RESET_INTERVAL | 理由 |
|---------|----------------|------|
| 函数调用任务（如 TextPlay） | 15 | 函数调用 prompt 较长，保守一些 |
| 自由对话任务（如 EdgeCodex） | 10 | 对话历史增长快，激进重置 |

**正确示例** — `customtasks/textplay/TextPlayViewModel.kt:32-63, 74-88`：
```kotlin
private val _isResettingConversation = MutableStateFlow(false)
private var turnCount = 0

fun processUserInput(...) {
    viewModelScope.launch(Dispatchers.Default) {
        _isResettingConversation.first { !it }  // 等待重置完成
        // ... sendMessage ...
        turnCount += 1
        if (turnCount >= RESET_INTERVAL) {
            resetConversation(model, tools)
        }
    }
}

private fun resetConversation(model: Model, tools: List<ToolProvider>) {
    _isResettingConversation.value = true
    LlmChatModelHelper.resetConversation(
        model = model,
        systemInstruction = Contents.of(buildSystemPrompt(gameState = lastGameState)),
        tools = tools,
        enableConversationConstrainedDecoding = true,
    )
    turnCount = 0
    _isResettingConversation.value = false
}
```

---

## 范式 4 — WebView 双向桥接

**核心规则**：

### Kotlin → JavaScript
- 用 `webView.evaluateJavascript("yourNamespace.method('$json')")`
- **必须转义** `\` 和 `'`：`json.replace("\\", "\\\\").replace("'", "\\'")`
- 链式调用：命令执行后立即回读状态（见示例）

### JavaScript → Kotlin
- `@JavascriptInterface` 注解方法的 Bridge class
- `webView.addJavascriptInterface(bridge, "Android")` 注册
- 在 Compose `update` 块中重新绑定 bridge，防止内存泄漏

**正确示例 — Kotlin→JS** — `customtasks/textplay/TextPlayScreen.kt:77-90`：
```kotlin
LaunchedEffect(webViewRef, commandFlow) {
    val webView = webViewRef ?: return@LaunchedEffect
    commandFlow.collect { command ->
        val json = Json.encodeToString(command)
        val escaped = json.replace("\\", "\\\\").replace("'", "\\'")
        webView.evaluateJavascript("textPlay.runCommands('$escaped')") { _ ->
            webView.evaluateJavascript("textPlay.getGameState()") { stateJson ->
                viewModel.updateGameState(stateJson)
            }
        }
    }
}
```

**正确示例 — JS→Kotlin** — `customtasks/edgecodex/EdgeCodexScreen.kt:197-216`：
```kotlin
class EdgeCodexJsBridge(
    private val viewModel: EdgeCodexViewModel,
    private val model: Model,
) {
    @JavascriptInterface
    fun analyzeCode(userMessage: String, code: String) {
        viewModel.analyzeCode(model, userMessage, code)
    }
}
```

---

## 范式 5 — Hilt CustomTask 注册

**核心规则**：
- 使用 `@Module @InstallIn(SingletonComponent::class)` 注册全局单例
- 使用 `@Provides @IntoSet` 将任务加入集合
- Gallery 通过注入 `Set<@JvmSuppressWildcards CustomTask>` 自动发现
- **不需要改 NavGraph**，Gallery 会根据 `task.id` 动态路由

**正确示例** — `customtasks/textplay/TextPlayTaskModule.kt`：
```kotlin
@Module
@InstallIn(SingletonComponent::class)
internal object TextPlayTaskModule {
    @Provides
    @IntoSet
    fun provideTextPlayTask(): CustomTask = TextPlayTask()
}
```

**坑**：漏写 `@IntoSet` 会导致 Gallery 启动时 Hilt 报 `cannot be provided without @Inject constructor` 错误。

---

## 范式 6 — 模型白名单 JSON 扩展

**核心规则**：
- 新增模型条目必须含以下字段（缺一不可）：
  - `name`, `modelId`, `modelFile`
  - `description`, `sizeInBytes`, `minDeviceMemoryInGb`
  - `defaultConfig` (含 `topK`, `topP`, `temperature`, `maxTokens`, `accelerators`)
  - `taskTypes`, `bestForTaskTypes`
  - `version`, `commitHash`
- `taskTypes` 的字符串必须与 Kotlin 端 `CustomTask.task.id` **严格一致**
- `commitHash` 开发阶段可用 `"main"`，**发版前必须改为具体 SHA**

**正确示例** — `model_allowlist_1_0_11_patched.json:223-244`：
```json
{
    "name": "TextPlay-270M",
    "modelId": "litert-community/functiongemma-270m-it",
    "modelFile": "functiongemma-270m-it_q8_ekv1024.litertlm",
    "description": "FunctionGemma 270M base model for TextPlay game.",
    "sizeInBytes": 288964608,
    "minDeviceMemoryInGb": 6,
    "defaultConfig": { "topK": 40, "topP": 0.95, "temperature": 1.0, "maxTokens": 1024, "accelerators": "cpu" },
    "taskTypes": ["llm_textplay"],
    "bestForTaskTypes": ["llm_textplay"],
    "version": "20260407",
    "commitHash": "main"
}
```

---

## 范式 7 — @HiltViewModel + StateFlow UI 状态

**核心规则**：
- 单一 `MutableStateFlow<UiState>` 管理所有 UI 状态
- `UiState` 是不可变 data class，用 `copy()` 更新
- 异步操作用 `viewModelScope.launch(Dispatchers.Default)`
- `try-catch-finally` 完整覆盖，**finally 必须重置 loading 标志**

**正确示例** — `customtasks/textplay/TextPlayViewModel.kt:25-63, 111-115`：
```kotlin
@HiltViewModel
class TextPlayViewModel @Inject constructor() : ViewModel() {
    private val _uiState = MutableStateFlow(TextPlayUiState())
    val uiState: StateFlow<TextPlayUiState> = _uiState.asStateFlow()

    fun processUserInput(model: Model, input: String, ...) {
        viewModelScope.launch(Dispatchers.Default) {
            setProcessing(processing = true)
            try {
                // ... 业务逻辑
            } catch (e: Exception) {
                setError(e.message)
            } finally {
                setProcessing(processing = false)  // 必须
            }
        }
    }
}

data class TextPlayUiState(
    val processing: Boolean = false,
    val resettingEngine: Boolean = false,
    val error: String? = null,
)
```

---

## 范式 8 — Compose Effect 组合

**核心规则**：
- `LaunchedEffect(deps)` 响应状态变化做副作用，依赖列表要精确
- `DisposableEffect(Unit) { onDispose { ... } }` 清理资源
- Elvis 操作符快速返回，避免空指针：`val x = ref ?: return@LaunchedEffect`

**正确示例** — `customtasks/textplay/TextPlayScreen.kt:71-90`：
```kotlin
// 1. 响应 processing 状态变化
LaunchedEffect(uiState.processing, uiState.resettingEngine) {
    // side effect on state change
}

// 2. 组件卸载时清理
DisposableEffect(Unit) {
    onDispose { /* cleanup */ }
}

// 3. 复杂依赖的流收集
LaunchedEffect(webViewRef, commandFlow) {
    val webView = webViewRef ?: return@LaunchedEffect
    commandFlow.collect { /* ... */ }
}
```

---

## 范式 9 — LlmChatModelHelper 生命周期

**核心规则**：

### Task 级初始化 (`initializeModelFn`)
```kotlin
override fun initializeModelFn(context, coroutineScope, model, onDone) {
    clearQueue()  // 清空命令 channel
    LlmChatModelHelper.initialize(
        context = context,
        model = model,
        supportImage = false,  // 按模型能力调整
        supportAudio = false,
        onDone = onDone,
        systemInstruction = Contents.of(buildSystemPrompt()),
        tools = tools,
        enableConversationConstrainedDecoding = true,  // FunctionGemma 必须 true
    )
}
```

### ViewModel 级重置 (`resetConversation`)
```kotlin
LlmChatModelHelper.resetConversation(
    model = model,
    supportImage = false,
    supportAudio = false,
    systemInstruction = Contents.of(buildSystemPrompt(currentState)),
    tools = tools,
    enableConversationConstrainedDecoding = true,
)
```

### Task 级清理 (`cleanUpModelFn`)
```kotlin
override fun cleanUpModelFn(context, coroutineScope, model, onDone) {
    clearQueue()
    LlmChatModelHelper.cleanUp(model = model, onDone = onDone)
}
```

**关键点**：
- FunctionGemma 任务 `enableConversationConstrainedDecoding = true`
- 自由对话任务 `enableConversationConstrainedDecoding = false`
- `initialize` 和 `resetConversation` 都要传 `systemInstruction`

**参考**：`customtasks/textplay/TextPlayTask.kt:59-86` + `TextPlayViewModel.kt:74-88`

---

## 范式 10 — 动态 system prompt 注入

**核心规则**：
- `buildSystemPrompt(contextualData: T? = null)` 接受可空的上下文参数
- 基础 prompt 用 `trimMargin()` 的多行字符串
- Contextual data 通过 `let { ... }` 条件拼接
- 初始化时传空（`buildSystemPrompt()`），运行时通过 reset 注入实时状态

**正确示例** — `customtasks/textplay/TextPlayTask.kt:104-146`：
```kotlin
fun buildSystemPrompt(gameState: TextPlayGameState? = null): String {
    val stateInfo = gameState?.let { state ->
        """
        |
        |Current game state:
        |- Player position: (${state.playerX}, ${state.playerY})
        |- Inventory: ${state.inventory.joinToString(", ").ifEmpty { "empty" }}
        """.trimMargin()
    } ?: ""

    return """
        |You are the action planner for TextPlay, a fully offline text adventure game.
        |
        |Available functions:
        |- movePlayer(direction, steps): ...
        |- lookAround(): ...
        |
        |Rules:
        |- Prefer function calls over text responses.
        |$stateInfo
        """.trimMargin()
}
```

---

## 新建 CustomTask 的检查清单

创建一个新 task 时，按顺序通过以下检查：

- [ ] **Tools 类** 遵循范式 1（@Tool 契约）
- [ ] **Action sealed class** 遵循范式 2
- [ ] **ViewModel** 含 turnCount 重置机制（范式 3）
- [ ] **Screen** 包含 WebView 桥接（范式 4，若需要）
- [ ] **TaskModule** 用 `@IntoSet` 注册（范式 5）
- [ ] **白名单 JSON** 新增模型条目（范式 6）
- [ ] **ViewModel** 用 `@HiltViewModel` + `StateFlow`（范式 7）
- [ ] **Screen** 正确使用 `LaunchedEffect` / `DisposableEffect`（范式 8）
- [ ] **Task** 正确调用 `LlmChatModelHelper.initialize/cleanUp`（范式 9）
- [ ] **`buildSystemPrompt`** 支持动态上下文注入（范式 10）
- [ ] **所有 Tools 和 ViewModels 都有 `Log.d(TAG, ...)` 日志**
- [ ] **所有异步操作都有 `try-catch-finally` 或 `.catch{}`**

---

## 参考源码

- **Google 官方模板**：`edge-ai-gallery/Android/src/app/src/main/java/com/google/ai/edge/gallery/customtasks/tinygarden/` （TinyGardenTools/ViewModel/Screen/Task/TaskModule 五件套）
- **TextPlay 实现**（函数调用任务）：`gallery-integration/customtasks/textplay/`
- **EdgeCodex 实现**（自由对话任务）：`gallery-integration/customtasks/edgecodex/`
- **白名单参考**：`gallery-integration/model_allowlist_1_0_11_patched.json`
