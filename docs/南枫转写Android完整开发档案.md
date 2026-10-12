# 南枫转写 Android 完整开发档案

后续维护说明：本页为2026-10-11复盘快照，原验证不追改；2026-10-12合同已统一搜索/草稿/字幕预检/输出等边界，当前正文只按 [合同索引](contracts-index.md) 读取，新增冲突与处理见 [统一审计](qa/contracts-20261012/README.md)。

复盘日期：2026-10-11，Asia/Shanghai。源码基线：`2bad9c2d559f69642ede2e4acd7a37b2599d9b71`；发布源码：`290e03f93fe6e9df43c243d3a04d2839ba9720be`；二者 App 代码无差异。本次只修改文档和开发流程资产，不修改业务源码、测试或构建配置。

## 1. 复盘口径与范围

全量入口为 [文件与 Git 索引](qa/project-review-20261011/inventory.md)：逐项检查主仓库 314 个跟踪项，完整读取文本做结构/测试声明/JSON 检查，二进制核对类型、大小和 SHA-256；枚举全部 17 个本地可达提交。核心业务链路另按实际调用阅读、对照合同和验证。主仓库包括 28 个 JVM 测试文件、7 个仪器测试源码文件；后者只做静态检查，绝不执行。

这不是“全部逐行语义已认证”。大型 UI/ViewModel 采用职责和关键链路阅读，供应商 whisper.cpp 只核对固定提交、许可、JNI/CMake 集成；预编译 JNI 不具备本轮可重现构建证明。上游所有源码、二进制内部、未获取远端历史与设备私有数据不冒充已审计。初始未跟踪 `design-qa.md` 原样保留；忽略目录、构建产物和主设备备份不作为当前源码。初始 `main` 本地领先现有 `origin/main` 1 个文档提交，未访问远端刷新；本次不提交、推送或发布。

事实等级：当前源码/本次检查 → 对应版本的 Git/实测报告 → 历史文档；聊天与用户提供方案只说明需求，不能补造验证。历史记录保留失败、旧测试和过时设计，不回写成当前成功。后续维护入口是 [状态](PROJECT_STATUS.md)、[交接](CURRENT_HANDOFF.md)；本档案是复盘快照，不替代活跃领域/UI 合同。

## 2. 项目目标和边界

第一用户任务：本地音视频投递到顺序队列，在后台处理，私有完整结果保存后进入历史，核对媒体、编辑/复制并明确导出。起源是 Windows 工具的 Android 原生重建，不是封装 Python。当前已扩展主动选择的云端转写，但本地标准无需账号和上传。[产品简报](product-brief.md)、[验收合同](product-acceptance.md)

已实现入口包括多文件 SAF、其他 App 分享和页面拖放；不是整目录递归导入。首页只承接未完成，历史只承接完成；首要目标设备为 OPPO Find N5，最低 Android 10。最长约两小时是需求范围，不是已经通过的稳定性承诺。GPU、硬字幕 OCR、外挂字幕关联、说话人分离、暂停、波形和自动摘要不属于已完成能力。[Manifest](../app/src/main/AndroidManifest.xml)、[App 导航及文件入口](../app/src/main/java/com/nanzhufeng/transcriber/ui/NanfengTranscriberApp.kt)、[UI 合同](ui-contract.md)

## 3. 技术栈与配置

| 层 | 实际配置 / 来源 |
| --- | --- |
| 工程 | 单 `:app`；AGP 8.5.2、Kotlin/Compose plugin 2.0.20、KSP 2.0.20-1.0.25；[根构建](../build.gradle.kts)、[设置](../settings.gradle.kts) |
| Android | compile/target 35、min 29、Java/Kotlin 17、NDK 26.1.10909125、CMake 3.22.1；[App 构建](../app/build.gradle.kts) |
| UI | Compose BOM 2024.06.00、Material3、Activity Compose 1.9.1、Lifecycle 2.8.4；同一 App 构建文件 |
| 数据 | Room 2.6.1 / schema v7、DataStore 1.1.1、SAF、Android Keystore AES-GCM；[数据库](../app/src/main/java/com/nanzhufeng/transcriber/data/task/TranscriptionDatabase.kt) |
| 媒体 / 网络 | MediaExtractor/MediaCodec、Media3 inspector 1.9.4、OkHttp 4.12.0、Coroutines 1.8.1；[解码器](../app/src/main/java/com/nanzhufeng/transcriber/data/media/MediaCodecAudioDecoder.kt) |
| 推理 | sherpa-onnx v1.13.6 双 ABI JNI；whisper.cpp v1.9.1 固定 submodule；C++17；[CMake](../app/src/main/cpp/CMakeLists.txt)、[第三方声明](../THIRD_PARTY_NOTICES.md) |
| 测试 | JUnit 4.13.2、MockWebServer 4.12.0；已有 Android 测试依赖不代表允许执行；[测试源目录](../app/src/test/java/com/nanzhufeng/transcriber) |

