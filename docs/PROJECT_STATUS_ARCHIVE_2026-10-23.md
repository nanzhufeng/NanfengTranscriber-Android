# Project Status

> 本文件只有一组当前标准字段；末尾为原阶段历史快照。本次复核时间不替代测试、设备或发布的实际执行日期。

## Verified

- Last verified: 2026-10-22 15:49 +08:00；0.14.15 Release构建/98项JVM/lint、同签名正式覆盖和回拉APK字节/安装元数据核验。
- Verified branch: main
- Code baseline: 0e0d7896552566d262c3f94f2c77ea8a0a64bae4
- Verification scope: WORKTREE；底层HEAD如上；一级入口和关于排版增量已纳入0.14.15 (30)，139项生产/构建文件冻结清单与实际安装回执见[本轮交付](releases/v0.14.15/verification.md)；视觉检查保留原模拟器证据。
- Verified by: Codex
- Status: Active

## Current

- 设置一级入口去除多余内层灰底、关于信息缩小及源码完整单行的增量已纳入0.14.15正式包，并经本次授权保数据覆盖；[视觉证据](qa/settings-entry-about-20261022/README.md)、[安装记录](releases/v0.14.15/verification.md)。本轮未自动启动主设备作业务验收。

- 界面WORKTREE已按南枫记标准统一灰底白卡、居中标题、胶囊操作及填充控件；全软件取消线框与阴影。设置皮肤单值选框点击打开含真实效果预览的六选弹窗；卡外分组标题统一、调用记录紧跟模型、“信息”改“关于”。Debug与隔离模拟器通过，详见[证据](qa/ui-surfaces-20261020/README.md)。

