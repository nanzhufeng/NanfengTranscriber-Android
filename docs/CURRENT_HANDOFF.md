# 当前交接

更新日期：2026-08-30

## 结论

当前工作树已按用户指定架构收敛，尚未发布：

1. **本地标准模式：SenseVoiceSmall INT8 + sherpa-onnx**。
2. **高精度模式：Qwen3-ASR API**，作为直接转写引擎，不是最终润色。

设置页取消服务地址、自由模型名和“翻译润色”入口；模型改为“本地标准 / 高精度 Qwen”两个简洁单选项，切换立即生效、不弹确认框。高精度详情仅保留“转写模型：Qwen3-ASR”和 API Key 操作；Key 只通过 Android Keystore 保存，已保存时在输入框显示固定长度掩码，用户点眼睛后才解密显示。

## 已改动的事实

- `OfficialModelCatalog` 只提供本地 SenseVoice 与高精度 Qwen 两项用户可见选择，Whisper 不再出现在设置或单项转写选择面。
- `Qwen3AsrApiEngine` 通过固定千问 Qwen3-ASR API 直接处理 PCM 音频分段；请求体严格使用官方单个 `input_audio` 内容项，不附加文本提示。缺少 Key 会在创建任务前拦截，旧任务在运行器中仍有同一失败保护；鉴权、限流、网络、HTTP 与响应格式错误保留为安全的中文任务原因。API 模型不下载、导入、导出或伪装成本地模型缓存。
- Qwen 后台仍按音频分段请求，但调用账本只在完整源文件转写成功或最终失败时写一条：按文件名聚合显示后台分段数、总耗时、服务端提供的 Token 与安全错误码；重试也在同一文件项汇总。费用采用南枫 AI 的版本化本机估算账本方式，按实际送出的音频时长以中国区 `Qwen3-ASR-Flash` 公开价 `¥0.00022/秒` 计算为人民币微元；价格版本、币种与估算金额一起固化，免费额度、促销和服务商结算不伪装成已扣费。设置首页仅在高精度详情下显示“调用记录”入口，点击后进入独立全屏记录页；不保存 API Key、音频、文件 URI、请求载荷或转写正文。
- `TranscriptionTaskRunner` 已彻底停用旧转写后文字润色执行链；历史 Room 字段仅为兼容读取保留。
- 设置把“转写模型”与“当前模型详情”拆为两张卡：前者只保留本地标准和高精度 Qwen 两项单选；后者随选择切换为本地缓存操作或 Qwen 高精度服务，绝不同时常驻。其余为转写参数、输出与文件、皮肤、隐私与诊断；已移除混乱的功能审阅卡。隐私文字按当前选中的本地/云端模型如实变化。
- 结果页的 TXT、MD、SRT、DOCX 格式按钮均直接按“设置 → 默认输出目录”和既有重名策略写入；不再触发文件选择器或确认弹窗。按钮下方即时显示“正在导出”、成功保存、未设目录或失败原因；未设置默认目录时不创建临时文件。
- 默认输出目录的 SAF 写入使用树根对应的 document URI，而非将 raw tree URI 直接传给 `DocumentsContract.createDocument`；导出文件直接落在用户选定目录根部，不再先创建按任务命名的子目录。此修复规避 ColorOS 对 parent URI 的严格校验；失败反馈会按权限失效、目录暂不可用或具体 I/O 原因区分，而不再笼统提示“目录有问题”。
- 历史的“转写结果”改为覆盖历史列表的全屏结果页，不再使用居中弹窗；系统返回键和“返回历史”只关闭该结果页，编辑、复制和格式导出链路保持原样。
- 单项转写设置只保留模型选择，不再显示语言、默认输出格式或缓存提示；高精度 API Key 卡移除底部红色删除操作。
- 历史页复用南枫下载的用户流程：单一时间筛选入口、批量选择删除，以及仅清理“结果已失效”的记录；所有历史删除都只移除 App 内转写结果和记录，绝不删除原音视频、模型或导出文件。视频核对不再挂接 Android `MediaController`，而是以固定 16:9 的裁剪预览框承载应用内播放、进度和时长控制，控件不会越出预览画面。媒体预览层同时具备图片缩略图与全屏本地查看代码，当前文件导入范围仍保持音频/视频，未伪造图片转写能力。
- 历史卡片的橙色转写摘要固定为一行并省略超长内容；左侧完成标记、时间与连接线以右侧卡片垂直中心对齐，避免卡片高度随内容变化时标记贴在顶部。
- 历史搜索框为白底胶囊形输入面，保留搜索图标和现有筛选逻辑。
- 视频核对会话按南枫下载的根层播放器合同处理：应用根层以 `rememberSaveable` 保存当前任务、播放位置与用户播放意图，历史页不再拥有该会话。内外屏／外接显示造成 Compose 或 Activity 配置重建时，旧 `VideoView` 销毁前回写快照，新视图按相同 URI、位置与播放态恢复；销毁瞬间的 `isPlaying=false` 不会覆盖用户已选择的“继续播放”意图。
- 视频核对页标题区使用状态栏安全边距，不再与系统状态图标重叠。视频默认显示中央播放/暂停键和底部进度条；单击视频空白区显示或隐藏两组控件，双击直接暂停/继续并显示控件。该手势只改变播放器控制层，不影响根层续播会话。
- Provider、分块、解码、连接、转写耗时等诊断摘要不再出现在视频核对或文字结果阅读页；每个历史记录有该摘要时，仅在其三点菜单提供“转写信息”，点击后以按文件归属的弹层展示。
- 结果页导出区收敛为一个可展开的格式选框和右侧“导出”按钮：选择格式只更新选择，不会立即写入；点击“导出”才按该格式写入已设置的默认目录。原先常驻的 SRT 说明文字已移除。
- SenseVoice 权重不随 APK、Git 或 Release 再分发；精确 ONNX 制品许可未完成，详见 [`model-license-status.md`](model-license-status.md) 和 [`../THIRD_PARTY_NOTICES.md`](../THIRD_PARTY_NOTICES.md)。

