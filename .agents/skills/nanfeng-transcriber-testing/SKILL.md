---
name: nanfeng-transcriber-testing
description: Select and run proportionate JVM, lint, contract and isolated acceptance checks for NanfengTranscriber-Android without instrumentation or unauthorized device and paid-service operations.
---

# 南枫转写测试

版本/数量取当前报告，不将档案快照中的91/56变成固定门槛。先核对根契约和工作区范围。

## 最小充分验证

| 改动 | 优先检查 |
| --- | --- |
| 模型传输/缓存 | FileModelStoreTest、ModelAssetManagerTest；Range忽略、同长损坏、备份往返 |
| 任务/后台/岛 | TaskTransitionPolicyTest、BackgroundTranscriptionQueueTest、NanfengIslandStateTest、MainScreenVisibilityPolicyTest、IslandDisplayControllerTest |
| 音频/准确率 | StreamingMonoResamplerTest、SpeechPauseSegmenterTest、AsrBenchmarkEvaluatorTest；真实质量另需Ground Truth |
| 云端/费用 | DashScopeFileTranscriptionClientTest/EngineTest、AsrCostEstimatorTest；Mock不证明实际模型/账号/计费 |
| 结果/导出 | TranscriptDocumentStoreTest、TranscriptionCheckpointStoreTest、TranscriptExportServiceTest；实际可读文件另验 |
| 文档/规则 | 合同脚本、链接/Skill校验、源码基线差异；不为文档重打冻结Release |

在仓库根运行，可用 `./gradlew testDebugUnitTest --tests '完整测试类名'` 定向；交付按影响运行 `./gradlew testDebugUnitTest lintDebug`。JDK需17兼容；本机Android Studio JBR路径是环境选项，不要求其他电脑相同路径。若构建确有需求，才assemble对应变体；正式签名缺失停止，不打印凭据。

合同命令：`ruby scripts/verify_contract_checkpoint.rb`。仅在本轮保证业务未变时加入 `--baseline <已核对commit/tag>`；只有存在需复核的冻结包/JUnit报告才附 `--apk <路径> --unit-results <目录>`，不凭空填参数。脚本正文是 [唯一实现](../../../scripts/verify_contract_checkpoint.rb)。

## 验收边界

永久禁止任何connected Android仪器测试及绕过runner；历史androidTest只读。隔离模拟器的手工真实链路需独立记录版本、输入与输出，不能当主设备；覆盖安装/权限/真实云端收费均需明确授权。本次JUnit、lint、构建、设备、服务分别报告PASS/FAIL/未运行，并说明UP-TO-DATE缓存；[历史验证与缺口](../../../docs/PROJECT_STATUS.md)仅用于路由。
