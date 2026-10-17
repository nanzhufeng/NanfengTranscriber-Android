# Paraformer无语音尾段：真实失败与恢复

日期：2026-10-17。源码基线main a2cd595之后的WORKTREE。不是GitHub发布或全模型验收。

最终93项生产/构建配置摘要见[source-manifest.json](source-manifest.json)，对应本次冻结正式包；后续不要重建该冻结包。

## 授权与原始证据

- 用户明确授权同签名正式包保数据覆盖，只重试截图原任务一次，费用上限¥1。主设备只安装已验签正式包；未使用Debug、仪器测试、卸载、清数据或数据库注入。
- 原正式0.14.10 (25)：唯一失败待处理项，Paraformer/自动识别/05:03/98%，进行中与等待均0。原文件名、媒体、正文和设备诊断原件只保留本机临时目录，不进入Git。
- 0.14.11 (26)增加白名单协议字段留存，在同一任务点一次重新开始。真实日志：status=FAILED; provider_code=SUCCESS_WITH_NO_VALID_FRAGMENT。任务与请求编号已在本机留存，不公开。旧代码query将FAILED一概当错误，engine清空恢复记录、整任务失败；300秒前段成功，尾部无有效语音因此阻断归档。
- 官方语义：[百炼错误码](https://help.aliyun.com/zh/model-studio/error-code/)明确该码表示VAD未检出有效语音，并非文件下载或鉴权失败。[北京Paraformer价格](https://help.aliyun.com/zh/model-studio/paraformer-v2)为0.00008元/秒；整段303秒理论原价约¥0.02424，低于授权上限。真实账户最终结算未读取，不能将估算当实扣。

## 最小修复

- Client仅保留白名单错误码、合法UUID任务/请求编号，不保留服务端message、响应正文、签名URL或Key。终态失败私有凭据保留failed_task_id/failure_detail，未知提交保护不变。
- Engine仅对Paraformer且明确SUCCESS_WITH_NO_VALID_FRAGMENT提交空segments与NO_SPEECH远端结果记录，协调器保留已有正文并推进断点；整文件无正文仍由原TranscriptCompletionPolicy进入NO_SPEECH。下载失败、权限错误、未知失败不转成功。
- 兼容0.14.11已保留的无语音失败凭据：在任何上传/提交/轮询前转为空分块缓存，不产生第二次收费重试。其他确认终态失败仍仅在用户主动重试时新建任务。

## 红绿回归与设备验收

- 回执留存测试先因safeTechnicalDetail为空失败；修复后绿。另有一次测试准备阶段Files.readString编译不兼容，改为readAllBytes后才取得有效红灯，不将编译错误当业务红灯。
- 无语音尾段测试先抛FILETRANS_TASK_FAILED；旧回执本地恢复测试先发出多余请求并失败。修复后两者通过，重启/旧凭据恢复均断言没有额外请求；真实下载失败仍为失败。
- 协调器测试通过真实DashScope客户端/引擎边界与MockWebServer验证：尾段无语音时，先前正文逐字保留、processedSamples与断点推进至整段。原整文件可读正文判定测试保持。
- 最终命令：`testReleaseUnitTest lintVitalRelease assembleRelease --quiet`，98项JVM，失败/错误/跳过均0。未运行任何connected Android测试或等价仪器执行。
- 最终0.14.12 (27)：106883328字节，SHA256 `00f88182a63afbcb6ab53583ccef6b3df8428fb6e436b45e1813300c5a5d6f1e`，包名com.nanzhufeng.transcriber，debuggable=false，v3证书SHA256 `ee95f8d726b8fb85f15d0eb26d385505cb20a22cf1d749ec3e4327d7ee783174`。覆盖后回拉base.apk逐字节cmp相同；firstInstallTime仍2026-07-20 01:09:40。CE/DE目录stat被系统拒绝，不声称全数据库或缓存逐字节不变。
- 同一失败项以旧回执本地恢复后从主页队列移出、历史显示已完成；打开该原任务的转写结果，正文1737字符，复制/保存/导出入口可见。仅验证读取，没有自动复制私有正文或额外手动导出。不是改提示或强制改Room状态制造完成。
- 没有扩大到其他模型、收费样本或主设备自动测试；没有提交、推送或GitHub发布。主设备临时安装APK和UI取证XML按精确路径清理，本机保留前包/后包及私有取证。
