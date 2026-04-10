# MEMO-B：Codex Gallery API 适配决策备忘录

**To**: 后续接手 EdgeAI Apps 项目的工程师 / Claude 其他 session / 未来的自己
**From**: Claude Opus 4.6（基于 Codex 的 commits `ca2de92` + `b238e3c` 逆向分析撰写）
**Date**: 2026-04-10
**Re**: Codex 在 Gallery API 适配阶段做出的关键工程决策与映射逻辑

---

## 1. 背景：为什么需要这次适配

Phase 1-7（Claude 主导）交付的 `gallery-integration/` Kotlin 代码虽然遵循了 TinyGarden 的文档描述模式，但有几处与 Gallery 实际 API 不兼容：

- `CustomTask` 接口在 Claude 的版本中被实现为 `object`（singleton），而实际 Gallery SDK 要求 `class` + `@Inject constructor()`
- `taskType: String` / `displayName: String` 这类扁平字段不存在，Gallery 需要一个完整的 `Task` data class
- 缺少必需的 `initializeModelFn` / `cleanUpModelFn` / `MainScreen` 三个 override
- `TextPlayTools` 的导入路径错误（`.tools.Tool` 而非 `.Tool`）
- `EdgeCodexTaskModule.kt` 完全不存在

Codex 接手后，在 `codex/gallery-integration-fix` 分支上做了一次大规模结构性重构（10 文件，+880/-802 行），**不是简单的 API rename**，而是重新设计了 task 与 Gallery 框架的契约接点。

---

## 2. 关键决策：从 `object` 到 `class @Inject constructor()`

### 决策内容
Codex 将所有 `CustomTask` 实现从 Kotlin `object`（单例）改为 `class` 带 `@Inject constructor()`。

### 为什么
这看起来是个小改动，其实是整个集成架构的基石：

1. **Hilt 不能直接注入 Kotlin `object`**——`object` 是静态单例，Hilt 的依赖图里没有位置放
2. **Gallery 用 `Set<@JvmSuppressWildcards CustomTask>` 做动态发现**——这个 Set 需要 Hilt 提供，必然要求每个成员是可注入的实例
3. **如果是 `object`，`@Provides @IntoSet fun provide(): CustomTask = TextPlayTask` 会报 "object cannot be provided"**

### 代码对比
**Claude 原版**：
```kotlin
object TextPlayTask : CustomTask {
    override val taskType: String = "llm_textplay"
    override val displayName: String = "TextPlay"
    // ...
}
```

**Codex 适配后**（`TextPlayTask.kt:28`）：
```kotlin
class TextPlayTask @Inject constructor() : CustomTask {
    private val _updateChannel = Channel<WebViewCommand>(Channel.BUFFERED)
    // ...
    override val task: Task = Task(id = "llm_textplay", ...)
}
```

### 连锁影响
这个改动顺带解决了一个更深的问题：**per-instance 状态**。原 `object` 版本没法持有 `Channel` 这类有状态资源；改成 `class` 之后，每个 Task 实例可以持有自己的 `_updateChannel`，从 Tools 层接收事件，通过 `commandFlow` 转发给 WebView。这是 WebView 桥接架构得以工作的前提。

---

## 3. 关键决策：扁平字段 → 结构化 `Task` data class

### 决策内容
从 Claude 原版的 `override val taskType / displayName / description`（三个顶层字段）迁移到 Gallery 的 `override val task: Task(...)`（单一结构化字段）。

### 为什么
`CustomTask` 接口实际上只要求一个 `task: Task` 字段，而 `Task` 这个 data class 携带了 Gallery 显示、分类、模型绑定所需的全部元数据：

```kotlin
data class Task(
    val id: String,              // 用于路由和白名单匹配
    val label: String,           // UI 显示名
    val category: Category,      // 分类（LLM / Vision / Audio）
    val icon: ImageVector,       // 图标
    val models: MutableList<Model>,  // 动态注入的模型列表
    val description: String,
    val shortDescription: String,
    val docUrl: String,
    val sourceCodeUrl: String,
    val experimental: Boolean,
    val defaultSystemPrompt: String,
)
```

