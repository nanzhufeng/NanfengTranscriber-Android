---
name: nanfeng-transcriber-code-review
description: Review NanfengTranscriber-Android changes for task truth, data preservation, cloud retry costs, lifecycle, compatibility and delivery risks; do not implement changes unless asked.
---

# 南枫转写代码审查

先确认审查基线/范围，Git先stat再限路径差异；保留WIP。读根契约和 [所有权](../../../docs/architecture-governance.md)，审查不自动改实现或触碰远端。

## 风险顺序

1. 数据：迁移是否完整注册/schema一致，删除范围是否限制任务私有资产；结果提交与Room完成是否倒置，失败是否留下可恢复事实。
2. 云端：Key是否外泄，上传/结果URL与重定向是否受控；是否保存提交意图/远端ID，未知重试会否重复付费，实际用量缺失是否伪造费用；本地取消是否被误报远端取消。
3. 后台：FGS启动/类型与独立进程、取消/异常/finally清理、late callback、bounded锁与平台超时；importance不能当Activity可见。
4. 音频：分块是否覆盖完整PCM，绝对时间戳/恢复键是否匹配，NO_SPEECH是否误归完成，模型capability是否被绕过。
5. UI/入口：字幕预检是否被前置本地模型门禁挡住；草稿、原结果、SRT时间轴是否混写；分享/文件/历史/通知是否同一所有者，折叠是否保存路由。
6. 交付：App版本、签名、包hash与README/Release证据是否对应；冻结后重复构建、主设备Debug/清数据、仪器测试均不能越界。

依据：[完整档案冲突与问题表](../../../docs/南枫转写Android完整开发档案.md)、[状态策略](../../../app/src/main/java/com/nanzhufeng/transcriber/domain/task/TranscriptionTaskState.kt)、[云端引擎](../../../app/src/main/java/com/nanzhufeng/transcriber/engine/DashScopeFileTranscriptionEngine.kt)。

## 输出

仅报告有定位依据、影响与触发条件的可行动问题，按风险排序，引用实际文件/行和可验证信号；区分确认缺陷、设计取舍、未知。注明运行/未运行的测试；不以静态审查宣称真机安全或远端服务通过。无确定问题也保留验证缺口，不编造“零风险”。
