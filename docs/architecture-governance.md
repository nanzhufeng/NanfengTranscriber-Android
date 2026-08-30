# 架构治理

## 分层

- `ui`：Compose 页面、皮肤 token 与折叠屏布局，只消费领域状态。
- `domain`：任务状态机、模型状态机、导出与重试规则。
- `data`：Room 任务真相、模型仓库、SAF 与安全凭据。
- `engine`：统一 `SpeechEngine` 接口、会话复用、音频预处理、sherpa-onnx / whisper.cpp 本地引擎与 Qwen3-ASR API 引擎。
- `service`：唯一媒体处理前台服务和任务执行器；Room 是跨进程任务真相层。

## 模型所有权

| 概念 | 唯一所有者 | 约束 |
| --- | --- | --- |
| 本地模型安装、校验、导入导出 | `ModelAssetManager` + `FileModelStore` | 仅 SenseVoice/Whisper；UI 不自行判断文件可用性 |
| 本地会话 | `ModelSessionManager` + `QueueModelSessionOwner` | 同模型/线程热复用，队列结束释放 |
| Qwen 高精度转写 | `Qwen3AsrApiEngine` | 固定千问端点与 Qwen3-ASR 模型，直接发送音频分段 |
| 千问 API Key | `SecureApiCredentialStore` | Android Keystore AES-GCM；不进任务、结果、日志或备份 |
| 任务与结果 | Room + `TranscriptDocumentStore` | 结果落盘后才可完成；历史是唯一完成记录 |

## 不可破坏边界

- Qwen 是模型选择之一，不得在本地转写完成后调用它润色、翻译或覆盖结果。
- UI 不展示服务地址或自由模型名称；模型单选切换立即生效，不弹确认框。Qwen 的音频外发与潜在费用固定显示在设置和隐私说明中。
- UI 不直接调用 JNI、网络下载或 DAO；下载与 SAF 导入必须汇入 `ModelAssetManager`。
- 本地模型在 `noBackupFilesDir/models/`，仅经长度和 SHA-256 校验后进入可用状态。
- 旧 Room 后处理字段只为读取历史任务保留，执行器不再运行后处理。
- 每个 PCM 分块完成后先原子提交断点，再更新 Room/通知；源、模型、语言、线程和 PCM 长度不一致时禁止恢复。

## 持久化边界

- 任务 PCM 与断点：`noBackupFilesDir/tasks/<taskId>/`；成功后清理，系统中断时保留。
- 完整结果：`transcript.json` 为真相，`transcript.txt` 为便捷副本。
- API Key：`secure-api-credentials.xml` 中仅保存密文，云备份和设备迁移均显式排除。
- 原生依赖必须固定版本并保留许可证；模型可按清单下载，但不可下载可执行代码。
