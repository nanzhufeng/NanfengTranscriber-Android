# Project Status

> 当前状态摘要；代码与适用验证证据优先，不把模型预设、接口 Mock 或覆盖安装当作真实云端/真机业务验收。

## Verified

- Last verified: 2026-10-11 14:13 +08:00
- Verified branch: main
- Code baseline: 5152a3d73626c561124c2fb6aa4546fc5d6497d5
- Verification scope: 生产代码 COMMIT；与 v0.14.8 标签生产代码相同。本轮文档/开发检查为 WORKTREE 增量，将独立本地提交；见 [最终回归](qa/final-regression-v0148/README.md)。不把本轮文档验收升级成设备重跑。
- Verified by: Codex
- Status: Active

## Current

- 0.14.8 (23) 正式签名 APK：本地 SenseVoice、云端文件转写预设与 Paraformer 低成本模式、队列/历史/明确导出；云端接入通过接口测试，真实账号服务未验收。
- 后台独立服务、已有片段断点、真实进度通知与点击展开的简洁灵动岛；设置仅保留授权状态和三个权限入口。
- 主设备已按明确授权同签名保数据覆盖至 0.14.8，回读包哈希与本地产物一致；不是主设备转写或 OEM 原生岛验收。
- [v0.14.8 正式 Release](https://github.com/nanzhufeng/NanfengTranscriber-Android/releases/tag/v0.14.8) 已公开，APK 与 SHA256SUMS.txt 均上传成功；GitHub 计算的 APK digest 与冻结包一致。

## In Progress

- 无；正式发布已完成，本轮合同/经验固化按本地 checkpoint 收尾，不重复发布。

## Blocked

- 无。

## Tests

| 检查 | 结论 | 时间 / 平台 / 范围 | 证据 |
| --- | --- | --- | --- |
| Release build / signing | PASS | 2026-10-10；Android 0.14.8 (23) | assembleRelease；本次冻结包重新验签，详见发布验证记录 |
| Unit tests | PASS | 2026-10-11；本轮重新运行，91 项，无失败/错误/跳过 | testDebugUnitTest；生产代码与代码 checkpoint 一致 |
| Lint | PASS | 2026-10-11；本轮重新运行 | lintDebug |
| 合同/经验资产 | PASS | 2026-10-11 14:13 +08:00；当前增量 | 合同 56 项；历史快照预期 10 项失败；经验校验 15 个链接有效，见最终回归记录 |
| 当前 UI | PASS | 2026-10-11；API 35 隔离模拟器，0.14.8 | docs/qa/readme-v0.14.8；隐私卡片与四段说明已移除 |
| 本地后台链路 | PASS | 2026-10-10；API 35 模拟器，后台实现版本 0.14.7 | docs/background-island.md；桌面/熄屏实际进度推进、结果落盘与取消清理 |
| 主设备签名覆盖 | PASS | 2026-10-10；OPPO PKH120，0.14.8 | 回读 hash 一致，首装时间与数据目录标识保留；详见发布验证记录 |
| 真实百炼 / 主设备业务 | NOT RUN | 未调用收费接口，未自动启动主设备 | 见验证缺口 |

## Verification Gaps

- 新云端模型真实账号开通、上传权限、识别结果与费用尚未验证；Mock 不证明真实服务可用。
- OPPO 真机长时/熄屏后台链路、OEM 原生岛、实际超过 10 分钟唤醒租约续期与 6 小时系统超时尚未验证。
- 本地准确率优化未取得独立 Ground Truth / CER A/B，不能宣称识别准确率提升幅度。

## Sync & Safety

- Sync checked at: 2026-10-11 14:13 +08:00；本次本地 checkpoint 收尾摘要写入前。
- Code / docs sync: OK；旧档位/Schema/导航正文已纠正并通过检查，历史 QA 保留版本标签；[增量档案](development-experience/README.md) 提供完整路由。
- Git working tree: DIRTY；当前合同/经验/检查脚本待本地提交，design-qa.md 是依赖临时图的历史材料，原样保留且不纳入当前基准。
- Remote sync: LOCAL ONLY（本轮新增资产）；既有发布源码与收尾基线已推送，本轮明确仅建立本地 checkpoint，不推送或更新 Release。字段为写入摘要前的快照，不追求自身 SHA 递归一致。
- Rollback point: UNKNOWN；旧发布可查 v0.14.4，但本次未复验该旧版本业务稳定性。
- Secrets/config risk: NONE（本次变更的暂存区范围）；未发现实际密钥、签名文件、APK 或个人绝对路径。本机签名凭据、安装备份、设备序列号不公开；没有对历史仓库作全量审计。

## Next

1. 仅在用户明确授权后验证真实云端转写与费用。
2. 后续由用户真实文件验证主设备后台与识别质量。
