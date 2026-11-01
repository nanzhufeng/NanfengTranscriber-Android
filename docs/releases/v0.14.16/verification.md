# v0.14.16 正式包保数据覆盖

2026-10-25 13:33 +08:00；用户明确授权“覆盖安装”。底层HEAD `0e0d7896552566d262c3f94f2c77ea8a0a64bae4`，范围WORKTREE。

- OPPO PKH120：0.14.15 (30) → **0.14.16 (31)**，包名`com.nanzhufeng.transcriber`，non-debuggable；包含外屏灵动岛顶部居中、摄像头/系统图标避让、完整轮廓和展开内容，以及屏幕切换重绘；内屏/横屏沿用原布局。
- `testReleaseUnitTest lintRelease assembleRelease`通过，103项JVM失败/错误/跳过均0；实际JUnit时间2026-10-25 05:29:11–12 UTC。lint 0 Error、27 Warning、6 Information；正式包确认没有临时QA服务或specialUse权限。
- 现装包与交付包证书SHA-256均为`ee95f8d726b8fb85f15d0eb26d385505cb20a22cf1d749ec3e4327d7ee783174`；复用既有App专属签名配置，未导出或打印凭据。
- 冻结APK 106932497字节，SHA-256 `473a37077738c776ed2961009c2a049bbc3e905aa604c8bea0b3ce0accf066ac`；产物相对项目根路径`../交付产物/南枫转写-Android/v0.14.16/南枫转写.apk`；同目录保存安装前0.14.15包作为参考。冻结后未重建或重签。
- 安装前转写服务未运行，包元数据与源码冻结清单无变化；对明确主设备推送唯一临时APK并比对设备临时文件哈希，经`pm install -r --user 0`返回Success。安装后回拉实际APK，与冻结包逐字节一致；临时APK已精确删除并核验不存在。
- 首装时间、数据目录、appId、non-debuggable及enabled_accessibility_services哈希不变。未卸载、清数据、自动启动、改权限、仪器测试或接管主设备无障碍。私有数据库未读取，不宣称全库逐字节审计。
- 几何/交互沿用[10月25日独立模拟器证据](../../qa/island-statusbar-20261025/README.md)。本轮为正式构建和安装核验，未启动主设备转写，ColorOS真实呈现与OEM层级仍待用户实际使用验证；无真实ASR或收费请求。

证据：[产物](artifact.json)、[安装回执](installation-verification.json)、[覆盖前](device-before.json)、[覆盖后](device-after.json)、[签名](signature.txt)、[139项源码清单](source-sha256.txt)。既有WIP和design-qa.md保留；未提交、推送或发布。
