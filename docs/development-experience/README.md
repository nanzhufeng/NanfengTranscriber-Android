# 南枫转写 Android 增量开发档案

本目录是2026-10-11增量记录，不覆盖随后现行合同；正文由 [合同索引](../contracts-index.md) 路由。测试数量/日期与当时未创建全球Skill等均是历史事实。

## 最终基准

- 生产代码：v0.14.8 标签 `290e03f93fe6e9df43c243d3a04d2839ba9720be`；本轮开始 HEAD `5152a3d73626c561124c2fb6aa4546fc5d6497d5` 仅增加发布收尾文档，生产代码相同。
- 本地代码 checkpoint：`checkpoint-v0.14.8-20261011-code`；本轮合同与回归资产随后单独提交，不改业务逻辑。
- APK、签名和 GitHub 发布回读事实只由 [发布验证](../releases/v0.14.8-verification.md) 维护；状态与缺口只由 [PROJECT_STATUS](../PROJECT_STATUS.md) 维护。

## 本次增量

2026-10-11：补正式经验审计、可迁移规则、验收模板和可执行合同检查，纠正旧 Whisper 档位、Room v4、固定五分钟块、独立字幕状态和内屏左栏等陈旧正文。此前完成的核心源码入 Git、上传和覆盖安装均不重复执行。

## 持久资产

- [项目 context](context.md)：身份、技术栈、唯一所有者、构建与数据安全。
- [经验审计](audit-20261011.md)：反馈—根因—实现—验证矩阵、A–E 分类与失败边界。
- [可迁移规则与模板](portable-rules.md)：触发、执行、验证、禁止项；不复制项目像素和服务名为全球标准。
- [本轮最终回归](../qa/final-regression-v0148/README.md)：本轮实际命令、源码与冻结包边界。
- [合同机器检查](../../scripts/verify_contract_checkpoint.rb)：检查状态/Schema、现行模型合同、核心文件 Git 跟踪、链接及可选 JUnit/冻结 APK。
- 项目唯一规范仍是 [UI 合同](../ui-contract.md)、[领域规则](../domain-rules.md)、[验收合同](../product-acceptance.md)、[架构治理](../architecture-governance.md) 与 [决策记录](../decision-log.md)，本档案不制造第二份竞争正文。

## 保持不动

不新建重复的全局 Skill；通用职责继续复用产品标准和工作流 Skill。本次没有明确长期记忆写入授权，因此不更新记忆。根目录 design-qa.md 依赖临时图片，仅作为历史材料原样保留，不纳入当前正式基准。
