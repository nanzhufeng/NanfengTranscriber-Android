# 2026-10-11 完整项目复盘验证

范围：开始 HEAD `2bad9c2d559f69642ede2e4acd7a37b2599d9b71`，本轮 WORKTREE 文档/AGENTS/项目 Skills 增量；业务源码、测试源和 Gradle 配置不改。当前冻结 APK 保持不变。本次没有 Git 提交/推送、设备动作、收费调用或 Release 生成。

## 盘点与证据

- [全量索引](inventory.md) 枚举初始314个Git跟踪项、17个可达提交，文本结构扫描、JSON解析、二进制大小/摘要及供应商边界。它不是逐行语义或二进制内部安全认证。
- 关键状态/数据/媒体/ASR/后台/历史/导出链路与合同对照见 [完整档案](../../南枫转写Android完整开发档案.md)，冲突逐项记录；历史失败与仪器记录保留原版本，不当成当前授权。
- 初始未知改动只有未跟踪 `design-qa.md`，原样保留。旧文档/根AGENTS修改前有精确本机快照，备份不进入Git或公开资产。

## 本次检查结果

| 检查 | 实际命令 / 范围 | 结果 |
| --- | --- | --- |
| JVM/lint | Android Studio JBR；`./gradlew testDebugUnitTest lintDebug --quiet` | PASS；28套JUnit、91tests，0失败/错误/跳过；允许Gradle复用UP-TO-DATE任务，不声称强制重算 |
| 初始合同与冻结包 | `ruby scripts/verify_contract_checkpoint.rb --baseline checkpoint-v0.14.8-20261011-final --apk 'app/build/outputs/apk/release/南枫转写.apk' --unit-results app/build/test-results/testDebugUnitTest` | 初始56项PASS；本轮增加可迁移入口链接后57项PASS |
| 文件清单脚本 | `ruby .agents/skills/nanfeng-transcriber-doc-sync/scripts/inventory.rb` | 实际运行并保存索引；最初相对根路径多一级，校正后通过；无业务/设备副作用 |
| 项目资产结构 | `ruby .agents/skills/nanfeng-transcriber-doc-sync/scripts/validate_assets.rb` | PASS；五个Skill的YAML/命名/UI metadata、185个持久本地链接、占位符/常见秘密模式，0失败；两个新脚本语法及实际运行通过 |
| 官方Skill quick_validate | 系统Python和Bundled Python均无PyYAML | NOT RUN；不安装全局依赖，以Ruby YAML.safe_load和等价字段/命名检查补充；不冒充官方脚本PASS |
| 变更边界 | `git diff --stat -- app build.gradle.kts settings.gradle.kts gradle.properties` | 空；既有源码/配置保持不动 |
| 历史服务/设备 | 只引用对应版本QA；未重跑 | 当前仍缺真实百炼、OPPO长时/准确率与OEM原生岛证据 |

本地资产检查只证明结构/链接与粗粒度秘密模式，不证明没有所有类型敏感信息、未来Skill决策永远正确或真实服务通过。供应商全部上游源码与预编译JNI不在本轮逐行/可重现验证范围。

## 产物和未确认项

[长期契约](../../../AGENTS.md)、[完整档案](../../南枫转写Android完整开发档案.md)、[可迁移经验](../../可迁移开发经验.md)、[五类项目Skills](../../../.agents/skills)、[状态](../../PROJECT_STATUS.md)；尚未确认问题集中在完整档案第15节。没有改全球Skills或长期记忆；本次文档完成不抬高旧版本设备/服务验收等级。

## 本轮文件清单

新增16个文件：

- docs/南枫转写Android完整开发档案.md
- docs/可迁移开发经验.md
- docs/qa/project-review-20261011/README.md
- docs/qa/project-review-20261011/inventory.md
- .agents/skills/nanfeng-transcriber-development/SKILL.md
- .agents/skills/nanfeng-transcriber-development/agents/openai.yaml
- .agents/skills/nanfeng-transcriber-debugging/SKILL.md
- .agents/skills/nanfeng-transcriber-debugging/agents/openai.yaml
- .agents/skills/nanfeng-transcriber-testing/SKILL.md
- .agents/skills/nanfeng-transcriber-testing/agents/openai.yaml
- .agents/skills/nanfeng-transcriber-code-review/SKILL.md
- .agents/skills/nanfeng-transcriber-code-review/agents/openai.yaml
- .agents/skills/nanfeng-transcriber-doc-sync/SKILL.md
- .agents/skills/nanfeng-transcriber-doc-sync/agents/openai.yaml
- .agents/skills/nanfeng-transcriber-doc-sync/scripts/inventory.rb
- .agents/skills/nanfeng-transcriber-doc-sync/scripts/validate_assets.rb

修改9个文件：

- AGENTS.md：仅长期规则和五类项目流程路由。
- docs/PROJECT_STATUS.md：本轮验证范围与未确认项。
- docs/CURRENT_HANDOFF.md：本轮紧凑交接。
- docs/domain-rules.md：JSON/TXT实际写入边界。
- docs/asr-accuracy-strategy.md：公开Paraformer与研发对照区分。
- docs/design-reference-index.md：当前设置模块与权限行为。
- docs/product-acceptance.md：过去仪器记录与当前永久禁令区分。
- docs/signing-and-device-acceptance.md：实际签名读取及完整凭据、安装授权边界。
- docs/development-experience/portable-rules.md：旧增量与当前完整经验/项目Skills路由。

其余文件未修改；历史design-qa.md不纳入本轮。以上只是本地工作区产物，未Git提交或推送。