版本为 `0.14.8 (23)`，包名 `com.nanzhufeng.transcriber`，ABI 为 arm64-v8a / x86_64。Release `debuggable=false`、未启用 minify。Gradle 每次配置以上海时区写 BUILD_TIME，因此重复生成 Release 可能改变哈希。仓库未发现跟踪的 `.github/workflows`，不能声称已有远端 CI 全平台门禁。[App 构建](../app/build.gradle.kts)、[全量索引](qa/project-review-20261011/inventory.md)

## 4. 目录与核心所有者

```text
app/src/main/java/com/nanzhufeng/transcriber/
  ui/       页面、共享组件、主题与 ViewModel
  domain/   状态、完成策略、导出、账本、岛展示策略
  data/     Room、模型资产、媒体、结果/断点、SAF、设置和凭据
  engine/   SpeechEngine、本地与云端适配器、会话与 PCM 协调
  service/  独立进程前台服务、任务 runner、队列与后台岛
app/src/main/java/com/k2fsa/   sherpa-onnx API 包装
app/src/main/cpp/             whisper JNI / CMake
app/src/main/jniLibs/         双 ABI sherpa JNI
app/src/main/res/             密度图标、通知图标、主题、备份规则
app/src/test/                JVM 行为与接口 Mock
app/src/androidTest/         历史仪器测试源码；不执行
app/schemas/                 Room 1–7 schema
third_party/                 固定 whisper 子模块、sherpa 许可证
branding/                    图标母版
scripts/                     合同检查与既有签名辅助脚本
docs/                        产品合同、决策、状态和版本化 QA
.agents/skills/              本次新增的项目流程能力
```

目录事实由 [全量索引](qa/project-review-20261011/inventory.md) 支持。逻辑分层不等于已形成多个 Gradle 模块，也不等于所有大文件职责已拆完。[架构合同](architecture-governance.md)

| 核心概念 | 实际所有者 | 消费者 / 边界 |
| --- | --- | --- |
| 依赖组合 | [Application/AppContainer](../app/src/main/java/com/nanzhufeng/transcriber/NanfengTranscriberApplication.kt) | UI 与工作进程各自初始化；Room 开启多实例失效通知 |
| 模型资产 | [ModelAssetManager](../app/src/main/java/com/nanzhufeng/transcriber/data/modelstore/ModelAssetManager.kt)、[FileModelStore](../app/src/main/java/com/nanzhufeng/transcriber/data/modelstore/FileModelStore.kt) | 下载/导入/导出串行协调，逐文件长度/哈希与提交 |
| 模型选择 | [OfficialModelCatalog](../app/src/main/java/com/nanzhufeng/transcriber/data/modelstore/OfficialModelCatalog.kt)、[SpeechEngineFactory](../app/src/main/java/com/nanzhufeng/transcriber/engine/SpeechEngineFactory.kt) | 三种公开模式；旧 Qwen3 仅旧任务兼容 |
| 任务真相 | [Room repository](../app/src/main/java/com/nanzhufeng/transcriber/data/task/RoomTranscriptionTaskRepository.kt)、[状态策略](../app/src/main/java/com/nanzhufeng/transcriber/domain/task/TranscriptionTaskState.kt) | 事务转换与条件进度更新；不由提示层重新判断完成 |
| 执行 | [runner](../app/src/main/java/com/nanzhufeng/transcriber/service/TranscriptionTaskRunner.kt)、[FGS](../app/src/main/java/com/nanzhufeng/transcriber/service/TranscriptionForegroundService.kt)、[队列](../app/src/main/java/com/nanzhufeng/transcriber/service/BackgroundTranscriptionQueue.kt) | `:transcription`、finally 统一清理、取消后不继续队列 |
| 片段/热会话 | [PCM coordinator](../app/src/main/java/com/nanzhufeng/transcriber/engine/PcmTranscriptionCoordinator.kt)、[QueueModelSessionOwner](../app/src/main/java/com/nanzhufeng/transcriber/engine/QueueModelSessionOwner.kt) | 每块提交后更新进度；按 provider/模型路径/线程复用 |
| 结果与输出 | [DocumentStore](../app/src/main/java/com/nanzhufeng/transcriber/data/result/TranscriptDocumentStore.kt)、[ExportService](../app/src/main/java/com/nanzhufeng/transcriber/domain/export/TranscriptExportService.kt)、[SAF OutputStore](../app/src/main/java/com/nanzhufeng/transcriber/data/output/AndroidTranscriptOutputStore.kt) | 私有 JSON 真相、TXT 副本、用户明确外部导出 |
| 提醒 | [NanfengIslandState](../app/src/main/java/com/nanzhufeng/transcriber/domain/task/NanfengIslandState.kt)、[DisplayController](../app/src/main/java/com/nanzhufeng/transcriber/domain/task/IslandDisplayController.kt)、[Overlay](../app/src/main/java/com/nanzhufeng/transcriber/service/TaskIslandOverlay.kt) | 阶段/进度事实与用户显示模式分离 |
| 凭据/费用 | [CredentialStore](../app/src/main/java/com/nanzhufeng/transcriber/data/settings/SecureApiCredentialStore.kt)、[CostEstimator](../app/src/main/java/com/nanzhufeng/transcriber/domain/invocation/AsrCostEstimator.kt) | Key 密文存储；版本化 CNY 估算不是扣费回执 |

