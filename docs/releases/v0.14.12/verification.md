# v0.14.12正式发布验证

日期：2026-10-17。用户授权打包上传GitHub；复用当日已验签、已保数据覆盖及原任务真实恢复成功的冻结包，不重建、不重签、不再次操作手机。

- 源码提交与v0.14.12注释标签解引用：`242e2cc5ef69c67795259da207097d200bfe52f4`。93项生产/配置摘要与提交文件一致，见[生产清单](../../qa/paraformer-no-speech-tail-20261017/source-manifest.json)。后续README/预览/回执为纯文档提交，不改变产物。
- APK：106883328字节；SHA256 `00f88182a63afbcb6ab53583ccef6b3df8428fb6e436b45e1813300c5a5d6f1e`。包名com.nanzhufeng.transcriber，0.14.12 (27)，debuggable=false；本轮重新验证v3正式签名，证书SHA256 `ee95f8d726b8fb85f15d0eb26d385505cb20a22cf1d749ec3e4327d7ee783174`。本地中文产物南枫转写.apk，远端ASCII名nanfeng-transcriber-android-v0.14.12.apk。
- 原修复阶段98项Release JVM、lintVitalRelease、assembleRelease通过，真实手机原任务恢复并打开1737字正文；本轮只复核既有报告并运行不生成APK的合同/链接/摘要检查，不刷新原构建和设备验证时间。详见[根因及真实验收](../../qa/paraformer-no-speech-tail-20261017/README.md)。
- README两张当前运行预览来自同一冻结包在独立API35/5596中处理公开自造字幕视频，非主设备或私人素材，无收费调用；实例已停止。详见[预览验证](../../qa/readme-v0.14.12/README.md)。
- [正式Release](https://github.com/nanzhufeng/NanfengTranscriber-Android/releases/tag/v0.14.12)：ID415593775，2026-10-17 13:23:18 +08:00公开，draft=false、prerelease=false，目标为上述完整源码提交；远端回执见[github-receipt.json](github-receipt.json)。只有APK与SHA256SUMS.txt，未上传预览图/诊断材料/权重。
- APK资产649599059，state=uploaded、106883328字节、GitHub digest与冻结SHA256一致。校验资产649599070，107字节，digest为sha256:20fd36e45a492aa2c5aa11e325e309ec70332e1516b3a273ef9738578bbf722e，与本地校验文件一致。
- 无鉴权curl从实际公开APK下载地址回拉：HTTP200、106883328字节、SHA256相同，逐字节cmp通过；不是仅以starter声明大小或传出字节判定完成。
- 保留无关design-qa.md；没有卸载、清数据、仪器测试、再转写、变更全局代理或删除旧0.14.10草稿。真实账号最终扣费、其他模型/媒体边界不由本次发布推导。
