# WordMemo 📱

Android 单词记忆 App（Kotlin + Jetpack Compose + Material3）：列表/卡片双模式学习、生词/熟练/忘记三状态管理、AI 拍照识词导入、AI 助教对话、离线词典阅读翻译。

> 本迭代（v0.4.0）参考 AI 新版 Web 端（Vue 3 + Spring Boot 的 ai-word-assistant）的功能设计进行了对齐与完善，详见下方「更新日志」。

## ✨ 核心功能

### 学习
- **列表模式**：行内三状态快速切换（生词/熟练/忘记）、发音（TTS）、行底色随状态区分、隐藏熟练词翻译（点击揭示）
- **卡片模式**：3D 翻转卡片、滑动切词、四种切换动画（平移/翻转/缩放/淡出）、认识/忘记常驻按钮
- **多词书管理**：按词书隔离学习与统计，学习队列只取当前词书
- **每日目标**：今日已学 X/目标 Y，跨天自动清零
- **自测模式**：熟练词翻转前隐藏释义，先回忆再对照
- **自动发音**：切换卡片自动朗读（可在设置关闭）

### 导入（多种方式）
- 📷 **AI 拍照识词**：拍照/相册选图，AI 提取生词为 JSON 预览确认后导入（兼容 OpenAI Chat Completions 格式）
- 📄 **JSON 词书**：兼容 `{book, words}`、顶层数组、网页版 `definition` 字段、中文字段名、纯单词字符串数组
- 📊 **CSV/文本词书**：支持 `单词,音标,释义[,单元]`、Tab 分隔（Excel 直接粘贴）、纯单词列表
- 释义缺失时**内置离线词典自动补全**，无需 AI

### AI 能力
- **AI 助教对话**：SSE 流式回复、单词上下文讲解（词根词缀/记忆技巧/易混词）
- **助教人设可配置**：自定义 System Prompt（参考网页版动态 Prompt），支持恢复默认
- **温度可调**：对话温度 0~1.5 滑条实时调节
- **阅读翻译**：文章阅读模式、点词翻译（内置离线词典免 API）、逐句/全文 AI 翻译

### 外观
- Material You（Monet 动态取色）与 MIUI X 双界面风格
- 深色模式（跟随系统/浅色/深色）
- 7 套预设主题色 + 自由取色器（一级/二级强调色）
- **自定义背景壁纸**：选择本地图库图片作为全站背景，自动加蒙层保证可读性

### 数据
- Room 本地数据库（含 v1→v2 迁移）
- JSON 整库导出/导入备份

## 🛠️ 技术栈

- Kotlin 2.0 + Jetpack Compose (BOM 2024.12) + Material3
- Room + KSP、Hilt、DataStore Preferences
- OkHttp / Retrofit / kotlinx.serialization（OpenAI 兼容 API，SSE 流式）
- CameraX + Coil（拍照识词）、Accompanist Permissions
- Navigation Compose 单 Activity + 底部 4 Tab（HorizontalPager）

## 🚀 构建

1. Android Studio 打开本项目（或本机安装 JDK 17 + Android SDK）
2. `./gradlew assembleDebug`
3. 产物：`app/build/outputs/apk/debug/app-debug.apk`

## ✍️ 签名说明

- 项目使用**仓库内置的项目专属 keystore** 签名：`keystore/wordmemo.keystore`
  - alias：`wordmemo`，store/key 密码：`wordmemo`（debug/社区发行惯例，随仓库分发）
  - `debug` 与 `release` 构建均使用该签名
- **任何机器**上用本仓库构建出的 APK 签名一致，可直接互相覆盖安装升级
- 若从 v0.3.8 及更早版本升级：早期 APK 由原开发机 debug 密钥签名，与本签名不一致，需**先卸载旧版再安装**（旧版本地数据会清空）
- 请勿更换该 keystore，否则又将出现「签名不一致」无法覆盖安装的问题

## ⚙️ AI 配置

设置 → AI 配置：
- Base URL（如 `https://api.openai.com/v1` 或任意兼容网关）
- API Key
- 模型名（支持一键拉取 `/models` 列表）
- 助教人设（System Prompt）与对话温度
- 拍照识词提示词（默认即可，也可自定义）

## 📜 更新日志

### v0.5.0（Miuix HyperOS 重构 + 性能优化，参考 KernelSU 系管理器）
- ✅ **接入真 Miuix 组件库**（`top.yukonga.miuix.kmp:miuix:0.8.8`，KernelSU/SukiSU 同款 HyperOS 风格组件体系）：
  - 「MIUI X」界面模式重构为真 Miuix 实现：`MiuixTheme` 全局配色 + Miuix 底部导航栏 + Miuix 设置页（`Scaffold`/`SmallTopAppBar`/`TabRow`/`Card`/`SuperSwitch`/`Slider`/`TextField`）
  - 不再是单纯换色主题；`MaterialTheme` 通过 Miuix→M3 色板映射继承同一套 HyperOS 配色，两套界面风格切换无功能差异
- ✅ **整体性能优化（掉帧修复）**：
  - 主题 remember 化：Monet/Miuix 整套配色只在种子色/深浅切换时重算，不再随每次重组重建
  - 卡片滑动改同步 State 写入 + `graphicsLayer` 延迟读取，去掉逐帧协程启动（原 `Animatable.snapTo` per-delta 模式）
  - AI 聊天流式滚动按 80 字符节流，消除逐 token 滚动掉帧
  - Pager `beyondViewportPageCount=1`：相邻 Tab 预保留，左右滑动不重建
  - **提供 release 签名 APK**：无调试开销，流畅度显著高于 debug 版
- ✅ **工具链全面升级**：Gradle 9.6.0 / AGP 9.4.0 / Kotlin 2.3.20 / KSP 2.3.11 (KSP2) / Hilt 2.60.1 / Room 2.8.4 / Compose BOM 2026.08.00 / compileSdk 37
- ✅ `kotlinOptions` 迁移到 `compilerOptions` DSL；AGP 9 以 `builtInKotlin=false` + `newDsl=false` 旁路运行（AGP 10 前需迁移 built-in Kotlin）

### v0.4.0（参考 AI 新版 Web 端完善）
- ✅ 词书隔离：首页列表/统计/学习队列按当前词书过滤，首页显示当前词书
- ✅ 每日学习进度：今日已学/每日目标展示（跨天自动清零），标熟/忘记自动累计
- ✅ AI 助教人设（System Prompt）可配置 + 对话温度可调；修复无上下文时默认 system prompt 不生效的问题
- ✅ 自动发音真正生效：卡片切换/标词后自动朗读当前单词
- ✅ 每日目标滑条修复（此前拖动无效）
- ✅ 导入增强：JSON 兼容网页版 `definition`/`unit` 格式与中文字段、纯单词数组；新增 CSV/文本词书导入；内置词典自动补全释义
- ✅ 修复词书页加号菜单「拍照/相册」入口未透传 `mode` 导致不自动打开的问题
- ✅ 自定义背景壁纸（外观设置选择图片，全局蒙层适配深浅色）

### v0.3.x（历史）
- 文章阅读模式、离线翻译词典、MIUI X 界面风格、Monet 调色、自测模式、JSON 导入、AI 助教、3D 翻转卡片等（详见 git 提交记录）

## 📜 许可证

仅供学习交流使用。