## 5. 真实数据流

```text
SAF / 分享 / 拖放
 → ViewModel 规范化输入、读取权限/必要时暂存、冻结任务参数
 → Room 队列 → 独立前台服务 → runner
 → 原文件可读与模型配置检查
 → 内嵌文字字幕探测（PREPARING 内部阶段）
    ├ 可读字幕 → 原时间轴分段 → 私有结果保存
    └ 未命中 → 本地资产或云端 session → 解码/恢复16kHz单声道PCM
              → 按模型推荐窗口转写 → 断点提交 → Room/通知/岛进度
 → 可读文字判定 → JSON 原子替换 + TXT写入 → Room COMPLETED
 → 历史打开/媒体核对/编辑草稿/复制
 → 用户明确导出 → SAF 目录冲突策略 / 系统保存入口
```

源码依据：[ViewModel](../app/src/main/java/com/nanzhufeng/transcriber/ui/TranscriptionViewModel.kt)、[runner](../app/src/main/java/com/nanzhufeng/transcriber/service/TranscriptionTaskRunner.kt)、[字幕提取器](../app/src/main/java/com/nanzhufeng/transcriber/data/media/EmbeddedSubtitleExtractor.kt)、[解码器](../app/src/main/java/com/nanzhufeng/transcriber/data/media/MediaCodecAudioDecoder.kt)。特别注意：runner 对云端模式的 Key 存在预检在字幕探测之前；“字幕无需本地模型”成立，但不能扩大成“选择云端且无 Key 时也能提字幕”。

状态主链为 `QUEUED → PREPARING → TRANSCRIBING → EXPORTING → COMPLETED`；EXPORTING 当前表示私有结果保存，不代表自动外部导出。`WAITING_INPUT / WAITING_MODEL / RECOVERY_REQUIRED / NO_SPEECH / FAILED / CANCELLED` 保留真实可恢复语义。COMPLETED 无隐式回退路径。[状态策略](../app/src/main/java/com/nanzhufeng/transcriber/domain/task/TranscriptionTaskState.kt)

## 6. 模型、音频与云端协议

本地默认 SenseVoice Small INT8 使用固定 revision 的 ONNX 和 tokens，两文件合计 237,431,441 字节，逐个 SHA 校验；CPU、16kHz、ITN 配置由本地引擎持有，不提供未经验证 GPU 开关。公开三档顺序为本地标准、高精度 Qwen、低成本 Paraformer；Whisper 保留内部研发代码但不在当前工厂的公开 provider 选择中。[模型目录](../app/src/main/java/com/nanzhufeng/transcriber/data/modelstore/OfficialModelCatalog.kt)、[SenseVoiceEngine](../app/src/main/java/com/nanzhufeng/transcriber/engine/SenseVoiceEngine.kt)

