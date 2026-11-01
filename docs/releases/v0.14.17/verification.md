# 0.14.17正式包：状态栏置顶修复

2026-10-29，用户提供0.14.16严重遮挡截图并要求置顶。本包修复窗口层级：用户手动开启独立置顶服务后使用高于状态栏的悬浮层，仍按摄像头左右分栏；未开启时普通窗口使用状态栏下方安全区。任务状态、ASR、通知和内屏原布局不另建实现。

- 包名`com.nanzhufeng.transcriber`，版本0.14.17 (32)，non-debuggable，106950608字节。
- 冻结产物：`../交付产物/南枫转写-Android/v0.14.17/南枫转写.apk`（相对仓库根目录）。
- SHA-256 `068cd1d903a9efcec7294623b2d214cfb808d88c177c9aedce283d00aee868f9`。
- 正式证书SHA-256 `ee95f8d726b8fb85f15d0eb26d385505cb20a22cf1d749ec3e4327d7ee783174`，与0.14.16本地交付记录一致；本轮主设备未连接，未声称现场验签。
- `testReleaseUnitTest lintRelease assembleRelease`通过：104项JVM，失败/错误/跳过均0；lint 28 Warning、6 Information、0 Error。较前版多1条UnusedAttribute：isAccessibilityTool仅API31+使用，API29/30忽略该false标记；无屏幕/手势能力仍由独立capability配置限制，不影响构建。未为此弱化检查。
- [141项源码冻结清单](source-sha256.txt)、[签名](signature.txt)、[最终Manifest](manifest.txt)、[单测结果](unit-results/)、[构建日志](release-build.log)、[lint结果](lint-release.xml)、[元数据](artifact.json)。正式包无临时QA服务或specialUse权限。
- 隔离模拟器置顶层级/直接点击/关闭重连/大字体/折叠屏及设置入口见[QA证据](../../qa/island-topmost-20261029/README.md)。

安装：PASS（2026-10-29）。用户连接OPPO PKH120并明确要求覆盖，现装0.14.16 (31)包回拉哈希与前版冻结交付一致，证书匹配，本轮无活动转写服务。推送冻结包后使用`pm install -r --user 0`返回Success，升级为0.14.17 (32)，回拉APK与冻结包逐字节一致；首装时间、数据目录、appId、non-debuggable与无障碍服务配置哈希均未变。精确临时APK已清理并验证不存在；安装前正式包仅在交付目录留作备份，未执行降级。

[覆盖回执](installation-verification.json)、[安装前元数据](device-before.json)、[安装后元数据](device-after.json)、[安装结果](install-result.txt)。本轮未卸载、清数据、自动启动、修改无障碍/其他权限或读取私有数据库；不将安装元数据不变宣称为全数据库逐字节审计。

状态栏置顶仍需用户在“设置→灵动岛→开启状态栏置顶”打开系统页，手动启用“南枫转写灵动岛置顶”。ColorOS实际显示尚未复验；无ASR/收费调用、提交、推送或远端发布。只更新安装证据，不重建/重签冻结包。

## 2026-11-01 GitHub正式发布准备

141项源码摘要与冻结清单一致。原本地交付APK已被清理，本轮从OPPO只读回拉现装0.14.17，SHA-256/字节数与冻结证据一致，恢复为同一份正式包；未重建或重签。重新执行Release JVM/lint通过（104项失败/错误/跳过均0）；同包在隔离模拟器运行三页[README截图](../../qa/readme-v0.14.17/README.md)。未覆盖、启动主设备、改权限或执行收费调用。新增归档日志中的本机路径已脱敏，原始证据在仓库外保留。远端发布待本轮回读确认。

## GitHub正式发布完成（2026-11-01）

[v0.14.17正式Release](https://github.com/nanzhufeng/NanfengTranscriber-Android/releases/tag/v0.14.17)已公开且为latest，非草稿/预发布；标签指向a8fbfae01b13e9ca04355be7ea856d24c41ab7ae，141项生产/构建文件与冻结源码逐项相符。源码及README预览已推送main。仅附用户安装APK，没有额外校验附件。

附件nanfeng-transcriber-android-v0.14.17.apk为106950608字节、state=uploaded；GitHub digest与原冻结SHA-256一致。匿名公开下载HTTP200，回拉106950608字节，SHA-256与本地冻结包相同并逐字节一致。[远端核验](publication-verification.json)。上传首次EOF后保留草稿并清理不完整附件，最终HTTP/1.1无显式代理传输成功；没有修改用户代理设置。发布未触发设备安装或真实ASR/收费调用。ColorOS置顶实际效果仍为未复验。
