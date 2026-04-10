# MEMO-A：EdgeAI Apps 设计范式备忘录

**To**: 后续接手 EdgeAI Apps 项目的工程师 / Claude 其他 session / 未来的自己
**From**: Claude Opus 4.6（Phase 1-7 主 + Phase 8b 评审）
**Date**: 2026-04-10
**Re**: 从 TextPlay / EdgeCodex 两个原型沉淀出的设计理念与工程范式

---

## 1. 核心设计哲学（三条）

### 1.1 离线优先，不是离线备选
这两个应用不是"有网用云端，没网降级跑本地"的 hybrid 架构。它们从第一行代码就假定设备**永远没有网**。这带来了几个关键约束：

- 模型必须能塞进手机内存（E2B ~5GB，E4B ~8GB，FunctionGemma 270M ~289MB）
- 推理延迟必须 < 2 秒才算可用
- 所有前端资源（HTML/CSS/JS）打包到 APK 的 `assets/` 目录，不走 CDN
- 模型文件用 `WebViewAssetLoader` 走 `https://appassets.androidplatform.net/` 本地虚拟域名加载

**反面教训**：不要引入任何 "fallback to cloud API" 的代码路径。一旦允许一个口子，隐私承诺就破了，商业叙事也垮了。

### 1.2 隐私即产品（Privacy as Feature）
离线不只是技术选择，更是商业叙事。两个应用的差异化都建立在"数据永不离开设备"上：
- **EdgeCodex**：企业级/政府/金融开发者的真正痛点——云端 AI 助手被合规部门禁用
- **TextPlay**：玩家的游戏对话历史、角色扮演内容、创作过程都是隐私敏感的

这决定了几个工程细节：
- 不写任何遥测/analytics 代码
- 不要求任何在线账号（HuggingFace OAuth 只用于首次下载模型）
- 日志只写 logcat，不上传
- crash report 本地存储，用户手动导出

### 1.3 WebView 是特性，不是权宜之计
初看 WebView 桥接像是"没时间写原生 UI 的偷懒"，实际上这是一个深思熟虑的架构选择：
- **跨平台复用**：HTML/JS 前端可以直接在 iOS（通过 WKWebView）、桌面 Electron、甚至纯浏览器中运行——iOS 和 Web 版本几乎零成本
- **快速迭代**：UI 改动不需要 Kotlin 重新编译（assets 热加载）
- **美术资产零门槛**：emoji + Canvas 2D 就够了，不需要 sprite sheet pipeline
- **FunctionGemma 天然契合**：模型的函数调用输出 → JS 的 `textPlay.runCommands(json)` 是直接映射

这也是 Google 官方 Tiny Garden 选择的架构，不是巧合。

---

## 2. 十条可复用范式（checklist 形式）

完整的带 `文件:行号` 引用的版本在 `gallery-integration/PATTERNS.md`。这里是高频提醒清单：

- [ ] **@Tool 返回 `Map<String, Any>`**，必须含 `"result"` 字段
- [ ] **import 是 `com.google.ai.edge.litertlm.Tool`**（不是 `.tools.Tool`——这个坑踩过一次了）
- [ ] **Sealed class 定义所有工具事件**，ViewModel 用 `when` 穷尽匹配
- [ ] **对话重置机制** — `turnCount` + `_isResettingConversation: StateFlow<Boolean>`
  - FunctionGemma 任务 15 轮重置
  - 自由对话 10 轮重置
- [ ] **WebView Kotlin→JS 转义**：`json.replace("\\", "\\\\").replace("'", "\\'")`——漏一个就 JS 语法错误
- [ ] **Hilt `@IntoSet`** 注册 CustomTask，**不要**手改 NavGraph
- [ ] **白名单 JSON 的 `taskTypes`** 必须与 `CustomTask.task.id` 严格一致
- [ ] **ViewModel 的 finally 块必须重置 `processing = false`**，否则卡死 loading spinner
- [ ] **`LaunchedEffect(deps)` 的依赖列表精确到具体 state**，避免 recomposition 风暴
- [ ] **`buildSystemPrompt(context)` 可空参数模式**——初始化用空，运行时注入真实 state

## 3. 质量基线（每个 PR 必过）

基于代码评审发现，定义三条最低质量线：

### 3.1 日志可见性
**规则**：每个 `@Tool` 方法、每个 ViewModel 公开方法、每个异步操作入口，都必须有 `Log.d(TAG, "...")`。
**原因**：端侧推理没有云端 observability，出问题只能靠 `adb logcat`。参考 `TinyGardenTools.kt` 的日志密度。

### 3.2 异常路径的可观察性
**规则**：`try-catch-finally` 中的 `catch` 块必须同时做两件事：
1. `Log.e(TAG, "...", e)` — 写入 logcat
2. `setError(e.message)` — 更新 UI 状态

**反例**：只 `setError` 不 `Log.e` 会让调试极其痛苦，因为 UI 只显示简化消息，没有堆栈。

### 3.3 生命周期清理
**规则**：任何 `Channel` / `Job` / `WebView` / `Engine` 都必须在对应的 `cleanUp` 或 `DisposableEffect.onDispose` 中显式释放。
**现状**：`TextPlayTask.cleanUpModelFn` 调用了 `clearQueue() + LlmChatModelHelper.cleanUp()`，这是正确的模板，新任务照抄即可。

---

## 4. 模型能力与 prompt 工程权衡