- 本地正式构建及主设备为Android 0.14.15 (30)：本地SenseVoice、云端Qwen文件转写和Paraformer低成本模式，队列/断点/历史/后台提醒与多格式导出，边界见[领域合同](domain-rules.md)。预设存在不等于全模型已真实验收。 本轮未发布，远端发布仍沿用下方既有证据。
- 正文明确保存、新任务按冻结参数自动导出一次已实现；旧历史不补导出。原文件核对/结果页为叉号关闭。保存、重开及TXT/MD隔离验收见[2026-10-15记录](qa/transcript-save-auto-export-20261015/README.md)，不是本轮重测。
- Paraformer无语音尾段修复已在原任务闭环：一次授权云端复现捕获SUCCESS_WITH_NO_VALID_FRAGMENT，修复后复用本地回执归档并读取1737字正文；仅该明确码作为空分块继续，整文件无文字仍为NO_SPEECH，其他失败不放行，见[修复证据](qa/paraformer-no-speech-tail-20261017/README.md)。
- 2026-10-17已同签名保数据覆盖OPPO至0.14.12，回拉APK逐字节一致，首装时间保留；CE/DE标识读取被拒绝，不宣称全库逐字节审计。
- [v0.14.12正式Release](https://github.com/nanzhufeng/NanfengTranscriber-Android/releases/tag/v0.14.12)已公开。2026-10-17匿名完整回拉/哈希/cmp通过，2026-10-18复核仍为远端latest，APK与校验文件均uploaded、digest相同，见[发布验证](releases/v0.14.12/verification.md)。0.14.10旧草稿不是当前发布阻塞。

## In Progress

- 10月22日设置入口/关于排版及授权0.14.15正式覆盖已完成；代码/文档仍未提交、推送或发布。

## Blocked

- 当前交付无已确认阻塞；未开始的验收列为缺口，不等同阻塞。

## Tests

| 检查 | 结论 | 实际运行日期 / 平台 / 版本范围 | 证据与本次复核边界 |
| --- | --- | --- | --- |
| 设置入口/关于微调 | PASS | 2026-10-22；0.14.14 Debug WORKTREE，独立5598，1140×2616/442dpi/font1.0、1.5 | compileDebugKotlin、lintDebug、assembleDebug；入口无灰底、关于正文缩小与源码单行截图检查，[证据](qa/settings-entry-about-20261022/README.md)。未重跑业务单测、未覆盖手机 |
| 整体UI / Debug与模拟器 | PASS | 2026-10-20；0.14.13 (28) WORKTREE；JVM实际14:07:37–38 UTC；独立5596外屏1140×2616、内屏2248×2480、442dpi、font1.0/1.5 | 98项JVM、编译/lint/assembleDebug；六皮肤预览/立即选择/返回取消、六子页、模型本地云端切换、语言菜单、首页/历史空态与重排检查；[证据](qa/ui-surfaces-20261020/README.md)。未执行仪器测试或主设备业务 |
| Release编译 / lint | PASS | 2026-10-22；0.14.15 (30) WORKTREE | testReleaseUnitTest、lintRelease、assembleRelease；0 Error、27 Warning、6 Information；[交付记录](releases/v0.14.15/verification.md) |
| Release JVM | PASS | 2026-10-22；0.14.15 (30)，98项，失败/错误/跳过均0 | JUnit实际07:46:44–45 UTC；未运行真实收费服务 |
| 当前运行UI | PASS | 2026-10-17；API35独立5596，0.14.12，1600×1800/320dpi | [当前预览](qa/readme-v0.14.12/README.md)，公开自造字幕输入，不是ASR准确率；本次未启动模拟器 |
| 主设备正式覆盖 | PASS（安装范围） | 2026-10-22；OPPO PKH120，0.14.14→0.14.15 | 同证书、non-debuggable、回拉cmp；首装时间/数据目录/appId/无障碍配置哈希不变；未启动业务或读取私有数据库；[记录](releases/v0.14.15/verification.md) |
| 真实百炼Paraformer | PARTIAL | 2026-10-17；原303秒任务 | 一次授权尾段复现抓到真实码，后续本地恢复完成；实际最终扣费UNKNOWN，本次未收费重试 |
| 本地后台链路 | PASS（历史范围） | 2026-10-10；API35、0.14.7 | [后台记录](background-island.md)：桌面/熄屏推进、落盘与取消；不迁移为0.14.12长时后台通过 |
| 合同 / 状态 / 基线 | PASS | 2026-10-18；状态文件WORKTREE、业务基线不变 | 本次运行verify_contract_checkpoint.rb、validate_assets.rb、字段唯一性/空白检查；93项摘要一致 |
| 真实Qwen / 扩展媒体和账号 | NOT RUN | 当前版无对应真实验收证据 | Paraformer单项成功不能推广至其他模型、全部输入或费用结算 |

已知失败：原无语音尾段故障已在历史修复及原任务恢复中闭环；本轮自动检查及正式覆盖通过，未启动主设备做转写业务或收费服务回归。

## Verification Gaps

- 0.14.15已正式覆盖，未自动启动；主设备UI/非空历史/结果/原文件媒体与后台悬浮岛业务仍待用户实际使用验证。六皮肤的选择与预览在隔离模拟器检查，不冒充全页面×六皮肤矩阵。

- Qwen高精度真实账号、上传权限、结果和计费尚未验收；Paraformer只有该任务真实证据，扩展媒体/语言/账号和最终实扣未确认。原一次/¥1授权已使用，不构成后续收费授权。
- 当前版OPPO长时/熄屏后台、OEM原生岛、超过10分钟唤醒续期及6小时系统时限未完整验证。
- 未取得独立Ground Truth/CER A/B，不宣称准确率提升幅度；无法读取CE/DE标识，未做全数据库/模型缓存逐字节完整性审计。
- 正文明确保存已实现，不再写成“只能编辑草稿”；私有JSON/TXT/Room/外部SAF并非同一事务，SRT仍保留原时间轴，本地取消不代表远端撤销。SAF突发中断、完整上游/预编译JNI安全及可重现构建仍未全面验证。

## Sync & Safety

- Sync checked at: 2026-10-22 15:49 +08:00；本次源码冻结清单、正式产物及安装回执检查。
- Code / docs sync: OK（0.14.15版本、源码冻结清单、签名、覆盖结果及交接）；原视觉/历史验证保留实际时间。
- Git working tree: DIRTY；UI、主题、悬浮岛外观及对应文档为本轮WIP；SettingsPageHeader.kt沿用此前暂存状态，本轮后续修改在工作区；既有app版本改动、发布记录和design-qa.md保留。
- Remote sync: LOCAL ONLY（本轮WIP）；HEAD仍为0e0d7896552566d262c3f94f2c77ea8a0a64bae4，未提交/推送。原远端main及v0.14.12发布状态引用2026-10-18复核，本轮不刷新远端验证。
- Rollback point: 242e2cc5ef69c67795259da207097d200bfe52f4（v0.14.12已签名、原任务恢复及公开回拉验证的代码交付点），不是数据库降级或全部业务稳定性保证。本次文档可从HEAD原文件或仓库外精确备份单独撤回。
- Secrets/config risk: NONE（本次文案与开发资产常见秘密模式检查范围）；全历史及真实配置审计UNKNOWN。不读取签名凭据/设备私有数据，不改全局Skill、记忆或网络配置。

## Next

1. 用户可自行打开手机检查整体UI与皮肤预览；当前停止于授权覆盖完成，不自动启动、提交或发布。
2. 需要继续云端验收时另行确定样本/费用范围，补Qwen链路和结算证据；按明确需求补当前版长时后台或独立CER验收，不自动操作主设备。

## 历史验证快照（非当前字段）

以下完整保留本次整理前的原阶段内容；“当前”“未提交”“尚未验证”等均指原阶段，不覆盖上方唯一当前状态。旧标准字段已显式改名，避免读取器误认当前基线。

### 历史快照：当前发布：0.14.12（2026-10-17）

- 业务/合同/回归源码242e2cc5ef69c67795259da207097d200bfe52f4与v0.14.12标签已推送；93项生产/配置摘要与冻结包对应源码一致。复核原98项Release报告、合同及冻结包，不重建、不再操作手机；当前版README预览来自独立5596公开演示，实例已结束。
- [v0.14.12正式Release](https://github.com/nanzhufeng/NanfengTranscriber-Android/releases/tag/v0.14.12)已公开（ID415593775，draft=false、prerelease=false），仅APK和校验文件两项附件。GitHub大小/摘要一致，匿名公开下载HTTP200、106883328字节、SHA256与冻结包相同，逐字节cmp通过；README下载已切换。详见[发布验证](releases/v0.14.12/verification.md)。之前0.14.10草稿及失败记录保留，不误改为成功。

### 历史快照：Paraformer尾段失败修复（2026-10-17）

- WORKTREE 0.14.12 (27)。用户授权同签名保数据覆盖和仅一次原任务云端重试，限¥1。真实回执确认：303秒媒体的尾段返回SUCCESS_WITH_NO_VALID_FRAGMENT，旧引擎错误地把无语音分块当整文件失败。修复为空分块提交、保留前文，整文件无正文仍为NO_SPEECH；其他失败不放行。
- 0.14.11先保留安全回执，0.14.12复用该回执在本地恢复原任务；手机历史结果已归档且打开可读，正文1737字符。未再次提交收费重试。98项Release JVM、lintVitalRelease、assembleRelease通过，最终正式包与设备回拉逐字节一致，首装时间保留；无法读取CE/DE目录标识，不宣称全库逐字节审计。详见[本次证据](qa/paraformer-no-speech-tail-20261017/README.md)。未提交、推送或发布；design-qa.md不动。

### 历史快照：当前发布（2026-10-16）

- 0.14.10源码、合同和当前README预览已提交推送至main，源码提交5249d34d1ca506c314c1d2bc34cd805433a18cbb，v0.14.10标签指向该提交。93项冻结生产/配置摘要一致；308项合同检查通过，复核原94项Release JVM报告，未重建、重签或再次操作手机。
- GitHub Release 414785494仍为草稿：校验文件已上传，APK未完成上传（HTTP408、连接重置及最终180秒超时；最后仅传出12976128字节）。不得称为已发布或可下载；首页暂保留上一公开v0.14.8下载入口。恢复上传后须核对完整APK远端大小与摘要，再公开。证据见[发布记录](releases/v0.14.10/verification.md)。旧阶段“未提交/未发布”保留为原日期事实。

### 历史快照：最新小改动（2026-10-16）

- 用户本轮授权后，0.14.10 (25)已正式同签名保数据覆盖OPPO；回拉包cmp一致、签名相同、首装时间/CE/DE目录标识保留，设备临时APK已清理。Release94项/lintVitalRelease/构建通过；未自动启动、未提交推送发布。冻结包见[覆盖记录](releases/v0.14.10/verification.md)，不再重建。

- WORKTREE：原文件核对与转写结果右上角ArrowBack改为Close叉号，沿用原onDismiss、点击区域和系统返回；保留此前保存/自动导出的既有WIP。
- 定向差异核验PASS：只替换两处图标/可访问名称并删除无用import，onDismiss及此前WIP逐字保留；compileDebugKotlin和空白检查PASS。未做模拟器/真机视觉验收，未改版本或冻结Release、未安装设备、未提交推送。

> 当前状态摘要；代码与适用验证证据优先，不把模型预设、接口 Mock 或覆盖安装当作真实云端/真机业务验收。

### 历史快照：最新覆盖（2026-10-15）

- 用户授权后正式0.14.9 (24)已同签名保数据覆盖OPPO，回拉APK SHA一致，首装时间/数据目录标识保留；Release94项、lintVitalRelease及构建通过。未自动启动业务、提交或发布。详见[覆盖记录](releases/v0.14.9/verification.md)。
- 两个新增核心文件已显式暂存，合同门禁整体PASS。下节“未覆盖/未跟踪”是覆盖前快照。

### 历史快照：最新增量（2026-10-15）

- WORKTREE：正文保存按钮、修改持久化、新任务按结果页选中格式自动导出一次；旧Completed不补导出。详见[验证记录](qa/transcript-save-auto-export-20261015/README.md)。
- 94项JVM、lint、Debug构建通过；5580正常UI验证保存/重启、TXT及MD自动输出和无重复；未验证主设备、真实ASR及SAF突发中断。
- 未提交/发布/主设备覆盖；正式冻结包不变。合同整体检查仅剩新增核心文件未纳入Git一项，语义/链接通过。下方为0.14.8历史交付快照，不覆盖本次WORKTREE。

### 历史快照：Verified

- 历史字段 Last verified: 2026-10-12 12:26 +08:00
- 历史字段 Verified branch: main
- 历史字段 Code baseline: 2bad9c2d559f69642ede2e4acd7a37b2599d9b71
- 历史字段 Verification scope: 业务源码COMMIT与v0.14.8一致；本轮两轮资料/开发检查按用户授权纳入Git并推送checkpoint，本摘要是提交前快照，最终以Git refs回读为准。合同通过静态门禁，见 [合同统一审计](qa/contracts-20261012/README.md)，未刷新旧测试/设备运行日期或改变功能。
- 历史字段 Verified by: Codex
- 历史字段 Status: Active

### 历史快照：Current

- 0.14.8 (23) 正式签名 APK：本地 SenseVoice、云端文件转写预设与 Paraformer 低成本模式、队列/历史/明确导出；云端接入通过接口测试，真实账号服务未验收。
- 后台独立服务、已有片段断点、真实进度通知与点击展开的简洁灵动岛；设置仅保留授权状态和三个权限入口。
- 主设备已按明确授权同签名保数据覆盖至 0.14.8，回读包哈希与本地产物一致；不是主设备转写或 OEM 原生岛验收。
- [v0.14.8 正式 Release](https://github.com/nanzhufeng/NanfengTranscriber-Android/releases/tag/v0.14.8) 已公开，APK 与 SHA256SUMS.txt 均上传成功；GitHub 计算的 APK digest 与冻结包一致。

### 历史快照：In Progress

- 无；全部根级合同已登记唯一职责，搜索/草稿/字幕预检/输出/会话/取消等边界按源码同步；旧决策与复盘归为历史/快照，防漂移门禁通过。不自动推进缺口修复或发布。

### 历史快照：Blocked

- 无。

### 历史快照：Tests

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

### 历史快照：Verification Gaps

- 新云端模型真实账号开通、上传权限、识别结果与费用尚未验证；Mock 不证明真实服务可用。
- OPPO 真机长时/熄屏后台链路、OEM 原生岛、实际超过 10 分钟唤醒租约续期与 6 小时系统超时尚未验证。
- 本地准确率优化未取得独立 Ground Truth / CER A/B，不能宣称识别准确率提升幅度。
- 本轮复盘确认：编辑只是草稿、TXT与Room不属同一事务、云端取消不代表远端撤销；完整上游/预编译JNI安全及可重现构建未验证，详见完整档案问题表。

### 历史快照：Sync & Safety

- 历史字段 Sync checked at: 2026-10-12 12:26 +08:00；收尾摘要写入前，远端未联网刷新。
- 历史字段 Code / docs sync: OK（本轮全部根级合同范围）；唯一正文见 [合同索引](contracts-index.md)，历史/快照不覆盖现行。源码旧注释、大文件、全文检索/编辑保存/远端取消等限制保留，不声明技术债全部消除。
- 历史字段 Git working tree: DIRTY（本次提交前快照）；两轮36个资料文件经范围检查准备纳入Git，业务代码/测试/构建无差异；design-qa.md原样保留、不提交。
- 历史字段 Remote sync: LOCAL ONLY（本次提交前快照）；已刷新origin/main为5152a3d，本地领先1个纯文档提交且不落后。用户本轮授权推送main及checkpoint-docs-contracts-skills-20261012标签，最终同步由Git回读确认，不更新Release。
- 历史字段 Rollback point: UNKNOWN；旧发布可查 v0.14.4，但本次未复验该旧版本业务稳定性。
- 历史字段 Secrets/config risk: NONE（本轮新增文档/Skill常见秘密模式检查范围）；不声称全历史秘密审计完成。本机签名凭据、安装备份和设备私有数据不读取或公开；没有改全球Skills或长期记忆。

### 历史快照：Next

1. 仅在用户明确授权后验证真实云端转写与费用。
2. 后续由用户真实文件验证主设备后台与识别质量。
