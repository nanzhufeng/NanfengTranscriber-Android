# v0.14.8 本地 checkpoint 最终回归

日期：2026-10-11，Asia/Shanghai。

## 基准与不重复项

- 初始源码/文档 HEAD：5152a3d73626c561124c2fb6aa4546fc5d6497d5；本地代码点 checkpoint-v0.14.8-20261011-code。
- 生产源码与已发布的 v0.14.8 一致；本轮只更新合同、经验资产与开发检查，不改 App、不重新构建/签名、安装或发布。
- 0.14.8 冻结包 SHA-256：4227c045231961f52c5099443a35a38ea7c2220b7dbd0b164c5e9d97ee67ee30，版本与证书在发布记录中另有证据；本轮只是回读复核。
- 已纳入 Git 的核心新增文件不重复创建/复制；历史 design-qa.md 及其临时图片不作为新验证。

## 本轮命令

1. `./gradlew testDebugUnitTest lintDebug --quiet`：PASS，91 项，失败/错误/跳过均为 0；未运行设备仪器测试。
2. `ruby scripts/verify_contract_checkpoint.rb --doc-ref checkpoint-v0.14.8-20261011-code --baseline checkpoint-v0.14.8-20261011-code`：预期 FAIL，捕获原冻结文档的 10 项真实漂移，生产代码一致与核心跟踪检查仍通过。
3. `ruby scripts/verify_contract_checkpoint.rb --baseline checkpoint-v0.14.8-20261011-code --apk 'app/build/outputs/apk/release/南枫转写.apk' --unit-results app/build/test-results/testDebugUnitTest`：PASS，56 项，0 失败；使用同一脚本检查更新后的合同、核心跟踪、链接、JUnit 和冻结包。
4. 官方经验校验：validate_app_experience_distillation.py：PASS，audit/context 结构与分类字段完整，15 个本地链接有效，无占位符或疑似秘密。
5. `ruby -c scripts/verify_contract_checkpoint.rb`、限定范围 Git 空白与暂存秘密检查：通过后才建立本地文档 checkpoint。

## 明确没有重跑

Release 生成、模拟器 ASR/新截图、主设备安装/业务、真实百炼调用、GitHub 推送与发布均未执行；保留各项旧证据的原版本和时间。没有把文档通过当作这些层级再次通过。

## 可追溯入口

[当前验收合同](../../product-acceptance.md)、[经验审计](../../development-experience/audit-20261011.md)、[冻结发布事实](../../releases/v0.14.8-verification.md)、[当前状态](../../PROJECT_STATUS.md)。
