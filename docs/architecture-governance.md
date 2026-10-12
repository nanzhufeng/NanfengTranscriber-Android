# 架构治理

本文件维护实际所有权与依赖；业务语义见 [领域规则](domain-rules.md)，全部职责见 [合同索引](contracts-index.md)。边界目标不代表既有代码已经完全符合。

## 分层

- `ui`：Compose 页面、皮肤 token 与折叠屏布局，只消费领域状态。
- `domain`：任务状态机、模型状态机、导出与重试规则。
- `data`：Room 任务真相、模型仓库、SAF 与安全凭据。
- `engine`：统一 `SpeechEngine` 接口、会话复用、完整音频窗口、SenseVoice 本地引擎与 DashScope 文件转写；whisper.cpp / 旧 Qwen3 仅保留内部兼容能力。
- `service`：唯一媒体处理前台服务和任务执行器；Room 是跨进程任务真相层。

## 模型所有权

| 概念 | 唯一所有者 | 约束 |
| --- | --- | --- |
| 本地模型安装、校验、导入导出 | `ModelAssetManager` + `FileModelStore` | 当前公开目录是SenseVoice；Whisper为内部研发能力，不等于当前可安装档位 |
| 推理会话 | `ModelSessionManager` + `QueueModelSessionOwner` | provider/真实路径/线程匹配才热复用，队列结束释放 |
| 新云端转写 | `DashScopeFileTranscriptionClient` + `DashScopeFileTranscriptionEngine` | 固定百炼预设、临时上传、远端任务恢复，真实服务独立验收 |
| 旧 Qwen3 任务 | `Qwen3AsrApiEngine` | 仅兼容已创建任务，不作为新选择 |
| 千问 API Key | `SecureApiCredentialStore` | Android Keystore AES-GCM；不进任务、结果、日志或备份 |
| 任务与结果 | Room + `TranscriptDocumentStore` | 结果落盘后才可完成；历史是唯一完成记录 |
| 后台提醒 | `NanfengIslandState` + `TaskProgressUpdateGate` | 通知与悬浮窗同一快照，不另算进度 |
| 灵动岛显示模式 | `IslandDisplayController` | 用户点击切换，新任务回到简洁；进度不自行展开 |
| 队列释放 | `BackgroundTranscriptionQueue` | 完成、异常与取消都走 finally，不能以文案代替释放 |
| 正文编辑草稿 | `TranscriptionViewModel.updateTranscriptDraft` | 仅内存；不是JSON持久修订 |
| 历史搜索 | `HistoryScreen` + ViewModel短预览 | 文件名/摘要匹配，不是全文数据库索引 |
| 外部明确导出 | `AndroidTranscriptOutputStore` + `TranscriptExportService` | 直接写SAF目标目录，返回Written或Skipped |

## 不可破坏边界

- Qwen 是模型选择之一，不得在本地转写完成后调用它润色、翻译或覆盖结果。
- UI 不展示服务地址或自由模型名称；模型单选立即生效。云端模式的音频外发与费用说明保留在对应模型配置区域，不依赖已移除的隐私卡片。
- Compose页面不直接调用JNI、下载或DAO；下载/模型SAF导入汇入ModelAssetManager。现有ViewModel承担平台输入、历史文件清理、结果读取/草稿和导出协调；这是真实职责，不能宣称已完全拆成纯领域用例。
- 本地模型在 `noBackupFilesDir/models/`，仅经长度和 SHA-256 校验后进入可用状态。
- 旧 Room 后处理字段只为读取历史任务保留，执行器不再运行后处理。
- 每个 PCM 分块完成后先原子提交断点，再更新 Room/通知；源、模型、语言、线程和 PCM 长度不一致时禁止恢复。

## 持久化边界

- 任务 PCM 与断点：`noBackupFilesDir/tasks/<taskId>/`；成功后清理，系统中断时保留。
- 完整结果：`transcript.json` 为真相，`transcript.txt` 为便捷副本。
- API Key：`secure-api-credentials.xml` 中仅保存密文，云备份和设备迁移均显式排除。
- 原生依赖必须固定版本并保留许可证；模型可按清单下载，但不可下载可执行代码。

## 不改变功能的维护边界

旧文字后处理实现与测试、Whisper JNI、Room兼容字段目前保留，不能因未公开而在合同整理中直接删除。ViewModel及页面大文件、跨文件/数据库非事务删除和TXT直接写入都是已记录技术债；是否拆分须由后续具体改动和回归范围决定。源码中“never uploads”等旧注释不作为行为事实。云端远端取消、全文搜索和编辑持久化缺口只记录，不偷偷新增能力。