当前三档外层推荐块均为 30 秒；PCM coordinator 的无参默认仍为 5 分钟，但 runner 显式传入模型推荐值。SenseVoice 内部切片必须保留完整样本，长独立输入按持续停顿选择切点；样本覆盖率不是识别准确率。热词、时间戳等必须以模型 capability 为准，不能套用 Whisper 参数。[窗口策略](../app/src/main/java/com/nanzhufeng/transcriber/engine/SpeechPauseSegmenter.kt)、[窗口行为测试](../app/src/test/java/com/nanzhufeng/transcriber/engine/SpeechPauseSegmenterTest.kt)、[准确率策略](asr-accuracy-strategy.md)

新云端模型 ID 在代码中为 `qwen-audio-3.1-asr-flash-filetrans`、`paraformer-v2`；同一 Keystore 读取器供旧/新引擎使用。实际网络链为上传凭证 → WAV 到临时 OSS → 保存 submission_pending → 异步提交 → 保存 task ID → 轮询 → 匿名下载句级结果 → 完成缓存 → 外层断点。请求不跟随重定向，结果下载不带 Key，资源 URL 校验；不是只替换旧同步接口模型字符串。[File Client](../app/src/main/java/com/nanzhufeng/transcriber/engine/DashScopeFileTranscriptionClient.kt)、[File Engine](../app/src/main/java/com/nanzhufeng/transcriber/engine/DashScopeFileTranscriptionEngine.kt)

缓存身份包含音频、模型、源位置、语言及账号 Key 指纹，不保存明文 Key；提交响应不明时保留不确定状态并拒绝自动重提，结果404明确提示可能重新计费。取消当前只停止本地队列/请求，客户端没有远端取消 API，不能承诺远端任务或费用已撤销。新模型名称、账号权限、临时上传可用性及真实结果未在本轮联网验证，代码/Mock 不能证明供应商实际服务。[云端恢复测试](../app/src/test/java/com/nanzhufeng/transcriber/engine/DashScopeFileTranscriptionEngineTest.kt)

## 7. 持久化、迁移和数据保护

Room v7 有任务与文件级调用记录两表，显式迁移 1→7：冻结线程、输出目录/策略、旧后处理字段、调用账本、任务/源文件聚合字段、费用版本/币种/微元。Application 注册完整迁移并开启多实例失效通知；没有破坏性 fallback。schema 文件存在不等于本轮设备迁移执行通过。[数据库迁移](../app/src/main/java/com/nanzhufeng/transcriber/data/task/TranscriptionDatabase.kt)、[schema7](../app/schemas/com.nanzhufeng.transcriber.data.task.TranscriptionDatabase/7.json)

模型在 `noBackupFilesDir/models/`；任务 PCM/断点/结果在 `noBackupFilesDir/tasks/<taskId>/`。恢复同时校验 source/model/language/thread/PCM 元数据和长度；成功后删 PCM/断点，失败或取消保留已提交恢复资产。源码采用临时文件+移动，文件系统不支持 ATOMIC_MOVE 时退回普通替换，不能声称所有文件系统都有断电原子保证。[CheckpointStore](../app/src/main/java/com/nanzhufeng/transcriber/data/result/TranscriptionCheckpointStore.kt)、[runner](../app/src/main/java/com/nanzhufeng/transcriber/service/TranscriptionTaskRunner.kt)

Key 在 Keystore AES-GCM 加密的 SharedPreferences，备份规则排除其文件和数据库；`hasApiKey()` 只判断密文/IV 存在，不证明可解密或账号有效。任务删除先停止活动执行，再删除任务私有目录与记录，不删除原媒体和已导出文件；历史批删只处理完成项。文件与数据库删除不是同一跨介质事务，异常一致性需另测。[凭据](../app/src/main/java/com/nanzhufeng/transcriber/data/settings/SecureApiCredentialStore.kt)、[备份规则](../app/src/main/res/xml/data_extraction_rules.xml)、[删除路径](../app/src/main/java/com/nanzhufeng/transcriber/ui/TranscriptionViewModel.kt)

JSON 原子提交，TXT 是直接输出流写入；二者完成后才置 COMPLETED。编辑正文是 ViewModel 草稿，TXT/MD/DOCX 导出用草稿，SRT 用原时间轴；未发现该编辑动作写回私有 JSON，不能宣称重启保留编辑结果。[结果存储](../app/src/main/java/com/nanzhufeng/transcriber/data/result/TranscriptDocumentStore.kt)、[编辑和导出](../app/src/main/java/com/nanzhufeng/transcriber/ui/TranscriptionViewModel.kt)、[导出测试](../app/src/test/java/com/nanzhufeng/transcriber/domain/export/TranscriptExportServiceTest.kt)

