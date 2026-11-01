# Project Status

> 本文件只有一组当前标准字段；原阶段历史快照见末尾归档链接。本次复核时间不替代测试、设备或发布的实际执行日期。

## Verified

- Last verified: 2026-11-01；源码冻结/104项Release JVM及lint复核、同APK隔离运行截图；10月29日的0.14.17 Release构建/104项JVM/lint/验签与隔离模拟器置顶验证；本轮已授权同签名保数据覆盖并回拉核验，未自动启动。
- Verified branch: main
- Code baseline: a8fbfae01b13e9ca04355be7ea856d24c41ab7ae（v0.14.17发布代码；其后仅发布回执文档）
- Verification scope: WORKTREE；底层HEAD如上；置顶修复已纳入0.14.17 (32)，141项生产/构建源码清单及正式构建见[记录](releases/v0.14.17/verification.md)；已同签名保数据覆盖并回拉核验，不视为ColorOS业务/UI验收。
- Verified by: Codex
- Status: Active

## Current

- 0.14.16状态栏层级被用户两张ColorOS截图确认为不合格：系统图标压住岛内进度。0.14.17新增用户手动开启的置顶窗口宿主，保留摄像头分栏；普通悬浮窗始终在状态栏下方。隔离层级、直接点击、权限关闭/重连、切屏与大字体通过，[QA](qa/island-topmost-20261029/README.md)、[正式包](releases/v0.14.17/verification.md)。用户已授权覆盖OPPO至0.14.17，回拉APK与冻结包逐字节一致、数据身份与无障碍配置哈希未变；未自动启动或修改无障碍配置。

- 设置一级入口去除多余内层灰底、关于信息缩小及源码完整单行的增量已纳入0.14.15正式包，并经本次授权保数据覆盖；[视觉证据](qa/settings-entry-about-20261022/README.md)、[安装记录](releases/v0.14.15/verification.md)。本轮未自动启动主设备作业务验收。

- 界面WORKTREE已按南枫记标准统一灰底白卡、居中标题、胶囊操作及填充控件；全软件取消线框与阴影。设置皮肤单值选框点击打开含真实效果预览的六选弹窗；卡外分组标题统一、调用记录紧跟模型、“信息”改“关于”。Debug与隔离模拟器通过，详见[证据](qa/ui-surfaces-20261020/README.md)。

