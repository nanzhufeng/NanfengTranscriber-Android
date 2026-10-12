---
name: nanfeng-transcriber-debugging
description: Diagnose NanfengTranscriber-Android transcription, history, cache, export and background faults using reproducible evidence; diagnosis alone does not authorize fixes.
---

# 南枫转写排错

先读根契约，确认失败版本、实际输入、前后台状态和可观察红灯。诊断请求只查因，不自动实现修复。

## 定位路径

- 等待/失败/恢复：追踪Room任务state/errorCode → runner实际分支；区分源权限、模型检查、API Key、解码、推理和私有保存。
- 模型不就绪/重复下载：对照OfficialModelCatalog制品和FileModelStore长度/哈希/part/校验记录，查Range重写与串行协调；不清缓存充当修复。
- 转写丢字/质量：固定PCM、模型与参数；查decoder → coordinator → SenseVoiceEngine → SpeechPauseSegmenter；样本覆盖与CER分开，不先叠去重/裁剪规则。
- 历史白屏/丢结果：查outputUri/JSON/TXT、IO读取、短预览、请求taskId与UI状态；不隐藏加载提示冒充恢复完整结果。
- 后台岛：查实际Room进度、NanfengIslandState、限频、权限/锁屏、Activity可见策略、Drawable后的padding；显示失败与ASR停止分别诊断。
- 云端：查提交意图、远端ID、完成缓存和安全errorCode；未知提交不得重发。取消本地不推断远端已取消或免费。

依据：[runner](../../../app/src/main/java/com/nanzhufeng/transcriber/service/TranscriptionTaskRunner.kt)、[后台合同](../../../docs/background-island.md)、[准确率策略](../../../docs/asr-accuracy-strategy.md)、[已知限制](../../../docs/南枫转写Android完整开发档案.md)。

## 验证与停止

1. 保存脱敏最小输入身份/失败信号，不输出Key、私有音频或全量日志。
2. 在首个可证伪分叉建立最小实验；获修复授权后才改代码，并重跑同一红灯及受影响入口。
3. 两次最小尝试无效，回查所有权和生命周期，不继续堆条件。
4. 无真实服务授权则使用Mock/既有报告并明确边界；禁止仪器测试、清数据及自动操作主设备。结论分已确认原因、推测与待验证，不将无法复现写成修复成功。
