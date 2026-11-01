# v0.14.14 正式包保数据覆盖

2026-10-20 22:53 +08:00；底层HEAD `0e0d7896552566d262c3f94f2c77ea8a0a64bae4`，范围WORKTREE。用户明确授权“覆盖安装”；只升级OPPO PKH120的现有主用户安装，不卸载、不清数据、不自动启动、不发布。

- 版本：0.14.13 (28) → **0.14.14 (29)**；包名 `com.nanzhufeng.transcriber`，non-debuggable。
- 签名：现装与交付包SHA-256证书均为 `ee95f8d726b8fb85f15d0eb26d385505cb20a22cf1d749ec3e4327d7ee783174`；签名配置使用既有用户级App专属Gradle属性，无敏感值进入记录。
- 自动验证：`testReleaseUnitTest lintRelease assembleRelease`通过；98项JVM，失败/错误/跳过均0，JUnit时间为2026-10-20 14:45:08–09 UTC；lint 0 Error、27 Warning、6 Information。构建完成后冻结包，不再重建或重签。
- APK：106916116字节；SHA-256 `ab66af8d66ee89172f32046cea42a8e44abe7eb1fc1e1cabf54f202d67303a2e`。
- 产物：`../交付产物/南枫转写-Android/v0.14.14/南枫转写.apk`；同目录保存安装前0.14.13包用于参考，未执行降级。
- 安装：明确设备推送至本次唯一临时APK，校验设备临时文件哈希后执行 `pm install -r --user 0`，返回Success；安装后回拉实际APK，哈希和逐字节比对均一致；临时APK已精确删除并核验不存在。
- 数据/配置边界：首装时间、数据目录、appId、non-debuggable边界及enabled_accessibility_services的哈希不变；没有读取私有数据库，不能将这些元数据核对宣称为全库逐字节审计。
- UI：沿用[本轮Debug/隔离模拟器证据](../../qa/ui-surfaces-20261020/README.md)，版本升级仅改变版本号和构建时间；本轮Release未启动主设备作UI或转写业务验收，无真实服务/收费调用。

证据：[产物](artifact.json)、[安装回执](installation-verification.json)、[覆盖前](device-before.json)、[覆盖后](device-after.json)、[签名](signature.txt)、[源码冻结清单](source-sha256.txt)。本地安装完成，尚未提交、推送或发布；既有工作区成果与design-qa.md保留。
