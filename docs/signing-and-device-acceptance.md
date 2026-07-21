# 签名与真机验收

## 当前状态

已为 `com.nanzhufeng.transcriber` 建立独立正式签名。Find N5 只安装该签名产生的 Release APK；Debug 签名仍只用于模拟器。

## 正式方案

正式签名满足：

- keystore、密码和恢复信息不进入 Git、APK、README 或普通日志。
- 主 keystore 位于仓库外的本机应用支持目录；同一加密 keystore 备份到 iCloud Drive。
- 密码保存在 macOS 钥匙串；仓库内的一键脚本只把密码复制到剪贴板，不显示、不落盘。
- Gradle 按“跨平台环境变量 → 用户级 Gradle 属性 → macOS 钥匙串”顺序读取签名信息。
- 首次安装前记录证书 SHA-256；之后所有验收和发布 APK 必须保持一致。
- 丢失正式签名意味着无法同签名覆盖升级，也就无法保证 App 私有模型缓存继续使用。

## 位置与跨平台调用

- 本机主文件：`~/Library/Application Support/NanzhufengSigning/NanfengTranscriber-Android/nanfeng-transcriber-release.jks`
- iCloud 备份：`iCloud Drive/南枫转写-Android-签名备份/nanfeng-transcriber-release.jks`
- 一键复制密码：双击仓库内 `scripts/复制南枫转写签名密码.command`
- 固定别名：`nanfeng-transcriber`

其他电脑或 CI 只需配置一次：

```text
NANFENG_TRANSCRIBER_KEYSTORE=<nanfeng-transcriber-release.jks 的绝对路径>
NANFENG_TRANSCRIBER_KEYSTORE_PASSWORD=<一键复制得到的密码>
```

本地平台也可改用用户级 Gradle 属性 `nanfengTranscriber.keystore` 与
`nanfengTranscriber.storePassword`。两种配置都不得写入仓库；CI 应使用平台的 Secret 管理。

## 已冻结的签名身份

- 证书主题：`CN=Nanfeng Transcriber, O=Nanzhufeng, C=CN`
- 算法：RSA 4096 / SHA256withRSA
- 证书 SHA-256：`ee95f8d726b8fb85f15d0eb26d385505cb20a22cf1d749ec3e4327d7ee783174`
- keystore SHA-256：`dc1aaa9a84e0310b8eb22baa8540bc2ab102c5837b27b78618b27d1920ac0503`

2026-07-20 已用同一证书完成 Find N5 的版本 2 首次安装与版本 3 覆盖升级；临时 APK 均已从设备清理。

## OPPO 安装门禁

1. 自动测试、Lint、Debug/Release 构建通过。
2. 核对包名、版本、正式证书 SHA-256 与当前安装状态。
3. 将 APK 推送到设备临时目录。
4. 执行 `pm install -r --user 0 <设备临时 APK>`。
5. 安装前后记录模型/任务数据指纹；不使用直接 `adb install`。
6. 签名不一致、ColorOS 要求图形确认或可能清数据时停止，不卸载、不清数据。
