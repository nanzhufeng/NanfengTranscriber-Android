# 合同唯一入口与维护索引

本索引只定义文档职责和更新路由，不另写业务规则。当前实现事实以源码及适用版本验证为准；产品意图、实现边界和待验证目标必须分开。具体基线与运行结果由 [PROJECT_STATUS](PROJECT_STATUS.md) 维护。

## 一、全部根级文档登记

以下路径相对仓库根；类型供机器检查使用。CURRENT 是现行合同；GUARD 是长期执行约束；ROUTE 只路由；STATUS 是动态观察；HISTORY/SNAPSHOT 只能证明原日期/版本，不覆盖现行合同。

| 路径 | 类型 | 唯一职责 |
| --- | --- | --- |
| `AGENTS.md` | GUARD | 每轮开发安全边界与能力路由，不写动态状态 |
| `README.md` | ROUTE | 用户介绍、下载、预览与维护入口，不承载内部合同 |
| `THIRD_PARTY_NOTICES.md` | CURRENT | 分发依赖的来源/署名；模型许可限制路由许可合同 |
| `docs/contracts-index.md` | ROUTE | 合同归属和同步规则的唯一入口 |
| `docs/product-brief.md` | CURRENT | 第一用户任务、产品范围，历史重建目标明确标记 |
| `docs/domain-rules.md` | CURRENT | 任务、模型、结果、恢复、删除、导出的真实业务语义 |
| `docs/architecture-governance.md` | CURRENT | 实际源码所有者、依赖边界、兼容能力和架构债务 |
| `docs/ui-contract.md` | CURRENT | 当前入口、布局、展示与交互；不假设全文搜索/编辑持久化 |
| `docs/design-reference-index.md` | CURRENT | 设计来源身份与优先顺序；具体几何只由UI合同维护 |
| `docs/background-island.md` | CURRENT | 后台/岛的平台机制、限制和带版本的设备证据 |
| `docs/asr-accuracy-strategy.md` | CURRENT | 当前ASR路线/协议、测量门禁；建议与实测分列 |
| `docs/model-license-status.md` | CURRENT | 精确模型制品许可及分发门禁；记录原核查时间 |
| `docs/signing-and-device-acceptance.md` | CURRENT | 签名配置、包身份和授权覆盖门禁 |
| `docs/product-acceptance.md` | CURRENT | 功能完成条件与验证范围，详细规则只引用对应所有者 |
| `docs/PROJECT_STATUS.md` | STATUS | 全项目当前能力、实际验证与缺口 |
| `docs/CURRENT_HANDOFF.md` | STATUS | 最新任务接续；下方旧任务明确归为历史 |
| `docs/next-codex-prompt.md` | ROUTE | 安全续接的最短入口，不指定永久暂停/旧候选版本 |
| `docs/decision-log.md` | HISTORY | 决策演变、取舍和废止原因；不能据早期条目恢复旧行为 |
| `docs/南枫转写Android完整开发档案.md` | SNAPSHOT | 2026-10-11完整复盘；后续合同变化仅添加路由，不重写旧检查 |
| `docs/可迁移开发经验.md` | SNAPSHOT | 经验来源与适用边界，不是本项目第二套业务合同 |

## 二、其余文档的适用范围

- `docs/evidence/`、`docs/qa/`、`docs/releases/`：版本化证据/审计，不是新的执行授权或永久规范。原报告不改日期、测试数或失败结果。
- `docs/development-experience/`：2026-10-11增量审计/context/模板，若与现行合同不同，以本索引路由的正文为准；旧模板不得成为另一个所有者。
- `.agents/skills/`：项目操作流程，不定义新业务行为；通用方法继续使用已有通用Skills。Skill检查脚本是开发资产，不是App运行时依赖。
- `design-qa.md`：未跟踪历史草稿，保留但不作为现行合同。供应商子模块的AGENTS/README/许可只作用于其依赖范围，不覆盖产品规则。
- SDK/Gradle/Manifest/Room schemas：配置事实由实际文件定义，文档摘要不能替代配置；不将依赖存在解释成测试授权。

## 三、变更时只更新所属正文

| 改动概念 | 必须同步 | 必须确认的消费路径 |
| --- | --- | --- |
| 模型或云端协议 | domain、architecture、asr策略；涉及许可再更新license/NOTICES | Catalog→设置/单项参数→Factory→runner→调用账本 |
| 任务状态/恢复/资源 | domain、architecture、后台合同；验收新增对应证据 | Room/schema→runner/queue→通知/岛→主页/历史 |
| 编辑/搜索/结果/输出 | domain、UI、acceptance的边界与验证 | 原JSON/TXT→内存草稿/短预览→复制→TXT/MD/DOCX或原时间轴SRT→SAF |
| 导航/皮肤/布局 | UI合同；仅来源改变才更新design-reference | 窄/宽窗口、根路由、历史来路、视频会话、安全区 |
| 签名/安装/发布 | signing合同、release记录、状态；配置需单独验证 | 本地包→证书/哈希→授权安装→回读→远端附件digest |

没有真实变更的合同不为“统一日期”刷新；不在状态、档案、索引和Skill再次复制整套数值。新根级文档必须登记职责；同一规则出现多个正文时，保留有证据的唯一所有者，其余改为路由或明确历史。

## 四、冲突与技术债处理

1. 写清旧文档原句、实际源码/测试和影响入口，不凭过时注释推断行为。
2. 区分“文档错误”“实现限制”“待验证需求”。本轮只更正文档；全文搜索、编辑保存、远端取消、跨介质事务等不能靠改合同变成已实现功能，也不能因如实记录而免除未来验收。
3. 更新所属正文，历史决策标记被哪个现行规则替代；旧QA保持原版本，不删独有事实。
4. 执行合同门禁及本地链接/Skill检查。语义检查用于发现漂移，不代替真实功能、设备或供应商验证。
5. 变更源码时才重新选择相应行为回归与构建；纯合同整理复核源码与冻结包不变，不重打/重签/安装。

开发检查统一使用 [verify_contract_checkpoint.rb](../scripts/verify_contract_checkpoint.rb)；本轮冲突、实际检查和未动范围见 [合同统一审计](qa/contracts-20261012/README.md)。
