# 下一轮建议提示词

项目当前为未完成品，因转写准确度不足而暂时搁置。除非用户明确要求恢复开发，不主动继续实现、安装或发布。

继续 `/Users/nanzhufeng/Documents/工具开发/NanfengTranscriber-Android`。先读根目录 `AGENTS.md`、`docs/CURRENT_HANDOFF.md`、`docs/domain-rules.md` 和 `docs/product-acceptance.md`。当前候选为 `0.14.0-final-regression (15)`，自动门禁和 API 35 模拟器字幕/四格式真链路已通过；OPPO 未连接 ADB，所以版本 15 尚未真机覆盖。

当前已建立 checkpoint：基线提交 `59ecf86e502e364a43502558bcc385cb451f6e15`，标签 `checkpoint-v15-final-regression` 保留最终回归基线，`paused-development-v15` 保留暂停开发声明。`main` 已跟踪私有仓库 `origin/main`；除非用户明确恢复开发，不得自动继续推送。

真机重连后先只读确认无转写进程/前台服务，拉取已安装 APK 与候选包比对证书，只允许“推到 `/data/local/tmp` + `pm install -r --user 0`”同签名覆盖。不卸载、不清数据、不安装测试辅助包、不触碰 `emulator-5580`。覆盖后验证模型缓存、历史、皮肤、字幕直接提取、四格式真机落盘和 crash/ANR；之后才做系统杀进程恢复、60 分钟和两小时长测。除非用户明确恢复开发，不继续推送或创建 GitHub Release。
