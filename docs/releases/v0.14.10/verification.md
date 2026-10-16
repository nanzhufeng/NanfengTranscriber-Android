# v0.14.10 正式覆盖

2026-10-16，WORKTREE，0.14.10 (25)。用户要求原文件核对/转写结果右上角箭头改叉号且功能不变，随后明确授权覆盖安装。

- 两处使用现有Close图标及对应中文可访问名称，onDismiss、触控区域和系统返回不变。与0.14.9的93项生产/配置清单对照，仅版本配置和HistoryScreen变化；与改图标前工作树快照比较，只改两处图标/名称并移除无用import，原保存/自动导出WIP完整保留。
- testReleaseUnitTest、lintVitalRelease、assembleRelease通过；94项JVM，失败/错误/跳过均0。未运行任何仪器测试，未操作模拟器。
- 正式包app/build/outputs/apk/release/南枫转写.apk，106883335字节；SHA256 `98249d498e62d2d35689b2c5e94ee87c0b0eb181ef3ddb7648c3c777313f7bef`。包名com.nanzhufeng.transcriber，debuggable=false；v3验签通过，证书SHA256 `ee95f8d726b8fb85f15d0eb26d385505cb20a22cf1d749ec3e4327d7ee783174`，与现装0.14.9相同。
- 安装前确认没有转写前台服务，保全旧正式包与回拉旧base.apk。指定OPPO推送精确包后以pm install -r --user 0覆盖成功；安装后回拉base.apk与本地逐字节cmp相同，重新验签相同。首装时间、CE/DE目录标识和dataDir均保留；不等同全库/模型逐字节审计。
- 设备临时安装包已删除并验证不存在；本机保留旧包备份。未卸载、清数据或自动启动软件，未进行手机界面/真实转写/云端验收。
- 生产/构建清单见source-manifest.json；冻结后不重建或重签此包。未提交、推送或发布；既有暂存文件与工作区成果保留。
