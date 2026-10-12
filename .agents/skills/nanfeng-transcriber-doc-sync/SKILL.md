---
name: nanfeng-transcriber-doc-sync
description: Synchronize NanfengTranscriber-Android contracts, development archive, decisions and status from code and Git evidence; maintain project Skills without changing runtime behavior.
---

# 南枫转写文档同步

根AGENTS是长期规则；当前状态、交接、历史与流程不得混层。本Skill不自动授权业务修改、记忆写入、提交/推送或发布。

## 执行

1. 核对checkout、分支/HEAD、改动数量和已存在成果，备份精确受改文件；不清理未跟踪历史QA。
2. 需要完整复盘时运行 `ruby .agents/skills/nanfeng-transcriber-doc-sync/scripts/inventory.rb`，先汇总再读细节。脚本只读输出Markdown，使用当前Git跟踪集合；不会将未提交新增文件当已提交基准。它是结构/哈希检查，不是上游源码或JNI内部安全认证。
3. 对照真实所有者：OfficialModelCatalog、Room/schema、TaskTransitionPolicy、runner/导出、岛策略与导航。列冲突位置、当前证据、影响、处理；不能仅因旧注释或文档判定代码行为。
4. 产品/领域/UI/所有权合同各维护唯一正文；关键选择进decision-log；动态事实进PROJECT_STATUS，当前任务接续进CURRENT_HANDOFF。完整档案保存历史演进与证据，不逐次复制动态状态。
5. 可迁移经验按通用/专属/不可照搬分类，保留触发→执行→验证→禁止与来源。项目流程放五类Skills，全球规范只路由；用户未授权时不修改长期记忆或全球Skill。
6. 运行合同脚本，以及 `ruby .agents/skills/nanfeng-transcriber-doc-sync/scripts/validate_assets.rb` 检查本地链接、常见秘密模式和Skill结构；有PyYAML时再运行系统Skill quick_validate，缺依赖如实注明，不能假称官方通过。源码未变时引用旧设备证据的原版本/日期，不刷新其实际测试时间。新脚本实际运行，不能仅校语法。

## 产物与停止

参考 [完整档案](../../../docs/南枫转写Android完整开发档案.md)、[可迁移经验](../../../docs/可迁移开发经验.md)、[本次验证](../../../docs/qa/project-review-20261011/README.md)。报告修改清单、保持不动项、当前未确认问题与备份；无业务改动应核验app/config差异为空。不得为追求状态字段与自身提交SHA完全相等反复提交。
