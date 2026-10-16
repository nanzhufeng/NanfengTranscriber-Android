# v0.14.9 正式覆盖

2026-10-15，WORKTREE，版本0.14.9 (24)。包含正文保存和按结果页记住的选定格式自动导出一次；自动导出参数在任务创建时冻结。功能隔离证据见../../qa/transcript-save-auto-export-20261015/README.md。

- Release JVM94项，0失败/错误/跳过；lintVitalRelease及assembleRelease通过。此前同业务源码Debug94项/lintDebug/构建通过。
- 正式APK：app/build/outputs/apk/release/南枫转写.apk，106883327字节，SHA256 `36791c59b806ac9a496fa5c69a0264ac0c99fbcc937e75e9d64907dbec18c68c`。
- 包名com.nanzhufeng.transcriber，非debuggable；v3验签通过，证书SHA256 `ee95f8d726b8fb85f15d0eb26d385505cb20a22cf1d749ec3e4327d7ee783174`，与OPPO原0.14.8相同。完整App专属用户Gradle属性签名，无环境变量混配。源码文件清单含新增生产文件。
- 用户明确“覆盖安装”，沿用已指定OPPO。推送精确正式包后执行pm install -r --user 0成功，安装后回拉base.apk与上述SHA完全一致。首装时间、ceDataInode、deDataInode及dataDir均不变；这不是私有Room全表或全部模型文件逐字节核验。
- 已移除本次设备临时APK；未自动启动业务，未卸载/清数据/仪器测试，未操作5592。原正式包在本机私有目录保全，未重签旧包。
- 新增AutomaticExportOnce及测试已显式纳入Git暂存，合同整体门禁PASS；其余修改保留工作树，未提交、推送或发布。冻结后未重建该APK。
- 未验证OPPO真实编辑/导出业务、真实云端及OEM长期后台；覆盖安装不替代这些验收。