### 为什么 `models = mutableListOf()` 是空的
注意 Codex 传的是 `mutableListOf()` 空列表。这不是 bug，**这是有意的设计**：Gallery 在启动时会扫描白名单 JSON，找出所有 `taskTypes` 包含 `"llm_textplay"` 的模型条目，自动填入这个 mutable list。这就是为什么白名单 JSON 的 `taskTypes` 字段和 Kotlin 的 `task.id` 必须严格一致的原因。

### `sourceCodeUrl` 的细节
Codex 填了 `https://github.com/jupiternaut/edge-ai-apps/tree/master/gallery-integration/customtasks/textplay`，这会在 Gallery 的任务详情页显示 "View source" 按钮，点击直接跳转到本仓库。细节贴心，但也意味着如果项目 fork 或迁移，这两处 URL 要一起更新。

---

## 4. 关键决策：引入 `Channel + commandFlow` 替代 callback

### 决策内容
Codex 没有用"直接回调函数"的方式让 Tools 和 WebView 通信，而是引入了 `kotlinx.coroutines.channels.Channel`：

```kotlin
private val _updateChannel = Channel<WebViewCommand>(Channel.BUFFERED)
private val commandFlow = _updateChannel.receiveAsFlow()
private val tools = listOf(
    tool(TextPlayTools(
        onAction = { action ->
            val unused = _updateChannel.trySend(action.toWebViewCommand())
        }
    ))
)
```

### 为什么 `Channel.BUFFERED` 而不是 `Channel.RENDEZVOUS` / `Channel.UNLIMITED`
- `RENDEZVOUS`（无缓冲，默认）：send 必须等到 receive，会在 @Tool 方法里阻塞——不能接受，因为 @Tool 方法必须快速返回给模型
- `UNLIMITED`：没有背压，如果 WebView 卡住会导致内存泄漏
- `BUFFERED`：默认 64 容量，既非阻塞又有界，是唯一合理的选择

### 为什么 `trySend` 而不是 `send`
`send` 是 suspend 函数，只能在 coroutine 里调用，而 @Tool 方法不是 suspend 的。`trySend` 是非阻塞的，满缓冲时会返回失败——这正是 `val unused = ...` 的意思，Codex 明确表达了"如果发送失败也不抛错"的语义。

### 为什么有 `clearQueue()` 方法
`clearQueue()` 在 `initializeModelFn` 和 `cleanUpModelFn` 中都被调用：
```kotlin
private fun clearQueue() {
    while (_updateChannel.tryReceive().isSuccess) {}
}
```
这是**防止跨 session 的陈旧命令泄漏**到新的 WebView 实例。比如用户退出游戏再重新进入，上次遗留的命令不应该执行。

---

## 5. 关键决策：`tool(TextPlayTools(...))` 的 builder 模式

### 决策内容
Codex 用了 `com.google.ai.edge.litertlm.tool` 顶层函数来包装 ToolSet 实例：
```kotlin
import com.google.ai.edge.litertlm.tool

private val tools = listOf(
    tool(TextPlayTools(onAction = ...))
)
```

### 为什么不直接传 `TextPlayTools` 实例
因为 `LlmChatModelHelper.initialize(..., tools: List<ToolProvider>)` 要求的是 `ToolProvider` 类型，而 `TextPlayTools` 继承的是 `ToolSet`（基类）。`tool(...)` 函数的作用是把 `ToolSet` 实例适配成 `ToolProvider`，内部会通过反射扫描 `@Tool` 注解方法。

### 为什么 `tools` 是 `private val`
`tools` 在 `TextPlayTask` 的构造时创建，持有对 `_updateChannel` 的闭包引用。**一旦创建就不能重建**——如果重建，新的 `tools` 会指向新的 channel，导致已经注册到模型的旧 tools 失效。把 `tools` 作为 per-instance 的不可变字段是唯一正确的做法。

---

## 6. 关键决策：`enableConversationConstrainedDecoding` 的 `true/false` 分野

### 决策内容
- **TextPlayTask / ViewModel**：`enableConversationConstrainedDecoding = true`
- **EdgeCodexViewModel**：`enableConversationConstrainedDecoding = false`

### 为什么
这是 LiteRT-LM 中控制**模型输出是否被约束为预定义的工具调用格式**的开关：

