# v0.14.10 正式覆盖

2026-10-16，WORKTREE，0.14.10 (25)。用户要求原文件核对/转写结果右上角箭头改叉号且功能不变，随后明确授权覆盖安装。

- 两处使用现有Close图标及对应中文可访问名称，onDismiss、触控区域和系统返回不变。与0.14.9的93项生产/配置清单对照，仅版本配置和HistoryScreen变化；与改图标前工作树快照比较，只改两处图标/名称并移除无用import，原保存/自动导出WIP完整保留。
- testReleaseUnitTest、lintVitalRelease、assembleRelease通过；94项JVM，失败/错误/跳过均0。未运行任何仪器测试，未操作模拟器。
- 正式包app/build/outputs/apk/release/南枫转写.apk，106883335字节；SHA256 `98249d498e62d2d35689b2c5e94ee87c0b0eb181ef3ddb7648c3c777313f7bef`。包名com.nanzhufeng.transcriber，debuggable=false；v3验签通过，证书SHA256 `ee95f8d726b8fb85f15d0eb26d385505cb20a22cf1d749ec3e4327d7ee783174`，与现装0.14.9相同。
- 安装前确认没有转写前台服务，保全旧正式包与回拉旧base.apk。指定OPPO推送精确包后以pm install -r --user 0覆盖成功；安装后回拉base.apk与本地逐字节cmp相同，重新验签相同。首装时间、CE/DE目录标识和dataDir均保留；不等同全库/模型逐字节审计。
- 设备临时安装包已删除并验证不存在；本机保留旧包备份。未卸载、清数据或自动启动软件，未进行手机界面/真实转写/云端验收。
- 生产/构建清单见source-manifest.json；冻结后不重建或重签此包。未提交、推送或发布；既有暂存文件与工作区成果保留。

## GitHub交付尝试（2026-10-16，覆盖阶段之后）

- 源码提交5249d34d1ca506c314c1d2bc34cd805433a18cbb已推送main，v0.14.10注释标签解引用至同一提交；远端refs回读一致。28个相关文件显式提交，design-qa.md保留未跟踪。93项冻结生产/配置摘要全部匹配，未重新构建或签名。
- 308项合同/原Release测试报告/冻结APK检查通过；5个Skills、236个本地链接检查通过。当前版本在独立API35模拟器5596用公开自造字幕视频验证两处关闭并更新README截图，实例已结束；不是新增ASR/收费云端/手机业务验收。
- Release ID414785494，tag_name=v0.14.10，draft=true，prerelease=false。校验文件资产646869986：SHA256SUMS.txt，107字节，state=uploaded，GitHub digest=sha256:83b3b4c9369fb1e198be9c91af43739196cfd4dc2903b43708989757818e0937。
- APK上传未成功：先后遇到HTTP408、连接重置及最终180秒超时（传出12976128/106883335字节，约72087字节/秒）。单命令HTTP/1.1、系统现有代理、官方域名解析IP重试均未完成；未改全局网络配置。最终回读草稿只含校验文件，无完整或starter APK，因此未公开Release，不能宣称用户已可下载v0.14.10。
- 冻结中文APK与ASCII暂存包仍保留；恢复时继续使用同一文件、标签和草稿，必须远端大小/摘要一致后再公开。README下载入口暂指向上一公开v0.14.8并注明版本差异。