## 8. 后台与交互

直接前台服务而不是 WorkManager 承担主转写；Manifest 使用独立进程、stopWithTask=false。API35 分支选择 mediaProcessing，较低系统选择 dataSync；资源由队列 finally 与平台退出路径释放，只能终止自身工作进程。唤醒锁为30分钟有界租约、10分钟续期，监测不等于绕过 OEM/平台限制。[服务](../app/src/main/java/com/nanzhufeng/transcriber/service/TranscriptionForegroundService.kt)、[生命周期测试](../app/src/test/java/com/nanzhufeng/transcriber/service/BackgroundTranscriptionQueueTest.kt)

通知、悬浮窗共用 NanfengIslandState，已知进度在结果提交前至多99%；未知不伪造百分比。简洁岛一行，点击展开，用户模式不随进度变化，新任务/回前台后恢复简洁；220dp宽、24dp边距、34dp可见/48dp触控是本项目数值，不是跨平台固定标准。Room 绑定的 importance125 不代表 Activity 可见；设置背景后恢复 padding 是实际几何修复。[后台合同及实测](background-island.md)、[可见策略测试](../app/src/test/java/com/nanzhufeng/transcriber/domain/task/MainScreenVisibilityPolicyTest.kt)

内外屏统一底部白色浮动导航，根路由与视频会话使用 rememberSaveable；六皮肤只换 token。关于可复制，GitHub 橙色下划线真实链接；设置已移除隐私诊断卡与岛四段小字，保留三个权限入口。布局、折叠和字体可访问性不因源码存在而自动通过。[UI 合同](ui-contract.md)、[导航实测](qa/navigation-v0146/README.md)、[主题](../app/src/main/java/com/nanzhufeng/transcriber/ui/theme/Theme.kt)

## 9. 关键决策、原因及替代方案

| 决策 | 原因 / 取舍 | 证据与现状 |
| --- | --- | --- |
| 原生 Kotlin/Compose + JNI | 需要 Android 生命周期、SAF、后台和折叠屏控制；不封装 Windows UI | [决策记录](decision-log.md) 2026-07-20；当前源码成立 |
| no-backup 模型与校验提交 | 覆盖长期复用，避免云备份巨型权重；换机靠明确导入导出 | [模型测试](../app/src/test/java/com/nanzhufeng/transcriber/data/modelstore/ModelAssetManagerTest.kt) |
| 独立进程/直接FGS | UI 与原生模型内存/崩溃隔离；仍接受平台终止边界 | [历史退出证据](evidence/find-n5-v6-worker-exit.txt)、[服务](../app/src/main/java/com/nanzhufeng/transcriber/service/TranscriptionForegroundService.kt) |
| 块断点、队列热会话 | 被系统杀后从头跑代价高；磁盘缓存与内存会话分责 | [历史失败与修复](evidence/README.md)、[会话测试](../app/src/test/java/com/nanzhufeng/transcriber/engine/QueueModelSessionOwnerTest.kt) |
| 字幕先于本地模型 | 有文字轨不必下载/推理；不是硬字幕OCR | [v15真实字幕验收](qa/final-regression-v15/README.md) |
| 私有结果先提交、外部明确导出 | 自动落盘与用户导出操作混淆；外部权限失败不丢结果 | Git `6b920b1`、[2026-09-11决策](decision-log.md) |
| 单模型ASR，停止自动后处理 | 用户要所选引擎直接结果，避免额外上传/费用/改写 | Git `ceaebf3`、[架构边界](architecture-governance.md) |
| 新文件异步接口兼容旧任务 | 新模型不能沿用同步Base64；保留旧ID恢复，不重写历史 | Git `290e03f`、[云端策略](asr-accuracy-strategy.md) |
| 临时上传不用新增后端 | 当前个人客户端直接复用Key；规模化/生产限制未解除 | [已有策略记录](asr-accuracy-strategy.md)，不是商业生产验收 |
| 双密度岛/底部统一导航 | 默认提示减少遮挡，点击才看细节；折叠保持路由 | Git `290e03f`、[后台](background-island.md)、[导航](qa/navigation-v0146/README.md) |

## 10. 开发过程与 Git 可还原边界