- **`true`（受约束解码）**：模型只能输出合法的函数调用或明确的文本响应，不会乱编函数名。FunctionGemma 270M 的训练目标就是输出结构化函数调用，打开这个约束能大幅降低幻觉率。
- **`false`（自由解码）**：模型可以输出任何文本。Gemma 4 E2B 做自由对话时，EdgeCodex 需要完整的 Markdown / 代码块 / 多段落输出，约束解码会破坏这种能力。

**教训**：这个开关容易被忽略，但直接决定了推理质量。新建任务时，问自己"我需要模型的输出遵守固定格式吗？"，如果是→true，否则→false。

---

## 7. 关键决策：`EdgeCodexTaskModule.kt` 的补全

### 决策内容
Claude 原版只有 `TextPlayTaskModule.kt`，没有 `EdgeCodexTaskModule.kt`。Codex 新建了这个文件（18 行）：

```kotlin
@Module
@InstallIn(SingletonComponent::class)
internal object EdgeCodexTaskModule {
    @Provides
    @IntoSet
    fun provideEdgeCodexTask(): CustomTask = EdgeCodexTask()
}
```

### 为什么这是编译通过的必要条件
没有这个 module 文件，Hilt 会报：
```
cannot be provided without an @Inject constructor or an @Provides-annotated method
```
因为 `EdgeCodexTask` 虽然有 `@Inject constructor()`，但 Hilt 需要知道它属于哪个 Set binding。`@IntoSet` 是唯一的注册入口。

**规则**：每新增一个 CustomTask，必须同时新增一个对应的 `{TaskName}TaskModule.kt`——没有例外。

---

## 8. 关键决策：白名单 JSON 的 schema 规范化

### 决策内容
`b238e3c` 这个 commit 对 `model_allowlist_1_0_11_patched.json` 做了 501 行改动。不是改了 501 个值，而是**重新格式化 + 字段补全 + 对齐了 Gemma-4-E2B/E4B 条目的 schema**。

### 具体做了什么
1. 为每个模型条目补齐了所有必需字段（`minDeviceMemoryInGb`, `bestForTaskTypes`, `commitHash`...）
2. 统一了 `defaultConfig` 的字段顺序和值类型（之前 `temperature` 有些是 `0.7`，有些是 `0.7f`）
3. 确保了 `llm_edgecodex` 在 Gemma-4-E2B 和 Gemma-4-E4B 的 `taskTypes` 里都有
4. 把 `TextPlay-270M` 条目完整写入，包括 `"commitHash": "main"`

### 为什么 `commitHash: "main"` 不是错误
读代码时会觉得这是偷懒：其他模型都是具体 SHA，为什么这个用 "main"？
**Codex 的判断**：`litert-community/functiongemma-270m-it` 还在活跃开发，HuggingFace 主分支可能随时有修补。固定到某个 SHA 意味着错过热修。用 `"main"` 是"开发期灵活性 > 生产期可重现性"的权衡。

**后果**：`REQUIREMENTS.md` Section 9 Pre-Release Checklist 里明确要求发版前必须固定——这是 Codex 的决策和发版流程之间的 handshake。

---

## 9. 关键决策：保留还是重写 Claude 的代码

### 决策分析
Codex 面对 10 个 Claude 原版 Kotlin 文件时，有三个选项：
1. **全部重写**：最干净，但浪费 Claude 已经做对的部分（sealed class 设计、游戏世界定义、system prompt 等）
2. **逐行修改**：最保守，但会在不兼容的地方产生"代码补丁之上的补丁"
3. **保留业务逻辑 + 重写 Gallery 契约层**：Codex 的选择

### Codex 保留的部分
- `TextPlayTools` 的 8 个 `@Tool` 方法定义（参数、描述、事件类型）
- `TextPlayAction` sealed class 层级
- `TextPlayGameState` data class
- `buildSystemPrompt()` 里的游戏世界描述（16x12 地图、NPC 位置、任务链）
- `EdgeCodexTask` 的编程助手 system prompt
- `CodexConfig` 的参数结构（temperature/topK/topP/maxTokens/systemPrompt）

