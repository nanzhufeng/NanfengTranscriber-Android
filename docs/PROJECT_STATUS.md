# Project Status

> 当前状态摘要；代码与适用验证证据优先，不把模型预设、接口 Mock 或覆盖安装当作真实云端/真机业务验收。

## Verified

- Last verified: 2026-10-12 12:26 +08:00
- Verified branch: main
- Code baseline: 2bad9c2d559f69642ede2e4acd7a37b2599d9b71
- Verification scope: 业务源码COMMIT与v0.14.8一致；本轮两轮资料/开发检查按用户授权纳入Git并推送checkpoint，本摘要是提交前快照，最终以Git refs回读为准。合同通过静态门禁，见 [合同统一审计](qa/contracts-20261012/README.md)，未刷新旧测试/设备运行日期或改变功能。
- Verified by: Codex
- Status: Active

## Current

- 0.14.8 (23) 正式签名 APK：本地 SenseVoice、云端文件转写预设与 Paraformer 低成本模式、队列/历史/明确导出；云端接入通过接口测试，真实账号服务未验收。
- 后台独立服务、已有片段断点、真实进度通知与点击展开的简洁灵动岛；设置仅保留授权状态和三个权限入口。
- 主设备已按明确授权同签名保数据覆盖至 0.14.8，回读包哈希与本地产物一致；不是主设备转写或 OEM 原生岛验收。
- [v0.14.8 正式 Release](https://github.com/nanzhufeng/NanfengTranscriber-Android/releases/tag/v0.14.8) 已公开，APK 与 SHA256SUMS.txt 均上传成功；GitHub 计算的 APK digest 与冻结包一致。

## In Progress

- 无；全部根级合同已登记唯一职责，搜索/草稿/字幕预检/输出/会话/取消等边界按源码同步；旧决策与复盘归为历史/快照，防漂移门禁通过。不自动推进缺口修复或发布。

## Blocked

- 无。

## Tests

| 检查 | 结论 | 时间 / 平台 / 范围 | 证据 |
| --- | --- | --- | --- |
| Release build / signing | PASS | 2026-10-10；Android 0.14.8 (23)，历史冻结验签 | assembleRelease与发布验证记录；本轮不重建/重签 |
| Unit tests | PASS | 2026-10-11已运行；91项，无失败/错误/跳过 | 本轮仅读取既有报告；业务代码与checkpoint一致 |
| Lint | PASS | 2026-10-11已运行 | lintDebug；本轮没有重跑 |
| 合同/开发资产 | PASS | 2026-10-12；当前WORKTREE，源码/包不变 | 合同306项、五类Skill/233个本地链接、语法/空白及两项负向参数通过；旧文档预期9项FAIL。官方quick_validate缺PyYAML未运行；未重跑JVM/lint，读取既有91项报告 |
| 当前 UI | PASS | 2026-10-11；API 35 隔离模拟器，0.14.8 | docs/qa/readme-v0.14.8；隐私卡片与四段说明已移除 |
| 本地后台链路 | PASS | 2026-10-10；API 35 模拟器，后台实现版本 0.14.7 | docs/background-island.md；桌面/熄屏实际进度推进、结果落盘与取消清理 |
| 主设备签名覆盖 | PASS | 2026-10-10；OPPO PKH120，0.14.8 | 回读 hash 一致，首装时间与数据目录标识保留；详见发布验证记录 |
| 真实百炼 / 主设备业务 | NOT RUN | 未调用收费接口，未自动启动主设备 | 见验证缺口 |

## Verification Gaps

- 新云端模型真实账号开通、上传权限、识别结果与费用尚未验证；Mock 不证明真实服务可用。
- OPPO 真机长时/熄屏后台链路、OEM 原生岛、实际超过 10 分钟唤醒租约续期与 6 小时系统超时尚未验证。
- 本地准确率优化未取得独立 Ground Truth / CER A/B，不能宣称识别准确率提升幅度。
- 本轮复盘确认：编辑只是草稿、TXT与Room不属同一事务、云端取消不代表远端撤销；完整上游/预编译JNI安全及可重现构建未验证，详见完整档案问题表。

## Sync & Safety

- Sync checked at: 2026-10-12 12:26 +08:00；收尾摘要写入前，远端未联网刷新。
- Code / docs sync: OK（本轮全部根级合同范围）；唯一正文见 [合同索引](contracts-index.md)，历史/快照不覆盖现行。源码旧注释、大文件、全文检索/编辑保存/远端取消等限制保留，不声明技术债全部消除。
- Git working tree: DIRTY（本次提交前快照）；两轮36个资料文件经范围检查准备纳入Git，业务代码/测试/构建无差异；design-qa.md原样保留、不提交。
- Remote sync: LOCAL ONLY（本次提交前快照）；已刷新origin/main为5152a3d，本地领先1个纯文档提交且不落后。用户本轮授权推送main及checkpoint-docs-contracts-skills-20261012标签，最终同步由Git回读确认，不更新Release。
- Rollback point: UNKNOWN；旧发布可查 v0.14.4，但本次未复验该旧版本业务稳定性。
- Secrets/config risk: NONE（本轮新增文档/Skill常见秘密模式检查范围）；不声称全历史秘密审计完成。本机签名凭据、安装备份和设备私有数据不读取或公开；没有改全球Skills或长期记忆。

## Next

1. 仅在用户明确授权后验证真实云端转写与费用。
2. 后续由用户真实文件验证主设备后台与识别质量。
