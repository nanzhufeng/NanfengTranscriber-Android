# 南枫转写 Android 开发契约

- 开始先核对 checkout、工作区及 `docs/PROJECT_STATUS.md`、`docs/CURRENT_HANDOFF.md`；保留既有未提交成果，不擅自 reset、clean、stash 或删除。
- 事实以当前代码及适用版本验证为准。所有合同先按 `docs/contracts-index.md` 找唯一正文；冲突必须记录，Windows、参考 App、历史决策和复盘快照不是本项目当前真相。
- 本项目运行时保持原生 Android；不引入 Python、PySide6 或 Chaquopy。仓库开发检查脚本不属于 App 运行时。
- 用户可见提示使用中文。任务状态由持久化领域事实决定，失败/等待/取消保留真实原因与恢复入口；结果保存成功前不得声明完成。
- UI、分享、拖放、后台、通知和历史遵循同一业务所有者。参考设计先读 `docs/design-reference-index.md`，六皮肤共用页面树与语义，不复制业务逻辑。
- 模型经长度、SHA-256 校验与临时文件提交才能 READY；进程重启、同签名升级不能清缓存。模型许可与代码许可分开核对，禁止未经许可再分发权重。
- 任务参数和断点匹配后才恢复；资源由队列生命周期统一释放。不得将前台服务、悬浮窗或单次样本通过解释成无限后台保证。
- 私有结果与外部导出分离，外部导出按领域合同执行，只写用户已授权的 SAF 目录。删除任务不能删除原媒体、模型或已导出文件；迁移不清库，保留旧任务兼容。
- 每任务只用所选 ASR，不恢复已废止的自动云端润色。收费验证须取得本轮明确授权和成本范围；密钥只经 Keystore 安全存储，不进 Git、日志、截图、备份或结果。
- 永久禁止所有环境的 `connected*AndroidTest` 及绕过式仪器执行。主设备不安装 Debug、不自动部署、不卸载或清数据；只有明确授权后才以已验签正式包同签名 `pm install -r --user 0` 保数据覆盖，不自动启动。
- 正式签名使用完整环境变量，或用户级 Gradle 的 App 专属属性；不得索取/打印口令或换签名绕过安装。APK 在 Gradle 中直接命名为 `南枫转写.apk`；冻结交付后不为文档任务重建或重签。
- 测试、编译、模拟器、真机业务、真实服务、安装与发布分别报告版本/时间/范围。动态状态只写状态与交接，不写进本文件；完整历史见 `docs/南枫转写Android完整开发档案.md`。

## 项目 Skills 路由

按当前任务读取 `.agents/skills/` 对应 `SKILL.md`：开发 `nanfeng-transcriber-development`；排错 `nanfeng-transcriber-debugging`；测试 `nanfeng-transcriber-testing`；审查 `nanfeng-transcriber-code-review`；文档同步 `nanfeng-transcriber-doc-sync`。它们只补本项目路径与判断，通用规范不重复维护。
