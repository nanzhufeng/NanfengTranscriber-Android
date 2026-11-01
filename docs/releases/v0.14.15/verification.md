# v0.14.15 正式包保数据覆盖

2026-10-22 15:49 +08:00；用户授权“覆盖安装”。底层HEAD `0e0d7896552566d262c3f94f2c77ea8a0a64bae4`，WORKTREE；本次包含设置一级入口去除内层灰底、关于信息缩小及源码完整单行适配。

- OPPO PKH120：0.14.14 (29) → **0.14.15 (30)**；包名`com.nanzhufeng.transcriber`，non-debuggable。
- Release构建：`testReleaseUnitTest lintRelease assembleRelease`通过；98项JVM，失败/错误/跳过均0；实际JUnit为2026-10-22 07:46:44–45 UTC，lint为0 Error、27 Warning、6 Information。
- 现装包与正式包签名SHA-256证书相同：`ee95f8d726b8fb85f15d0eb26d385505cb20a22cf1d749ec3e4327d7ee783174`；沿用既有App专属签名配置，无凭据值进入日志。
- 冻结APK：106916099字节，SHA-256 `5c44bc3384f2a56dd7f67a9165f8bfe07dd03a1cee12fcd159049a0d8abe2148`；项目同级路径`../交付产物/南枫转写-Android/v0.14.15/南枫转写.apk`，同目录保留安装前0.14.14包。冻结后未重建或重签。
- 对明确主设备推送唯一临时APK，核对设备文件哈希后执行`pm install -r --user 0`返回Success；安装后回拉实际APK，与冻结包哈希及逐字节相同；临时APK已精确清理并核验不存在。
- 首装时间、数据目录、appId、non-debuggable及enabled_accessibility_services配置哈希不变。未卸载、清数据、自动启动、接管无障碍或运行仪器测试；私有数据库未读取，不能视为全库审计。
- 视觉沿用[10月22日独立模拟器检查](../../qa/settings-entry-about-20261022/README.md)，覆盖普通字体/1.5倍字体；本轮只验证正式包与安装，没有启动主设备验收UI或转写/付费服务。

证据：[产物](artifact.json)、[安装回执](installation-verification.json)、[覆盖前](device-before.json)、[覆盖后](device-after.json)、[签名](signature.txt)、[139项源码清单](source-sha256.txt)。代码与文档尚未提交、推送或发布；既有WIP和design-qa.md保留。