### Codex 重写的部分
- `CustomTask` 接口实现方式（`object` → `class @Inject constructor()`）
- 所有 `initializeModelFn` / `cleanUpModelFn` / `MainScreen` override
- ViewModel 的 `sendMessage` / `sendMessageAsync` 调用签名
- `resetConversation` 的参数列表
- Channel + commandFlow 桥接机制

### 教训
**不要在别人写的代码上"只改最小必要改动"**。如果接手一段代码发现其框架假设错了，干脆利落地重写框架层，保留业务层——比打补丁更清晰。Codex 这次 802 行的删除不是浪费，是必要的"清地基"。

---

## 10. Codex 没做 / 留给后续的事情

基于我对这两个 commit 的分析，Codex 明确**没有**做以下事情：

1. **没有加日志**——这是 Phase 8b 的代码评审发现的，由 Claude 补上（commit `388de13`）
2. **没有处理流式推理的异常细节**——`try-catch` 已经存在，但缺 `Log.e` 增强
3. **没有写 `PATTERNS.md`**——Codex 写了可运行的代码，但没有沉淀设计原则
4. **没有解决 HuggingFace OAuth 配置**——`ProjectConfig.kt` 的 `clientId` / `redirectUri` 仍然是空的
5. **没有做端到端真机测试**——只做到了 `./gradlew assembleDebug` 通过和产出 APK，没有验证应用在真机上能跑起来、能下载模型、能完成一次完整的游戏/编程会话
6. **没有做性能测试**——没有 benchmark 推理延迟、内存占用、电池消耗

**这是合理的范围控制**：Codex 的 PR 标题是 "Adapt gallery integration to current Gallery APIs"，它严格按这个范围做事，没有膨胀。

---

## 11. 接手建议（给下一个接手的人）

如果你现在要继续推进 EdgeAI Apps：

### 立即可做（基于 Codex 已有工作）
1. 安装 APK 到真机：`adb install -r C:\Users\gengr\projects\edge-ai-gallery\Android\src\app\build\outputs\apk\debug\app-debug.apk`
2. 在 Gallery 里找到 TextPlay / EdgeCodex 入口
3. 下载 FunctionGemma 270M 和 Gemma 4 E2B 模型
4. 跑一次完整的游戏流程 / 代码解释流程
5. `adb logcat | grep -E "TextPlayTools|TextPlayViewModel|EdgeCodexViewModel"` 验证日志

### 短期任务（1-2 天）
1. 配置 HuggingFace OAuth（ProjectConfig.kt）
2. 补充端到端 instrumentation test（至少一个基础 smoke test）
3. 把 `"commitHash": "main"` 替换为具体的 HuggingFace commit SHA
4. 做一次推理延迟 benchmark，在 Pixel 8 / Samsung S24 等目标设备上

### 中期任务（1 周）
1. 微调 FunctionGemma 270M 在 TextPlay 指令集上
2. 实现模型增量下载 / 分片下载优化首次体验
3. 加入应用内崩溃报告收集（本地存储，用户手动导出）
4. UI 优化：适配平板和折叠屏

### 长期任务
1. iOS 版本移植
2. 桌面版本（Electron + LiteRT-LM Windows binding）
3. 新增 CustomTask（遵循 PATTERNS.md 的 checklist）

---

## 12. 致谢与信号

Codex 的这次适配工作展示了几个值得学习的工程特质：

1. **精准的范围控制**：PR 严格限定在"Gallery API 适配"，没有顺手修无关代码
2. **对 Hilt 依赖图的理解**：从 `object` → `class` 的转换体现了对 DI 框架的深入理解
3. **实际验证的严谨性**：不是"代码看起来对"，而是真的跑通了 `./gradlew assembleDebug`
4. **配置细节的完整性**：`b238e3c` 单独一个 commit 专门处理白名单 schema，说明关注点分离做得好

作为代码评审人，我给这份工作 8.7/10 分——扣的 1.3 分主要是**日志和 PATTERNS.md 缺失**，这是 Claude 在 Phase 8b 补上的。

---

*— Claude Opus 4.6（基于 Codex commits `ca2de92` + `b238e3c` 逆向分析撰写）*
*2026-04-10*