全部可达提交的完整 SHA/日期/变更文件数见 [历史索引](qa/project-review-20261011/inventory.md)。Git 最早是 2026-07-21 的 v15 checkpoint，因此7月20日的早期阶段来自仓库内证据/决策，不伪造更细提交；一个聚合发布包含多个增量，不按文档日期杜撰逐个commit。

| 阶段 | 能确认的演进 |
| --- | --- |
| 2026-07-20（文档证据） | 工程/JNI与真实短样本、正式签名、缓存、三页UI、独立进程；60分钟Base压力在约77%被系统终止，随后引入分块恢复；不是长测成功 |
| 2026-07-21 `59ecf86` → `42ae0d2` | v15源码冻结与交接；字幕直提、输出、六皮肤等初始综合基线 |
| 2026-07-21 `aa5cb64` → `78c7947` | 未完成/暂停和预发布可见性的文档演进，不当作新增业务实现 |
| 2026-08-30 `ceaebf3`、`990f23f`、`2436f69` | SenseVoice/历史导出体验合并与预览；旧文字后处理废止 |
| 2026-08-31 `cca8754`、09-01 `dc89a5b` | 0.14.1/0.14.2正式发布聚合提交；详细归因以差异和对应QA为准 |
| 2026-09-11 `6b920b1`、09-15 `b02799b` | 明确导出/UI优化、关于信息 |
| 2026-10-11 `290e03f` | 0.14.8聚合：云端文件接口、本地完整窗口、导航、后台岛及设置精简；中间0.14.5/6/7文档不能冒充独立提交 |
| 2026-10-11 `5152a3d`、`2bad9c2` | 正式Release回读、合同纠漂、91项回归和增量沉淀；无App变化 |
| 本次未提交文档增量 | 新完整档案、五类项目Skill、长期AGENTS和冲突纠正；业务代码不变 |

## 11. 踩坑、修复与防回归

| 真实问题 | 原因 / 修复 | 可追溯验证 / 不可外推 |
| --- | --- | --- |
| 60分钟任务在约77%中断，从头重跑 | OEM/系统杀工作进程；持久PCM和匹配块断点 | [历史失败](evidence/README.md)保留失败；不等于当前2小时门禁通过 |
| Range被忽略导致续传可能重复字节 | 完整200响应必须重写临时文件 | [模型传输测试](../app/src/test/java/com/nanzhufeng/transcriber/data/modelstore/ModelAssetManagerTest.kt) |
| 静音裁剪丢低音量/短音节、短句碎裂 | 原阈值裁剪；改为完整连续窗口 | [窗口测试](../app/src/test/java/com/nanzhufeng/transcriber/engine/SpeechPauseSegmenterTest.kt)、[P0记录](asr-accuracy-strategy.md)；没有CER提升证明 |
| 正文大结果读取白屏等待 | 完整读取在IO，列表读取短TXT预览；损坏保留错误 | [ViewModel](../app/src/main/java/com/nanzhufeng/transcriber/ui/TranscriptionViewModel.kt)；当前无大结果首帧耗时基准，不能承诺零加载 |
| 未点导出已出现TXT | 私有副本与外部导出混用；完成时不再写SAF用户目录 | Git `6b920b1`、[领域规则](domain-rules.md) |
| 进度100%先于保存 | 处理比例不等于结果提交；ongoing封顶99 | [通知策略测试](../app/src/test/java/com/nanzhufeng/transcriber/domain/task/TaskNotificationPresentationPolicyTest.kt) |
| 后台岛不出现 | Room绑定提升主进程importance125；改成区分Activity可见 | [可见策略测试](../app/src/test/java/com/nanzhufeng/transcriber/domain/task/MainScreenVisibilityPolicyTest.kt)、[实际后台](background-island.md) |
| 胶囊变宽仍贴边 | Drawable赋值重置padding；背景后恢复边距 | [Overlay](../app/src/main/java/com/nanzhufeng/transcriber/service/TaskIslandOverlay.kt)、版本化几何证据 |
| 云端响应丢失可能重复付费 | 先保存提交意图与远端ID；不确定时阻止重发 | [恢复测试](../app/src/test/java/com/nanzhufeng/transcriber/engine/DashScopeFileTranscriptionEngineTest.kt)，不是服务端结算证明 |
| GitHub上传长期低速 | 精确停止本轮上传，单次HTTP/1.1重传并回读digest | [发布记录](releases/v0.14.8-verification.md)；只证明 workaround，不证明HTTP/2根因 |
| 合同仍写旧档位/schema/导航 | 文档与实现没有同步；源码对照门禁 | [最终回归](qa/final-regression-v0148/README.md)：旧快照10项红、新56项绿 |