## 当前验证边界

- 已执行：`testDebugUnitTest`、`lintDebug`、`lintVitalRelease`、`assembleDebug`、`assembleRelease`；均通过。API 35 隔离模拟器 `emulator-5554`（`1140 × 2616 / 442dpi`）已验收两项模型单选、Qwen 直接切换，以及本地缓存/Qwen 服务在同一详情位的互斥切换；另验证 API Key 保存、重启后的掩码回填、点眼睛解密显示与删除测试凭据。另有 JVM 契约覆盖 Qwen 官方单音频请求体与安全 HTTP 错误映射。截图/XML 证据在 [`qa/model-architecture-v16/`](qa/model-architecture-v16/)。
- 本轮已重跑 `testDebugUnitTest`（54 项）、`lintDebug`、`assembleDebug`、`assembleRelease`，均通过。仅覆盖安装 API 35 隔离模拟器 `emulator-5554`，视觉确认“调用记录”入口和全屏空态可达；未发起真实 Qwen 请求。JVM 覆盖分段聚合为单文件记录以及 60 秒音频 `≈ ¥0.0132` 的版本化估算。
- 最新一轮已执行 `testDebugUnitTest`（54 项）、`lintDebug` 与 `assembleDebug`，均通过；仅以 `adb -s emulator-5554 install -r` 覆盖 API 35 隔离模拟器。使用历史中已有的 8 秒 MP4 打开“原文件核对”，截图/XML 确认 16:9 视频框为 `[88,331][1051,873]`，播放控件、进度条与 `00:02 / 00:08` 时长均在该框内；未触碰 OPPO，未运行 instrumentation。
- 导出修复后已执行 `testDebugUnitTest`（54 项）、`lintDebug`、`assembleDebug` 与 `assembleRelease`，均通过。API 35 隔离模拟器 `emulator-5554` 经系统文件夹选择器授权 `Documents` 后，从已有历史结果手动导出 TXT，UI 显示“TXT 已保存到默认输出目录”，并回读确认 `/sdcard/Documents/nanfeng-final-subtitle-test.txt` 实际存在（108 B）。此证据覆盖共享的 `AndroidTranscriptOutputStore` 写入链路；尚未覆盖到 OPPO。
- 播放会话修复后已执行 `testDebugUnitTest`（54 项）、`lintDebug`、`assembleDebug` 与 `assembleRelease`，均通过。API 35 隔离模拟器播放已有 8 秒 MP4 后，强制经历纵横配置切换；返回稳定布局后仍为同一文件，控件为“暂停”（代表仍在播放），进度为 `00:06 / 00:08`，没有回到 `00:00`。此为配置重建回归，尚不等同于 OPPO Find N5 实际内外屏切换验收。
- 2026-08-30 16:25 已按用户明确授权，将最新正式签名包以 `adb push` + `pm install -r --user 0` 同签名覆盖 OPPO `3B157F009E800000`。预检确认包名 `com.nanzhufeng.transcriber`、版本 `15`、Release `debuggable=false` 与证书 SHA-256 一致；安装后回读 base APK 的 SHA-256 与本机产物一致：`3d6522a1f2f2336a5604a8d21d2b2e71510dcc5c714b3573ba86c39cadb030a4`。`firstInstallTime` 仍为 `2026-07-20 01:09:40`；未卸载、未清数据、未运行 instrumentation，也未进入业务路径。
- 2026-08-30 16:52 已按用户明确授权覆盖包含默认输出目录修复的 Release。仍以 `adb push` + `pm install -r --user 0` 完成；预检证书一致、`debuggable=false`，安装后回读 base APK 与本机哈希一致：`4bd44df913063dafd2ccf411e7f30bbbbc26d597a5a6069862974193eba40fea`。`firstInstallTime=2026-07-20 01:09:40` 保持不变；未卸载、未清数据、未做 OPPO 业务操作或 instrumentation。真实 OPPO 目录写入仍需用户自行点一次导出确认，因为没有代替用户在其实际目录创建文件。
- 2026-08-30 17:16 已按用户明确授权覆盖包含内外屏视频续播修复的 Release。继续使用 `adb push` + `pm install -r --user 0`；预检证书一致、`debuggable=false`，安装后回读 base APK 与本机哈希一致：`7c67c0cdbfcfb35a354899add11b614917264d797ada87f99550837878732055`。`firstInstallTime=2026-07-20 01:09:40` 保持不变；未卸载、未清数据、未做 OPPO 业务操作或 instrumentation。OPPO 实际内外屏切换续播仍待用户从正在播放的视频手动切换屏幕确认。
- 已执行 OPPO Find N5（序列号 `3B157F009E800000`）同签名保数据覆盖：先从 `0.13.0-media-card-subtitles (14)` 升至 `0.14.0-final-regression (15)`，再以包含当前模型设置重组的最新 Release 覆盖。均使用 `/data/local/tmp` + `pm install -r --user 0`，未使用 `adb install`、未卸载、未清数据；`firstInstallTime=2026-07-20 01:09:40` 始终不变。最近一次覆盖（2026-08-30 15:41）包含调用记录入口、Qwen 调用审计、Whisper 选择移除及单项转写精简；设备 base APK SHA-256 与本地 Release 一致：`4f5926c1b2f89aed1f3bfeeae1986bce858597fa75d2c6b60a378e6889628339`。这只证明覆盖和安装包一致，不构成业务链路验收。
- 未执行：任何 `connected*AndroidTest`、真实 Qwen API 请求、OPPO 业务操作或自动化测试、GitHub 上传/Release。
- Qwen 真实服务验收需要用户自有 API Key 与明确同意外发对应音频；没有该证据时，只能称为请求实现与本地构建验证完成。

