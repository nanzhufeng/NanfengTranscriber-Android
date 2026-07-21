# 验证证据

## 2026-07-20 模拟器推理原型

- AVD：`NanzhufengFindN5Api35`，Android 15、ARM64、`1140 × 2616`。
- 原生引擎：whisper.cpp `v1.9.1`，提交 `f049fff95a089aa9969deb009cdd4892b3e74916`。
- 模型：`ggml-tiny.en-q5_1.bin`，仅临时放入模拟器 App 私有目录，不进入 APK 和 Git。
- 真实输入：上游 `samples/jfk.wav`，16 kHz 单声道，11 秒。
- 结果：1 项仪器测试通过，2 个分段，文本命中 JFK 样本内容。
- 本次记录：模型加载 220 ms、推理 14,731 ms、实时系数约 1.339。
- 缓存：同签名 `adb install -r` 覆盖升级前后 SHA-256 一致；模拟器完整重启后 SHA-256 仍一致。

这只能证明 Android ARM64 JNI、真实模型、真实 PCM 和缓存目录链路成立，不能替代 OPPO Find N5 真机性能、温度、长音频和覆盖升级验收。

界面截图：[`emulator-engine-probe.png`](emulator-engine-probe.png)。机器可读结果：[`emulator-real-inference.json`](emulator-real-inference.json)。

## 2026-07-20 完整媒体链路

- 真实输入经 `MediaExtractor + MediaCodec` 解码到 no-backup 私有 PCM16 文件。
- PCM 以 5 分钟为默认块进入 whisper.cpp，避免两小时素材整段驻留内存。
- JFK 样本完整链路：解码 207 ms、模型加载 298 ms、推理 15,935 ms，2 个有效分段。
- Room 文件数据库关闭并重开后，运行中任务仍存在，并被明确恢复为 `RECOVERY_REQUIRED`。

机器可读结果：[`emulator-full-pipeline.json`](emulator-full-pipeline.json)。该结果仍是模拟器证据，不是 Find N5 性能证据。

## 2026-07-20 Find N5 正式签名验收

- 设备：OPPO Find N5（PKH120），Android 16、`1140 × 2616 / 442dpi / font scale 1.0`。
- 正式包：`com.nanzhufeng.transcriber`，`0.2.1-engine-acceptance (3)`。
- APK SHA-256：`85c8211786bc9a48d5b4051c18f3fde150d36ef3a34fef049484a91d6bd804ff`。
- 证书 SHA-256：`ee95f8d726b8fb85f15d0eb26d385505cb20a22cf1d749ec3e4327d7ee783174`。
- 安装：版本 2 首次安装成功，版本 3 通过同签名 `pm install -r --user 0` 覆盖升级成功；未触发 ColorOS 确认页，未卸载、未清数据。
- 冷启动：125 ms，进程存活，目标进程日志未发现 `FATAL EXCEPTION` 或 `AndroidRuntime` 崩溃。

外屏证据：[`find-n5-signed-engine-acceptance.png`](find-n5-signed-engine-acceptance.png)。本次只证明正式签名、覆盖升级、冷启动和真机 UI 成立；尚未证明模型下载、缓存复用或真机转写性能。

后续 `0.3.0-operational-acceptance (4)` 已在同一 Find N5 上完成 Base 模型首次下载、SHA 校验与原子提交，页面确认模型已就绪；对应外屏证据为 [`find-n5-operational-main.png`](find-n5-operational-main.png)。真机真实媒体转写速度仍未验证。

## 2026-07-20 三页工作台 UI 模拟器验收