### 4.1 FunctionGemma 270M 的能力边界
- **强项**：简单的单句指令 → 函数调用映射（"walk north" → `movePlayer("north", 1)`）
- **弱项**：多步骤规划、上下文推理、模糊指令消歧
- **应对**：system prompt 里明确写 "Break multi-step requests into multiple function calls in order"（见 `TextPlayTask.kt:141`）
- **不要指望**：让它做任何文本生成类任务（描述、对话、叙事）——那是 Gemma 4 E2B 的活

### 4.2 Gemma 4 E2B 的适用场景
- **代码解释 / 重构建议**：LiveCodeBench 44%，对 EdgeCodex 够用
- **多轮对话**：128K context 窗口足够大多数编程 session
- **不擅长**：精确的函数调用（比 FunctionGemma 270M 差很多）、长篇创作

### 4.3 分工原则
**TextPlay**: FunctionGemma 270M 做动作理解 + 游戏 JS 做所有叙事文本（硬编码模板）
**EdgeCodex**: Gemma 4 E2B 做所有 AI 工作，没有函数调用层

这不是"能力不够用更大模型补"的问题，而是**架构哲学不同**：游戏需要确定性的游戏状态，编程助手需要灵活的自由对话。

---

## 5. 前端测试工具链（浏览器预览）

没有 Android 环境时，前端可以独立跑：

```bash
cd edge-ai-apps
npx serve textplay/frontend -l 3000   # http://localhost:3000
npx serve edgecodex/frontend -l 3001  # http://localhost:3001
```

`.claude/launch.json` 已配置预览服务器，可以用 Claude Code 的 `preview_*` 工具链直接操作：
- `preview_start` / `preview_stop` — 启停
- `preview_snapshot` — 获取可访问性树（文本化 UI 结构）
- `preview_eval` — 执行任意 JS（比如 `textPlay.runCommands(...)` 直接测游戏逻辑）
- `preview_fill` + `preview_click` — 模拟用户输入

**坑**：`preview_screenshot` 在 requestAnimationFrame 持续循环的页面上会超时。改用 snapshot + eval 组合。

---

## 6. 未来扩展的三个方向

基于当前架构，以下扩展成本低：

### 6.1 新增 CustomTask（成本：1-2 天）
严格按 `PATTERNS.md` 的 10 条 checklist 做，新 task 的 5 文件结构：
```
customtasks/yourtask/
├── YourTaskTools.kt       // @Tool 方法 + sealed class 事件
├── YourTaskViewModel.kt   // @HiltViewModel + StateFlow + 对话重置
├── YourTaskScreen.kt      // Compose + WebView + JS 桥接
├── YourTask.kt            // CustomTask 接口实现
└── YourTaskTaskModule.kt  // Hilt @IntoSet 注册
```
在白名单 JSON 里加模型条目，Gallery 自动发现。

### 6.2 模型升级（成本：< 1 天）
当 Google 发布 Gemma 4.1 或 FunctionGemma v2 时：
1. 在 `model_allowlist_1_0_11_patched.json` 加新条目
2. 新条目的 `taskTypes` 复用现有的 `llm_edgecodex` / `llm_textplay`
3. 在应用内的模型管理页面切换默认模型
4. 不需要改任何 Kotlin 代码

### 6.3 iOS 版本（成本：1-2 周）
Google 官方 AI Edge Gallery 已经有 iOS 版本。迁移路径：
1. 前端 HTML/CSS/JS 资源直接复用
2. Kotlin 层需要重写为 Swift，但模式可以直接照搬：
   - `@Tool` → Swift 的 function calling API
   - `@HiltViewModel` → Swift 的 `@Observable` + `@StateObject`
   - `WebView` → `WKWebView`
3. LiteRT-LM 的 iOS binding 已经就绪

---

## 7. 不要做的事情（anti-patterns）

基于 Phase 1-8 的试错经验：

1. **不要用 CodeGemma**——它是 2024 年的旧模型，Gemma 4 E2B 更强
2. **不要 fork Gallery 整个仓库**——用 `gallery-integration/` 补丁包 + Google 官方 Gallery 的组合更干净
3. **不要在 @Tool 方法里做复杂业务逻辑**——只发出 sealed class 事件，业务逻辑放 ViewModel
4. **不要在 system prompt 里塞完整游戏规则手册**——FunctionGemma 270M 的 context 有限，只写核心动作和当前状态
5. **不要尝试让 FunctionGemma 生成叙事文本**——它不擅长，让游戏 JS 做这个
6. **不要用 `Map<String, String>` 做 @Tool 返回类型**——会导致 `error.NonExistentClass`
7. **不要忘记对话重置**——连续 20+ 轮后模型会开始说胡话
8. **不要硬编码 commitHash 为 "main"**——这是当前 TextPlay-270M 条目的已知债，发版前必须固定

---

## 8. 关键引用

| 资源 | 路径 |
|------|------|
| 完整范式文档（带代码引用） | `gallery-integration/PATTERNS.md` |
| 开发历程 | `DEVLOG.md` |
| 构建环境要求 | `REQUIREMENTS.md` |
| Google 官方参考实现 | `edge-ai-gallery/Android/src/app/src/main/java/com/google/ai/edge/gallery/customtasks/tinygarden/` |
| HuggingFace 模型 | `litert-community/functiongemma-270m-it`, `litert-community/gemma-4-e2b-it` |
| LiteRT-LM Kotlin docs | https://github.com/google-ai-edge/LiteRT-LM/blob/main/kotlin/README.md |

---

*— Claude Opus 4.6*
*2026-04-10*
