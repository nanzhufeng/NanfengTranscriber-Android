# Project Status

> 当前状态摘要；代码与适用验证证据优先，不把模型预设、接口 Mock 或覆盖安装当作真实云端/真机业务验收。

## Verified

- Last verified: 2026-10-11 12:54 +08:00
- Verified branch: main
- Code baseline: b02799b5df953504716fa9b879d484dc3799bfde
- Verification scope: WORKTREE；本次发布的 app/build.gradle.kts、app/src/main、app/src/test 及配套文档；可重建快照随发布提交保存。见 [v0.14.8 验证记录](releases/v0.14.8-verification.md)。
- Verified by: Codex
- Status: Active

## Current

- 0.14.8 (23) 正式签名 APK：本地 SenseVoice、云端文件转写预设与 Paraformer 低成本模式、队列/历史/明确导出；云端接入通过接口测试，真实账号服务未验收。
- 后台独立服务、已有片段断点、真实进度通知与点击展开的简洁灵动岛；设置仅保留授权状态和三个权限入口。
- 主设备已按明确授权同签名保数据覆盖至 0.14.8，回读包哈希与本地产物一致；不是主设备转写或 OEM 原生岛验收。

## In Progress

- 将当前代码推送 main，并发布 v0.14.8 正式 Release 的 APK 与校验附件。

## Blocked

- 无。

## Tests

| 检查 | 结论 | 时间 / 平台 / 范围 | 证据 |
| --- | --- | --- | --- |
| Release build / signing | PASS | 2026-10-10；Android 0.14.8 (23) | assembleRelease；本次冻结包重新验签，详见发布验证记录 |
| Unit tests | PASS | 2026-10-11；当前工作区，91 项，无失败/错误/跳过 | testDebugUnitTest；包含百炼 Mock 接口和队列生命周期回归 |
| Lint | PASS | 2026-10-11；当前工作区 | lintDebug |
| 当前 UI | PASS | 2026-10-11；API 35 隔离模拟器，0.14.8 | docs/qa/readme-v0.14.8；隐私卡片与四段说明已移除 |
| 本地后台链路 | PASS | 2026-10-10；API 35 模拟器，后台实现版本 0.14.7 | docs/background-island.md；桌面/熄屏实际进度推进、结果落盘与取消清理 |
| 主设备签名覆盖 | PASS | 2026-10-10；OPPO PKH120，0.14.8 | 回读 hash 一致，首装时间与数据目录标识保留；详见发布验证记录 |
| 真实百炼 / 主设备业务 | NOT RUN | 未调用收费接口，未自动启动主设备 | 见验证缺口 |

## Verification Gaps

- 新云端模型真实账号开通、上传权限、识别结果与费用尚未验证；Mock 不证明真实服务可用。
- OPPO 真机长时/熄屏后台链路、OEM 原生岛、实际超过 10 分钟唤醒租约续期与 6 小时系统超时尚未验证。
- 本地准确率优化未取得独立 Ground Truth / CER A/B，不能宣称识别准确率提升幅度。

## Sync & Safety

- Sync checked at: 2026-10-11 12:54 +08:00；写入本状态前。
- Code / docs sync: OK；当前源码与 README / 发布验证范围对应，历史 QA 保留版本标签。
- Git working tree: DIRTY；当前发布改动待提交，另保留无关的 design-qa.md，不纳入发布。
- Remote sync: LOCAL ONLY；origin/main 与底层基线一致，当前发布改动尚未推送。
- Rollback point: UNKNOWN；旧发布可查 v0.14.4，但本次未复验该旧版本业务稳定性。
- Secrets/config risk: REVIEW NEEDED；发布暂存区将检查密钥/签名文件与个人路径；本机签名凭据、安装备份、设备序列号不公开。

## Next

1. 完成本次正式 Release 与附件远端回读。
2. 仅在用户明确授权后验证真实云端转写与费用。
3. 后续由用户真实文件验证主设备后台与识别质量。
