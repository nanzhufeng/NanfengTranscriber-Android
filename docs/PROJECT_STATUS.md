# Project Status

## Paraformer尾段失败修复（2026-10-17）

- WORKTREE 0.14.12 (27)。用户授权同签名保数据覆盖和仅一次原任务云端重试，限¥1。真实回执确认：303秒媒体的尾段返回SUCCESS_WITH_NO_VALID_FRAGMENT，旧引擎错误地把无语音分块当整文件失败。修复为空分块提交、保留前文，整文件无正文仍为NO_SPEECH；其他失败不放行。
- 0.14.11先保留安全回执，0.14.12复用该回执在本地恢复原任务；手机历史结果已归档且打开可读，正文1737字符。未再次提交收费重试。98项Release JVM、lintVitalRelease、assembleRelease通过，最终正式包与设备回拉逐字节一致，首装时间保留；无法读取CE/DE目录标识，不宣称全库逐字节审计。详见[本次证据](qa/paraformer-no-speech-tail-20261017/README.md)。未提交、推送或发布；design-qa.md不动。

## 当前发布（2026-10-16）

- 0.14.10源码、合同和当前README预览已提交推送至main，源码提交5249d34d1ca506c314c1d2bc34cd805433a18cbb，v0.14.10标签指向该提交。93项冻结生产/配置摘要一致；308项合同检查通过，复核原94项Release JVM报告，未重建、重签或再次操作手机。
- GitHub Release 414785494仍为草稿：校验文件已上传，APK未完成上传（HTTP408、连接重置及最终180秒超时；最后仅传出12976128字节）。不得称为已发布或可下载；首页暂保留上一公开v0.14.8下载入口。恢复上传后须核对完整APK远端大小与摘要，再公开。证据见[发布记录](releases/v0.14.10/verification.md)。旧阶段“未提交/未发布”保留为原日期事实。

## 最新小改动（2026-10-16）

- 用户本轮授权后，0.14.10 (25)已正式同签名保数据覆盖OPPO；回拉包cmp一致、签名相同、首装时间/CE/DE目录标识保留，设备临时APK已清理。Release94项/lintVitalRelease/构建通过；未自动启动、未提交推送发布。冻结包见[覆盖记录](releases/v0.14.10/verification.md)，不再重建。

- WORKTREE：原文件核对与转写结果右上角ArrowBack改为Close叉号，沿用原onDismiss、点击区域和系统返回；保留此前保存/自动导出的既有WIP。
- 定向差异核验PASS：只替换两处图标/可访问名称并删除无用import，onDismiss及此前WIP逐字保留；compileDebugKotlin和空白检查PASS。未做模拟器/真机视觉验收，未改版本或冻结Release、未安装设备、未提交推送。

> 当前状态摘要；代码与适用验证证据优先，不把模型预设、接口 Mock 或覆盖安装当作真实云端/真机业务验收。

## 最新覆盖（2026-10-15）

- 用户授权后正式0.14.9 (24)已同签名保数据覆盖OPPO，回拉APK SHA一致，首装时间/数据目录标识保留；Release94项、lintVitalRelease及构建通过。未自动启动业务、提交或发布。详见[覆盖记录](releases/v0.14.9/verification.md)。
- 两个新增核心文件已显式暂存，合同门禁整体PASS。下节“未覆盖/未跟踪”是覆盖前快照。

## 最新增量（2026-10-15）

- WORKTREE：正文保存按钮、修改持久化、新任务按结果页选中格式自动导出一次；旧Completed不补导出。详见[验证记录](qa/transcript-save-auto-export-20261015/README.md)。
- 94项JVM、lint、Debug构建通过；5580正常UI验证保存/重启、TXT及MD自动输出和无重复；未验证主设备、真实ASR及SAF突发中断。
- 未提交/发布/主设备覆盖；正式冻结包不变。合同整体检查仅剩新增核心文件未纳入Git一项，语义/链接通过。下方为0.14.8历史交付快照，不覆盖本次WORKTREE。

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