## 12. 测试验证与本次结果

本次执行 `testDebugUnitTest lintDebug --quiet` 成功，读取28套JUnit报告共91 tests，failures/errors/skipped均0；Gradle可复用UP-TO-DATE步骤，不宣称全部任务强制重算。合同脚本以 final checkpoint 为 baseline、冻结APK和JUnit为输入，56检查PASS。未重建Release、未安装或启动设备、未调用付费API、未执行仪器测试。[本次复盘验证](qa/project-review-20261011/README.md)

测试覆盖状态转换、模型续传/哈希/备份往返、重采样与字幕解析、结果/断点、导出格式、云端Mock及恢复、文件级费用估算、完整样本窗口、会话复用、生命周期清理、通知/岛策略、预览与UI资格。旧后处理测试仍存在，只证明兼容代码，不意味着当前产品重新启用。[全量测试索引](qa/project-review-20261011/inventory.md)、[测试源码](../app/src/test/java/com/nanzhufeng/transcriber)

历史证据分别保留：v15模拟器字幕四格式/复制；v0.14.6内外屏导航；v0.14.7真实本地33分钟样本桌面/熄屏推进与取消清理；v0.14.8设置/主页预览和主设备签名覆盖。历史仪器报告只作过去事实，当前规则永久禁止重跑。真实百炼、当前OPPO完整转写、长时温控/内存和独立准确率均不因这些报告通过。[v15](qa/final-regression-v15/README.md)、[后台](background-island.md)、[Release](releases/v0.14.8-verification.md)

## 13. 构建、部署和交付

开发需 Android SDK/JDK17兼容环境；本机可使用 Android Studio JBR。获取工程后初始化固定submodule，再按任务选择 JVM/lint 或构建。禁止把文档复核自动扩展为设备部署。正常可用命令为 `./gradlew testDebugUnitTest lintDebug`；有构建需求时再运行 assembleDebug/assembleRelease。[构建配置](../app/build.gradle.kts)、[CMake](../app/src/main/cpp/CMakeLists.txt)

正式凭据实际为三项环境变量 `NANFENG_TRANSCRIBER_KEYSTORE`、`NANFENG_TRANSCRIBER_KEYSTORE_PASSWORD`、`NANFENG_TRANSCRIBER_KEY_PASSWORD`；备用属性为 `nanfengTranscriber.keystore/storePassword/keyPassword`。没有完整凭据时停止；Gradle不直接读macOS钥匙串。不得运行签名复制辅助脚本来为纯复盘读取口令。[已纠正签名文档](signing-and-device-acceptance.md)、[App构建](../app/build.gradle.kts)

本地产物直接为 `app/build/outputs/apk/release/南枫转写.apk`；已冻结0.14.8包SHA-256 `4227c045231961f52c5099443a35a38ea7c2220b7dbd0b164c5e9d97ee67ee30`。既有公开Release附APK和校验文件，仓库报告记录其digest匹配；本轮不联网重新确认远端，也不重新生成包。签名、版本、数据保留、安装、启动、业务、发布各有独立门槛。[发布验证](releases/v0.14.8-verification.md)

只有用户另行授权覆盖，才核对本地/现装包身份、签名、非Debug、首装与数据标识，再 push 精确设备临时文件、`pm install -r --user 0`、回读包验证并清临时文件；不卸载、不清数据、不自动启动。这个流程不授权真实文件处理、改权限或付费服务。[签名门禁](signing-and-device-acceptance.md)、[长期规则](../AGENTS.md)

## 14. 文档/源码冲突台账

