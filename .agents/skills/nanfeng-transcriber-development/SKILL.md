---
name: nanfeng-transcriber-development
description: Implement scoped features in NanfengTranscriber-Android with its task, model, result and background owners; use for development in this repository.
---

# 南枫转写开发

本 Skill 只补项目实现路径，不复刻通用产品标准。开始先读根 `AGENTS.md`、状态与本次交接；当前版本不硬编码进流程。

## 执行

1. 读 [领域](../../../docs/domain-rules.md) 和 [所有权](../../../docs/architecture-governance.md)，列本轮目标、禁止项、真实完成条件；只处理已授权增量。
2. 枚举实际入口：文件选择/分享/拖放、单项参数/设置、队列、通知/岛、主页/历史、导出。不存在或不受影响的入口明确排除。
3. 找最早所有者：模型资产走ModelAssetManager/FileModelStore；任务走Room repository；执行走runner/engine；结果走DocumentStore/ExportService；展示用共享快照。不得页面另建状态或直接调用JNI/DAO。
4. 影响持久协议时检查全部Room迁移/schema与旧任务；参数在创建时冻结，恢复键覆盖变化。沿用旧API Key读取边界，不恢复废止的后处理路径。
5. 先定义可观察红灯，在公开seam新增最小行为测试再实现。用本项目 testing Skill选择验证；UI改动先读 [设计来源](../../../docs/design-reference-index.md) 和 [UI合同](../../../docs/ui-contract.md)。
6. 达到本轮完成条件即同步现行合同、决策和状态；不以重构名义扩展全项目，不因实现完成自动安装/发布。

## 停止与交付

需要真实收费调用、主设备变更、签名身份改变或不可逆数据变更时停止请求新授权。报告路径、自动验证与设备/服务缺口；未知不补造。检查流程依据 [完整档案](../../../docs/南枫转写Android完整开发档案.md)。
