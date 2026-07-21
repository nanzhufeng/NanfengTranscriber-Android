# 下一轮建议提示词

继续 `/Users/nanzhufeng/Documents/工具开发/NanfengTranscriber-Android`。先读根目录 `AGENTS.md`、`docs/CURRENT_HANDOFF.md`、`docs/domain-rules.md` 和 `docs/product-acceptance.md`。当前候选为 `0.14.0-final-regression (15)`，自动门禁和 API 35 模拟器字幕/四格式真链路已通过；OPPO 未连接 ADB，所以版本 15 尚未真机覆盖。

当前已建立本地 checkpoint：基线提交 `59ecf86e502e364a43502558bcc385cb451f6e15`，最终文档交接由标签 `checkpoint-v15-final-regression` 定位。无 upstream，不得自动推送。

真机重连后先只读确认无转写进程/前台服务，拉取已安装 APK 与候选包比对证书，只允许“推到 `/data/local/tmp` + `pm install -r --user 0`”同签名覆盖。不卸载、不清数据、不安装测试辅助包、不触碰 `emulator-5580`。覆盖后验证模型缓存、历史、皮肤、字幕直接提取、四格式真机落盘和 crash/ANR；之后才做系统杀进程恢复、60 分钟和两小时长测。GitHub 上传继续暂停。