## 未通过或尚待验证

- SenseVoice 与 Qwen3-ASR API 在同一真实中英混合素材上的 CER/WER、术语保留率、速度、内存、温度、断点恢复与两小时稳定性。
- Qwen 的真实凭据、鉴权、限流、计费与实际转写质量；不得用模拟响应或空 Key 代替。
- OPPO Find N5 的历史/模型缓存可读、系统杀进程恢复、60 分钟和两小时长测；本次仅核对首装时间与已安装 base APK 一致性，未进入业务路径。

## 操作禁令

- 永久禁止 `connected*AndroidTest`，不得以任何设备或环境变量绕过。
- OPPO 已按用户授权完成一次同签名保数据覆盖；后续仍禁止卸载、清数据、数据库注入、自动化部署或测试，除非用户再次明确授权。
- 不提交、不推送、不上传 GitHub、不创建 Release。
- 不得把未完成许可的 SenseVoice 权重放进 APK、Git、测试样例或附件。

## 下一步（仅在用户明确授权后）

1. 用同素材分别跑本地标准与高精度 Qwen，分开记录本地/云端证据。
2. 用户提供并允许使用自己的千问 API Key 后，做一次最小真实 Qwen 转写并展示网络与费用边界。
3. 用户另行允许时，在 OPPO 从真实文件导入到结果保存完成最小业务链路，再做系统恢复与长测；仍不运行 instrumentation。