- 依据：Codex 南枫下载原任务最终确认回合、当前安装版与权威源码；旧 `find-n5-home-*`、`reference`、`final-pairs` 和拼图已排除。
- 外屏：`1140 × 2616 / 442dpi`，底部导航，主页/历史/设置各自独立根视口。
- 内屏：`2248 × 2480 / 442dpi`，左侧导航；主页重排为主工作区 + 右侧模型/添加任务，设置重排为 2×2 语义卡片。
- 主页：Base 长期缓存与一条真实 Room 转写记录被正确读回；完整工作区骨架和添加任务区保持稳定。
- 历史：搜索、状态筛选和真实 Room 任务列表已接入；日期固定为中文可读的 `yyyy-MM-dd HH:mm`。
- 设置：语言、CPU 线程、保持屏幕常亮均写入 DataStore；“切换中文 → 杀进程 → 冷启动”后选项仍保留，语言和线程真实传入 whisper.cpp 工作流。
- 当前只开放真实启用的“工作台青”；其余五套皮肤未放假开关，仍待按南枫记当前正式令牌逐套实现。
- 本轮只在项目专用模拟器安装 Debug 包；未向 OPPO 安装新 UI 包，不能报告为正式真机视觉验收。

六页独立证据：

- [`emulator-ui-outer-home.png`](emulator-ui-outer-home.png)
- [`emulator-ui-outer-history.png`](emulator-ui-outer-history.png)
- [`emulator-ui-outer-settings.png`](emulator-ui-outer-settings.png)
- [`emulator-ui-inner-home.png`](emulator-ui-inner-home.png)
- [`emulator-ui-inner-history.png`](emulator-ui-inner-history.png)
- [`emulator-ui-inner-settings.png`](emulator-ui-inner-settings.png)

## 2026-07-20 Find N5 三页正式 UI 验收

- 正式包：`com.nanzhufeng.transcriber`，`0.4.0-ui-acceptance (5)`。
- APK：48,075,999 B，SHA-256 `26fcab54712fe4038eb02014c8fd11aac7ad8c08d9f2d9990e0e0154afe208cc`。
- 安装前从手机拉取版本 4 APK，与新版本 5 的证书逐项比较；两者 SHA-256 均为 `ee95f8d726b8fb85f15d0eb26d385505cb20a22cf1d749ec3e4327d7ee783174`。
- 安装：推送到 `/data/local/tmp` 后执行 `pm install -r --user 0`，返回 `Success`；未卸载、未清数据，临时 APK 已清理。
- 冷启动：首次 195 ms；设置持久化复核时 146 ms；目标进程日志没有 `FATAL EXCEPTION`、`AndroidRuntime` 或进程死亡记录。
- 缓存保留：覆盖后主页和设置都显示 Base 141.1 MB 已校验、可长期复用，没有重新下载模型。
- 历史：真机当前没有历史记录，页面如实显示空状态；没有用模拟数据冒充用户记录。
- 设置：切换“中文 + 4 线程”，杀进程冷启动后仍保持选中；完成证据采集后恢复“自动识别 + 自动线程”。
- 外屏：`1140 × 2616 / 442dpi / font scale 1.0`，底部导航三页独立验收。
- 内屏：`2248 × 2480 / 442dpi`，左侧导航；主页双栏、历史自适应列表和设置 2×2 模块通过。
- 折叠：由 `CLOSED (0)` 切到 `OPENED (3)` 时当前设置页保持；完成后显式恢复 `CLOSED (0)`，最终再次确认外屏尺寸。
- 门禁：18 项 JVM 测试 0 失败，Debug lint 与 Release vital lint 通过，Release 双 ABI 构建通过。

真机证据：

- [`find-n5-ui-v5-outer-home.png`](find-n5-ui-v5-outer-home.png)
- [`find-n5-ui-v5-outer-history.png`](find-n5-ui-v5-outer-history.png)
- [`find-n5-ui-v5-outer-settings.png`](find-n5-ui-v5-outer-settings.png)
- [`find-n5-ui-v5-inner-home.png`](find-n5-ui-v5-inner-home.png)
- [`find-n5-ui-v5-inner-history.png`](find-n5-ui-v5-inner-history.png)
- [`find-n5-ui-v5-inner-settings.png`](find-n5-ui-v5-inner-settings.png)
- [`find-n5-ui-v5-settings-persistence.png`](find-n5-ui-v5-settings-persistence.png)

## 2026-07-20 版本 6 后台转写与恢复验收