| 冲突位置 | 当前事实 / 影响 | 本轮处理 |
| --- | --- | --- |
| 原AGENTS Windows真相、推理前不能做六皮肤 | 独立Android已六皮肤且默认模型演进；旧阶段门禁不是永久规则 | 重写长期AGENTS，阶段历史留本档案 |
| signing文档自动读Keychain、两项凭据 | Gradle只读环境/属性，Release还要求keyPassword | 同步签名文档，不读真实凭据 |
| domain私有JSON/TXT均原子保存 | DocumentStore JSON临时替换；runner TXT直接输出 | 更正文档，部分TXT/断电风险保留；不改代码 |
| asr策略“Paraformer只作研发对照” | 目录中paraformer-v2已公开第三档 | 区分云端公开模式与其他本地对照 |
| design-reference设置仍列隐私诊断 | Settings已删除，新增灵动岛/关于 | 更新适配结论，旧来源日期保留 |
| product-acceptance“历史及当前均不运行仪器” | v15报告真实记录8/8，当前永久禁止 | 改为历史记录不删除、当前禁止不重跑 |
| File Client注释“never uploads” | 同文件实际有uploadAudio | 记录过时注释；本轮不改业务源码 |
| Catalog注释“experimental候选达标后替换” | 该列表的single已作为公开默认 | 记录注释/命名债务，不能当未展示候选 |
| 旧portable-rules“不另建Skill” | 旧增量当时未创建；本轮用户明确要求五类项目Skill | 旧记录标注历史并路由新资产，不修改全局Skill |
| 旧PROJECT_STATUS待本地提交 | 当前HEAD已含2bad9c2，只有新复盘资产待提交 | 更新动态摘要，源码基线保持可复核 |

本次遵循实际代码纠正文档，不借“同步”名义改变运行时规则。新补充解释不能倒灌历史测试结果。[相关源码与历史全部索引](qa/project-review-20261011/inventory.md)

## 15. 已知问题、证据缺口与后续路线

| 项目 | 已确认事实 / 待确认 | 下一步与完成条件（建议，不是已授权任务） |
| --- | --- | --- |
| 新云端模型可用性 | 代码ID/Mock成立；真实账号权限、响应/用量未知 | 获授权和费用上限后，用短公开样本记录上传→提交→结果→账本与服务回执 |
| 取消与费用 | 没有远端取消调用，本地停止不等于免计费 | 明确产品语义和供应商支持，再决定是否实现/测试远端取消 |
| 后台长测 | 33分钟重复样本推进成立，不是当前OPPO长测 | 授权后测目标真机长文件、温度/PSS、系统中断恢复；租约续期与超时另证 |
| 准确率 | 完整窗口保样本；没有独立Ground Truth A/B | 固定原片/PCM/模型/参数，人工标注CER/WER/术语和耗时，单变量比较 |
| 模型许可 | 精确ONNX制品许可证未归档 | 获取匹配文本或换明确许可制品；未过门禁不分发权重/宣称商业许可完备 |
| 编辑保留 | 草稿未写回JSON，SRT仍原分段 | 先明确是否要持久修订/时间轴编辑，再设计可撤销协议 |
| 大结果首帧 | IO读取/短预览存在；没有最新大文件性能报告 | 用受控大结果测点击→首帧/内存与错误态，不能先承诺“进来零等待” |
| 数据一致性 | JSON与TXT/Room非一个事务；删除文件/DB非跨介质事务 | 隔离故障注入验证部分写入/删除失败，不碰主设备数据 |
| 架构维护 | ViewModel1528行，History1252、Home1061、Settings960行（快照） | 只在触及职责时增量抽取受测seam，不为档案全量重构 |
| CI/二进制 | 未见跟踪workflow，JNI为预编译依赖 | 如需供应链/CI治理再单独定义目标与授权，不冒充现成门禁 |

依据分别是 [策略](asr-accuracy-strategy.md)、[许可](model-license-status.md)、[后台](background-island.md)、[源码索引](qa/project-review-20261011/inventory.md)、[状态](PROJECT_STATUS.md)。优先级建议：先补真实服务/准确率/目标设备的核心验收，再做编辑协议与故障一致性，最后按实际触发拆职责；不存在已确认的发布日期或新功能承诺。

## 16. 长期资产与维护方式

长期规则见 [AGENTS](../AGENTS.md)，开发/排错/测试/审查/文档同步各自一个项目Skill；通用原则与专属/不可照搬内容见 [可迁移开发经验](可迁移开发经验.md)。本档案整合历史而不删除旧QA；近期增量矩阵和机器合同检查继续保留：[增量审计](development-experience/audit-20261011.md)、[合同检查](../scripts/verify_contract_checkpoint.rb)。

本次产物只在项目内落盘；不修改全球Skill、长期记忆、设备、账号或远端。文件索引脚本只读且按当前Git跟踪集合运行，新文档尚未提交时不自动冒充已提交基准；生成时间与未来仓库状态可以不同，复核必须注明范围。