- 本地正式包及主设备已核对为Android 0.14.17 (32)：本地SenseVoice、云端Qwen文件转写和Paraformer低成本模式，队列/断点/历史/后台提醒与多格式导出，边界见[领域合同](domain-rules.md)。预设存在不等于全模型已真实验收。 本轮已发布[v0.14.17](https://github.com/nanzhufeng/NanfengTranscriber-Android/releases/tag/v0.14.17)，回执见[发布核验](releases/v0.14.17/publication-verification.json)。
- 正文明确保存、新任务按冻结参数自动导出一次已实现；旧历史不补导出。原文件核对/结果页为叉号关闭。保存、重开及TXT/MD隔离验收见[2026-10-15记录](qa/transcript-save-auto-export-20261015/README.md)，不是本轮重测。
- Paraformer无语音尾段修复已在原任务闭环：一次授权云端复现捕获SUCCESS_WITH_NO_VALID_FRAGMENT，修复后复用本地回执归档并读取1737字正文；仅该明确码作为空分块继续，整文件无文字仍为NO_SPEECH，其他失败不放行，见[修复证据](qa/paraformer-no-speech-tail-20261017/README.md)。
- 2026-10-17已同签名保数据覆盖OPPO至0.14.12，回拉APK逐字节一致，首装时间保留；CE/DE标识读取被拒绝，不宣称全库逐字节审计。
- [v0.14.12正式Release](https://github.com/nanzhufeng/NanfengTranscriber-Android/releases/tag/v0.14.12)已公开。2026-10-17匿名完整回拉/哈希/cmp通过，2026-10-18复核仍为远端latest，APK与校验文件均uploaded、digest相同，见[发布验证](releases/v0.14.12/verification.md)。0.14.10旧草稿不是当前发布阻塞。

## In Progress

- 用户11月1日授权的GitHub正式发布已完成；main源码/README、v0.14.17 latest Release及APK匿名下载回拉逐字节核验通过。未重建/重签冻结包。

- 置顶修复、独立模拟器与0.14.17授权保数据覆盖已完成；用户手动开启置顶后的ColorOS业务/UI验收尚未执行，源码与README已提交发布；本轮不扩展ColorOS业务验收。

## Blocked

- 当前交付无已确认阻塞；未开始的验收列为缺口，不等同阻塞。

## Tests

2026-11-01发布复核：104项Release JVM与lint通过；同冻结APK隔离运行三页README预览，141项源码与标签提交一致；正式Release/附件与公开下载回拉通过，见[发布回执](releases/v0.14.17/publication-verification.json)。无GitHub Actions workflow，不虚构云端检查。

| 检查 | 结论 | 实际运行日期 / 平台 / 版本范围 | 证据与本次复核边界 |
| --- | --- | --- | --- |
| 状态栏层级修复 | PASS（本地/隔离） | 2026-10-29；0.14.17 WORKTREE；104项Release JVM失败/错误/跳过均0，lint/assemble/验签通过 | 置顶层311000高于状态栏151000；普通111000退回安全区；顶部点击/撤销重连/折叠/大字体/设置入口，[证据](qa/island-topmost-20261029/README.md)。主设备NOT RUN |
| 灵动岛状态栏/挖孔布局 | PASS（本地/模拟器） | 2026-10-25；Debug WORKTREE；103项JVM失败/错误/跳过均0；自有5598，1140×2616与2248×2480、442dpi、font1.0/1.5 | 编译/lint/构建；分栏/未知进度/完整轮廓、展开/收起、同任务折叠保持、取消驱动清理，[证据](qa/island-statusbar-20261025/README.md)。非真实转写或主设备验收 |
| 设置入口/关于微调 | PASS | 2026-10-22；0.14.14 Debug WORKTREE，独立5598，1140×2616/442dpi/font1.0、1.5 | compileDebugKotlin、lintDebug、assembleDebug；入口无灰底、关于正文缩小与源码单行截图检查，[证据](qa/settings-entry-about-20261022/README.md)。未重跑业务单测、未覆盖手机 |
| 整体UI / Debug与模拟器 | PASS | 2026-10-20；0.14.13 (28) WORKTREE；JVM实际14:07:37–38 UTC；独立5596外屏1140×2616、内屏2248×2480、442dpi、font1.0/1.5 | 98项JVM、编译/lint/assembleDebug；六皮肤预览/立即选择/返回取消、六子页、模型本地云端切换、语言菜单、首页/历史空态与重排检查；[证据](qa/ui-surfaces-20261020/README.md)。未执行仪器测试或主设备业务 |
| Release编译 / lint | PASS | 2026-10-25；0.14.16 (31) WORKTREE | testReleaseUnitTest、lintRelease、assembleRelease；0 Error、27 Warning、6 Information；[记录](releases/v0.14.16/verification.md) |
| Release JVM | PASS | 2026-10-25；0.14.16 (31)，103项失败/错误/跳过均0 | JUnit实际05:29:11–12 UTC；未运行真实转写或收费服务 |
| 当前运行UI | PASS | 2026-10-17；API35独立5596，0.14.12，1600×1800/320dpi | [当前预览](qa/readme-v0.14.12/README.md)，公开自造字幕输入，不是ASR准确率；本次未启动模拟器 |
| 0.14.17主设备正式覆盖 | PASS（安装范围） | 2026-10-29；OPPO PKH120，0.14.16→0.14.17 | 同证书、non-debuggable、回拉cmp；首装时间/数据目录/appId/无障碍配置哈希不变；临时APK清理，未启动App或读私有数据库，[回执](releases/v0.14.17/installation-verification.json) |
| 主设备正式覆盖 | PASS（安装范围） | 2026-10-25；OPPO PKH120，0.14.15→0.14.16 | 同证书、non-debuggable、回拉cmp；首装时间/数据目录/appId/无障碍配置哈希不变；未自动启动或读私有数据库；[记录](releases/v0.14.16/verification.md) |
| 真实百炼Paraformer | PARTIAL | 2026-10-17；原303秒任务 | 一次授权尾段复现抓到真实码，后续本地恢复完成；实际最终扣费UNKNOWN，本次未收费重试 |
| 本地后台链路 | PASS（历史范围） | 2026-10-10；API35、0.14.7 | [后台记录](background-island.md)：桌面/熄屏推进、落盘与取消；不迁移为0.14.12长时后台通过 |
| 合同 / 状态 / 基线 | PASS | 2026-10-18；状态文件WORKTREE、业务基线不变 | 本次运行verify_contract_checkpoint.rb、validate_assets.rb、字段唯一性/空白检查；93项摘要一致 |
| 真实Qwen / 扩展媒体和账号 | NOT RUN | 当前版无对应真实验收证据 | Paraformer单项成功不能推广至其他模型、全部输入或费用结算 |

已知失败：用户10月29日确认0.14.16被系统图标遮挡；0.14.17已在独立模拟器修正，但ColorOS未复验。原无语音尾段故障保留历史闭环；本轮已授权覆盖主设备，但未跑转写或收费服务。

## Verification Gaps

- 0.14.17已授权同签名保数据覆盖主设备；尚未自动启动，需用户手动开启置顶服务后验证ColorOS效果，不自动更改无障碍配置。

- 0.14.16已正式覆盖，未自动启动；主设备UI/非空历史/结果/原文件媒体与后台悬浮岛业务仍待用户实际使用验证。六皮肤的选择与预览在隔离模拟器检查，不冒充全页面×六皮肤矩阵。

- Qwen高精度真实账号、上传权限、结果和计费尚未验收；Paraformer只有该任务真实证据，扩展媒体/语言/账号和最终实扣未确认。原一次/¥1授权已使用，不构成后续收费授权。
- 当前版OPPO长时/熄屏后台、OEM原生岛、超过10分钟唤醒续期及6小时系统时限未完整验证。
- 未取得独立Ground Truth/CER A/B，不宣称准确率提升幅度；无法读取CE/DE标识，未做全数据库/模型缓存逐字节完整性审计。
- 正文明确保存已实现，不再写成“只能编辑草稿”；私有JSON/TXT/Room/外部SAF并非同一事务，SRT仍保留原时间轴，本地取消不代表远端撤销。SAF突发中断、完整上游/预编译JNI安全及可重现构建仍未全面验证。

## Sync & Safety

- Sync checked at: 2026-10-29；0.14.17源码冻结清单、正式包、隔离置顶证据；新增OPPO同签名覆盖/回拉字节与数据身份回执。
- Code / docs sync: OK（0.14.17候选、生产/构建源码清单、签名、隔离验证与交接；最新安装证据0.14.17）；历史截图和测试保留各自实际时间。
- Git working tree: 发布代码与交付文档已提交；原有design-qa.md未跟踪文件保留。未执行reset/clean/stash。
- Remote sync: SYNCED；main已推送发布代码a8fbfae，v0.14.17正式Release为latest，唯一APK uploaded且匿名下载/哈希/cmp通过；后续仅同步发布回执文档。
- Rollback point: 242e2cc5ef69c67795259da207097d200bfe52f4（v0.14.12已签名、原任务恢复及公开回拉验证的代码交付点），不是数据库降级或全部业务稳定性保证。本次文档可从HEAD原文件或仓库外精确备份单独撤回。
- Secrets/config risk: NONE（本次文案与开发资产常见秘密模式检查范围）；全历史及真实配置审计UNKNOWN。未输出/入仓签名凭据，不读取设备私有数据；正式构建仅使用原配置签名。不改全局Skill、记忆或网络配置。

## Next

1. 当前0.14.17已覆盖，停止于授权安装完成。用户在系统无障碍页手动开启置顶服务后自行查看ColorOS效果；未自动启动或改权限；本轮仅发布已核验版本。
2. 需要继续云端验收时另行确定样本/费用范围，补Qwen链路和结算证据；按明确需求补当前版长时后台或独立CER验收，不自动操作主设备。

## 历史归档

[2026-10-23整理前完整状态页](PROJECT_STATUS_ARCHIVE_2026-10-23.md)逐字节保留原文，含原阶段历史快照；不作为新的验证或执行授权。本次仅整理入口，以上实际验证日期与结论未刷新。