- 正式版本：`0.5.0-background-transcription (6)`；APK SHA-256 `0781ba4b636bc5bf96994fcaa5b2d2f28cbe97f9ce346f89df1fe54787efc21f`。
- 门禁：19 项 JVM 测试 0 失败；Debug lint、Release vital lint 和 Release 双 ABI 构建通过；Room v1→v2 迁移仪器测试通过。
- API 35 模拟器：真实 JFK WAV 开始转写后立即返回桌面，`mediaProcessing` 前台服务持续运行，最终任务为 `COMPLETED`，通知、`transcript.json` 和 `transcript.txt` 均存在。
- 已验证通知取消只终止 `:transcription` 工作进程，UI 进程继续存活；手动杀死工作进程后，系统重投递同一任务并将 attempt 从 1 增至 2。
- 历史页可打开完整结果，并将该历史结果重新导出为通过 ZIP 完整性检查的 DOCX OpenXML 文件。
- 正常完成、取消和超时路径均会释放 wake lock、停止前台服务并终止独立工作进程，避免模型内存长期滞留。
- Find N5 安装前后正式证书 SHA-256 均为 `ee95f8d726b8fb85f15d0eb26d385505cb20a22cf1d749ec3e4327d7ee783174`；使用 `pm install -r --user 0` 覆盖成功，未卸载、未清数据、未抢占用户正在使用的相册页面。
- Find N5 本轮未跑真实媒体转写，因此不能把模拟器速度或后台证据报告为真机性能结论。

模拟器后台运行界面：[`simulator-v6-background-active.png`](simulator-v6-background-active.png)。

## 2026-07-20 Find N5 短样本与 60 分钟压力门禁

- 真机版本 6、已缓存 Base 模型、11 秒 JFK：解码 72 ms、模型加载 164 ms、转写 3,067 ms、实时系数 0.28；完整英文文本正确。
- 60 分钟 QA 输入为重复 JFK 的 16 kHz 单声道 WAV，115,200,078 B；只用于压力与恢复验收，不使用用户私人媒体。
- 任务在桌面后台保持 `mediaProcessing` 前台服务，通知真实更新到 255 个片段；页面返回时能从 Room 读回 25% 等进度。
- 58 个采样点记录：电池温度 30.4–38.2°C；PSS 392,849–477,580 KB；RSS 519,120–586,628 KB；系统热状态最高观测为 2（中等）。
- 21:12:43，工作进程 PID 12465 在约 77% 时退出。Android Historical Process Exit 记录 `reason=13 (OTHER KILLS BY SYSTEM)`、RSS 525 MB、`Cached(nirvana)[(fg-service){fg-service}]`；没有 Java 崩溃或用户取消证据。
- 版本 6 随后能重启工作进程，但会从头准备音轨，故此轮 60 分钟门禁判定为失败，不得写成“后台长转写已通过”。
- 测试结束后，真机 `/sdcard/Download/NanfengTranscriber-QA` 已删除；没有保留用户无关的大文件。

逐 10 秒采样数据：[`find-n5-v6-60m-monitor.csv`](find-n5-v6-60m-monitor.csv)。系统退出摘要：[`find-n5-v6-worker-exit.txt`](find-n5-v6-worker-exit.txt)。

## 2026-07-20 版本 7 分块断点修复

- 正式版本：`0.6.0-resumable-transcription (7)`；APK SHA-256 `0f6544224040a29f51a4a766201dcc95bdb4916322b4fef0ffe0209776168903`。
- 每完成 5 分钟分块，先原子保存累计分段、绝对时间戳、检测语言、已处理样本和性能数据，再更新 Room/通知进度。
- 恢复前校验源、模型、语言、线程数、PCM 采样参数和文件长度；匹配才从断点继续，否则重新准备音轨。
- 22 项 JVM 测试 0 失败；Debug lint、Release vital lint、Debug/Release 双 ABI 构建通过。
- API 35 模拟器版本 7 使用真实 JFK 完成后台回归：解码 224 ms、模型加载 488 ms、转写 53,080 ms、实时系数 4.83；完成后 PCM、断点、服务、工作进程和 wake lock 均已清理。
- Find N5 已同签名覆盖到版本 7，未卸载、未清数据、未抢占用户正在使用的南枫记；但版本 7 的真机断点恢复长链仍待重测。
