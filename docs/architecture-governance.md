# 架构治理

## 分层

- `ui`：Compose 页面、六皮肤 token、折叠屏布局，只消费领域状态。
- `domain`：任务状态机、模型状态机、导出与重试规则，不依赖 Android UI。
- `data`：Room 任务真相层、模型仓库、SAF 权限与文件导入导出。
- `engine`：统一 `SpeechEngine` 接口、会话复用、音频预处理和 whisper.cpp JNI。
- `service`：唯一的媒体处理前台服务和任务执行器；Room 是跨进程任务真相层，UI 只发出启动、恢复或取消请求。

## 业务所有权

| 概念 | 唯一所有者 | 公开入口 | 消费者 |
| --- | --- | --- | --- |
| 模型资产安装、校验、导入导出 | `ModelAssetManager` | `download`、`importModel`、`exportModel` | 模型页、后台下载、推理会话 |
| 模型文件与校验元数据 | `FileModelStore` | `inspect`、原子提交 | `ModelAssetManager`、`ModelSessionManager` |
| 大模型内存会话 | `ModelSessionManager` | `acquire`、`releaseCurrent` | 转写任务用例 |
| 队列级热会话 | `QueueModelSessionOwner` | 按模型与线程获取、队列结束释放 | `TranscriptionTaskRunner` |
| 转写任务事实 | Room 任务仓库 | `TranscriptionTaskRunner`、状态转换接口 | 页面、通知、恢复、取消、导出 |
| 后台转写生命周期 | `TranscriptionForegroundService` | 带任务 ID 的显式 Intent | UI、系统通知、恢复入口 |
| 分块恢复断点 | `TranscriptionCheckpointStore` | 原子保存、兼容性校验、恢复读取 | `TranscriptionTaskRunner`、`PcmTranscriptionCoordinator` |
| 完整转写结果 | `TranscriptDocumentStore` | 原子写入 `transcript.json` / `transcript.txt` | 历史详情、TXT/MD/SRT/DOCX 再导出 |
| 用户目录自动导出 | `AndroidTranscriptOutputStore` | SAF 建目录、冲突策略、格式写入 | `TranscriptionTaskRunner` |
| 翻译润色密钥 | `SecureApiCredentialStore` | Android Keystore AES-GCM 保存、读取、删除 | 设置页、任务执行器 |
| 兼容文字后处理 | `OpenAiCompatibleTextPostProcessor` | HTTPS `/chat/completions`、分块、中文错误 | `TranscriptionTaskRunner` |

## 不可破坏边界

- UI 不直接调用 JNI、网络下载或数据库 DAO。
- 下载与 SAF 导入必须汇入 `ModelAssetManager`，不能各自写正式模型文件。
- JNI 不决定产品状态；它只返回可诊断的引擎结果。
- 同一时刻最多一个大型模型驻留内存，模型切换通过 `ModelSessionManager` 管理。
- 原生推理只在 `:transcription` 独立进程运行；同一批队列结束、超时或取消后释放会话，避免每项重复加载，也避免长期占用模型内存。
- 开始执行时必须把语言、线程数等运行参数冻结进 Room 任务，后台进程不得依赖 UI 进程的瞬时状态。
- 每个 5 分钟 PCM 分块完成后必须先原子提交断点，再更新 Room/通知进度；系统杀进程后最多重做当前未提交分块。
- 断点只有在源文件、模型、语言、线程数和 PCM 长度全部一致时才可复用；不一致或损坏时重新准备音轨。
- 任务完成前必须先保证对应导出文件已经成功落盘。
- 润色失败不能覆盖或删除本地原始转写。
- API Key 不得写入任务、结果、技术详情或日志；任务只冻结启用状态、服务地址和模型名。

## 持久化位置

- 模型：`noBackupFilesDir/models/<modelId>/<version>/`，避免进入 Android 自动备份。
- 任务 PCM 与恢复断点：`noBackupFilesDir/tasks/<taskId>/decoded.pcm` 和 `checkpoint.json`；成功后删除，失败或系统中断时保留供恢复。
- 任务与产物索引：Room v4；开启多进程失效通知，v1→v2→v3→v4 显式迁移保留已有任务、历史和模型缓存。v3 冻结每项输出目录与同名冲突策略，v4 冻结可选文字后处理参数但不保存密钥。
- 完整结果：`noBackupFilesDir/tasks/<taskId>/transcript.json`，JSON 为真相文件，TXT 为便捷副本。
- 用户选择和皮肤：DataStore。
- 翻译润色 API Key：Android Keystore 密钥 + `secure-api-credentials.xml` 密文；云备份和设备迁移规则均显式排除。

## 依赖策略

原生依赖必须固定版本或提交，并保留许可证和来源。禁止运行时下载可执行代码。模型文件可以按显式清单下载，但必须校验长度和 SHA-256。
