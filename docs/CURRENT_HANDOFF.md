# 当前交接

更新日期：2026-10-12

## 最新授权：两轮资料纳入Git并推送checkpoint

- 用户明确授权只提交完整复盘/合同统一两轮的文档、AGENTS、五类项目Skills及开发检查脚本，推送main并建立注释标签 `checkpoint-docs-contracts-skills-20261012`；不改App源码、测试、构建配置或冻结安装包，不更新Release。
- 提交前核验：底层2bad9c2，远端main为5152a3d，仅领先一个既有纯文档提交；当前暂存范围计划36个资料文件，design-qa.md排除且保留。合同306项、五类Skills/233链接通过，冻结APK摘要不变。
- 本节为提交前快照，不冒称已经推送；最终成功以main及注释标签的GitHub ref回读为准。当前业务基线仍为既有验证源码，不为了写入自身SHA重复提交。
- 后续只按用户新任务行动；历史任务中的“未提交/未推送”属于原阶段事实，不覆盖此次明确授权。

## 最新任务：全部合同统一（功能不变）

- 现行合同唯一入口docs/contracts-index.md；业务语义domain，呈现UI，实际所有权architecture，证据/缺口PROJECT_STATUS；其余根级合同均已登记。下方所有旧任务记录都是原日期历史，不是当前授权或待执行路线。
- 本轮核对并同步：文件名/摘要而非全文搜索、内存编辑草稿、云端Key先于字幕、私有目录与SAF直写分离、SKIP无新写入、provider/路径/线程会话键、取消不撤销远端、局部去重/历史筛选边界、设置多所有者、分层债务与未实现降噪；不改App功能。
- 开发门禁扩展到全部根级文档职责/链接及边界；开始文档快照预期9项FAIL，当前306项PASS；五类Skill/233链接、语法/空白及两项参数负向测试PASS。没有重跑JVM/lint，既有91项报告原日期保留。
- 2bad9c2基线与冻结包未变；未改源码/测试/Gradle、设备/权限/收费接口、Git提交/推送/Release、全球Skills/记忆；既有未提交档案/Skills与design-qa.md保留。细节见docs/qa/contracts-20261012/README.md。
- 上下文闸门HANDOFF，当前范围收口，不扩展新功能；下一步仅按用户新请求读取AGENTS、合同索引和状态，不重演旧暂停或v15自动验收路线。

以下为历史任务记录（原结果/时间保留）。

## 本次任务收尾：完整项目复盘（未提交的文档增量）

- 目标：四类正式资产；根AGENTS长期规则、完整开发档案、五类项目Skills和可迁移经验已生成。主入口为 `docs/南枫转写Android完整开发档案.md`；状态与问题以 PROJECT_STATUS 及档案第15节为准。
- 基线：2bad9c2d559f69642ede2e4acd7a37b2599d9b71；初始314个跟踪项、17个可达提交已结构盘点，核心链路对照源码。不是所有代码逐行认证，上游/JNI内部与真实服务均不补造通过；全量路径/摘要索引和本轮验证见 `docs/qa/project-review-20261011/`。
- 当前检查：JVM91项/lint成功；合同57项；五类Skill的Ruby YAML/字段/元数据及185个本地链接通过，两个新只读脚本实际运行。官方quick_validate因PyYAML缺失未运行，未安装全局依赖；全球工作流审计无硬错误，原有软警告不改。
- 文档冲突已记录并同步：Windows真相/阶段规则、签名Keychain层级及keyPassword、JSON/TXT写入、Paraformer定位、隐私卡片、过去仪器记录。业务注释冲突留档，不修改业务代码。
- 未改：App源码/测试/Gradle、冻结APK、主设备/模拟器、账户/收费服务、Git提交/远端/Release、全球Skills和长期记忆。原未跟踪design-qa.md保留；本轮资产待本地提交，不自动纳入Git。
- 上下文闸门HANDOFF，已收口既有任务，无新功能分支。下一步仅按用户新请求；继续前先读项目AGENTS、状态和本节，不重做已完成历史验收。

## 最新任务：本地 checkpoint、最终回归与增量正式沉淀

- 开始 HEAD：5152a3d73626c561124c2fb6aa4546fc5d6497d5；本地代码点 checkpoint-v0.14.8-20261011-code，生产代码与发布标签相同。
- 核心新增文件已经纳入 Git，当前只补陈旧合同、项目 context/经验审计/可迁移模板和机器检查；已完成的源码接入、覆盖、预览和发布不重复执行。
- 本轮 JVM 91 项与 lintDebug 通过；旧合同快照预期触发 10 项漂移，新合同 56 项通过；经验结构/15 个链接/秘密模式检查通过。[增量档案](development-experience/README.md) 和 [回归记录](qa/final-regression-v0148/README.md) 保存详细证据。
- 本轮只建立本地提交和 checkpoint-v0.14.8-20261011-final；不推送、不重建/重签、不安装、不调用收费服务。冻结包未变，全球 Skill 与长期记忆未修改。
- design-qa.md 依赖临时图片，原样保留，不作为当前基准；当前状态和缺口以 [PROJECT_STATUS.md](PROJECT_STATUS.md) 为准。下一步仅按新的明确请求行动。

## 当前任务：v0.14.8 GitHub 正式发布

- 用户授权打包上传 GitHub；项目整体状态与验证缺口由 [PROJECT_STATUS.md](PROJECT_STATUS.md) 维护。
- 当前冻结包 0.14.8 (23)，已包含设置“隐私与诊断”移除、灵动岛改名及四段说明删除。APK hash 与签名、验证范围见 [发布验证](releases/v0.14.8-verification.md)。不要再次打包改变已安装、待发布产物。
- 发布代码基线 `290e03f93fe6e9df43c243d3a04d2839ba9720be` 已推送 origin/main，v0.14.8 标签指向同一基线；所有此前同项目功能改动纳入发布，保留无关 design-qa.md，不提交个人临时文件、安装备份或签名凭据。
- 2026-10-10 用户已授权将主设备保数据覆盖到 0.14.8，回读 hash 与冻结包一致，首装时间和数据目录标识未变；这仍不是主设备业务验收。本次发布不自动启动或修改主设备。
- [v0.14.8 正式 Release](https://github.com/nanzhufeng/NanfengTranscriber-Android/releases/tag/v0.14.8) 已公开（非草稿、非预发布），APK 106866933 字节与 SHA256SUMS.txt 已上传；GitHub 的 APK digest 与冻结包一致。README 已换成当前版本真实模拟器预览。上传初次连接过慢，中止本轮上传进程后使用单独 HTTP/1.1 请求成功；未修改全局网络配置。
- 本轮任务已闭环；下一步只按用户的新请求行动，不自动调用收费服务、操作主设备或扩大功能范围。

以下为各增量的历史执行记录，不覆盖上述当前版本和 PROJECT_STATUS 的状态。

## 最新增量：后台灵动岛与一行简洁模式（v0.14.7）

- 用户要求依据灵动岛建议增加后台转写进度并保障后台执行；随后确认默认一行简洁模式，点击展开详细版，最后要求加长并留足冗余。
- 已实现：同一真实任务快照驱动通知与后台悬浮窗；简洁胶囊至少 220dp 宽、左右各 24dp、可见高度 34dp / 48dp 点击范围，详细版保留原内容与查看/取消操作。背景赋值后重设 padding，避免文字贴边。进度不会自行展开；新任务或返回软件后恢复简洁版。
- 稳定性：独立前台服务保留，异常/取消/结束 finally 统一清理，30 分钟有界 partial wake lock 每 10 分钟续期。修复 Room 绑定导致主进程 FGS importance 125 被误当作可见 Activity 的根因；未保存结果前不显示 100%。现有断点继续复用。
- `testDebugUnitTest` 91 项（0 失败/错误/跳过）、`lintDebug`、`assembleRelease` 通过；`0.14.7 (22)` 正式签名、非 Debug，证书 SHA-256 `ee95f8d726b8fb85f15d0eb26d385505cb20a22cf1d749ec3e4327d7ee783174`。最终 APK SHA-256 `19d9cfb43fecf175a282e741ddb8c0c5e7c6bde4f4fbb0b445a7c128b82430e6`，路径 `app/build/outputs/apk/release/南枫转写.apk`。
- 隔离 ARM64 模拟器 `emulator-5592`：真实官方固定版本 SenseVoice 模型经正常备份入口导入，33 分钟重复 JFK 语音样本实际推理。退到桌面和熄屏后进度继续推进，生成真实结果；完成后 FGS/悬浮窗/活动唤醒锁无残留。从详细版取消时回读 CANCELLED，断点与音轨保留，无运行资源残留。最终加长版窗口实测 440×96px（320dpi，即 220×48dp），点击展开/收起已确认；最终两个截图与联系表见 `docs/qa/background-island-v0147/`。
- 首次需要在设置“隐私与诊断 → 后台转写与灵动岛”授权悬浮窗；未授权仍使用通知。未操作或安装 OPPO、未执行仪器测试、未进行真实百炼调用、Git 提交或 GitHub 发布。真机长期/OEM 后台限制、原生系统岛、实际超过 10 分钟续期与 6 小时超时仍待验证，不能保证软件永不被系统停止。详见 `docs/background-island.md`。
- 上下文闸门为 HANDOFF；当前增量交接已记录，不继续扩大新功能范围。下一步仅按用户明确授权执行同签名保数据覆盖，或在新线程继续真机验收。

## 最新增量：内外屏统一最新南枫下载导航（v0.14.6）

- 用户明确要求按最新南枫下载实现。基准为权威持续开发源码 2026-10-09 的 `PrimaryFloatingNavigation`，不是旧左栏、导出镜像或 7 月截图；来源与 WIP 文件哈希见 `docs/design-reference-index.md`。
- 内外屏共用底部纯白独立悬浮胶囊，移除内屏左栏。项目高度 52/56dp、外壳/选中项圆角 32/28dp、减淡阴影 6dp、内部留白 3dp、横向外屏 32dp/内屏 24dp、最大宽度 720dp、顶部间距 4dp、底部间距 4/6dp，保留系统手势 inset。标签统一“首页／历史／设置”；使用现有皮肤强调色，不更改业务数据。
- `testDebugUnitTest` 80 项（0 失败）、`lintDebug`、`assembleRelease` 通过。当前正式签名包 `0.14.6 (21)`，v3 证书已验证；APK SHA-256 `e6851d2cde8c1030db6e42d4e7f1adcb213c5656ee446d301cb68b703950dcdb`，路径 `app/build/outputs/apk/release/南枫转写.apk`。
- 隔离模拟器 `emulator-5592` 上运行正式包：外屏 1140×2616、内屏 2248×2480、442 dpi、字体比例 1.0，三页导航均在同一底部几何位置、选中正确。外屏项目实测 144px ≈ 52dp，内屏 155px ≈ 56dp；外屏设置切为内屏后仍选中设置。六张原图、XML、可重跑几何验收和联系表位于 `docs/qa/navigation-v0146/`。QA 后已恢复模拟器原 1600×1800 / 320dpi 配置。
- 2026-10-09 用户随后授权“覆盖安装”：OPPO `设备序列号已脱敏 / PKH120` 从 `0.14.4 (19)` 以正式同签名包 `adb push` + `pm install -r --user 0` 保数据覆盖到 `0.14.6 (21)`。安装后回读 base APK 哈希与上述产物完全一致，证书一致，`firstInstallTime=2026-07-20 01:09:40`、`ceDataInode=1091129`、`deDataInode=1030178` 保持不变，设备临时安装包已清理；`lastUpdateTime=2026-10-09 18:57:14`。旧安装包备份位于 `本地旧版安装包备份（未公开）`。
- 未启动 App，未进行 OPPO 实际折叠、手势、键盘、ASR 服务或真实任务验收；覆盖与包一致性不能代替这些业务验证。现有 ASR/UI 未提交成果保留，不提交、不推送、不发布。

## 最新目标：新 Qwen 文件转写 + Paraformer 低成本模式

- 用户要求用 `qwen-audio-3.1-asr-flash-filetrans` 替换 Qwen3-ASR，增加 `paraformer-v2` 低成本模式。
- 用户随后明确要求按原 Qwen3 标准直接完成，不增加后端配置。官方 REST 支持 `oss://` 临时 URL，现已沿用同一 Key，通过百炼临时上传接入两模型。临时文件最长 48 小时，官方不建议规模化生产使用，此限制见设置详情和准确率策略。
- 已接通：模型目录、默认选择迁移、新建/单项任务、后台队列、远端 task ID 原子持久化和恢复、结果时间戳、完整文件调用账本以及独立计费。旧 Qwen 设置迁移到新模型，已创建旧任务保留旧协议。完整接口测试覆盖上传/提交/轮询/结果、进程恢复与提交状态不明确时防重复计费。
- 当前版本 `0.14.5 (20)`。最终 `testDebugUnitTest`（80 项，0 失败）、`lintDebug`、`assembleRelease` 全部通过；Release `debuggable=false`、v3 签名已验。APK SHA-256：`234bf2d033c1e37742935bab0d554c933ab62f7c256f9b7e23e6575cf5c266f0`。包路径 `app/build/outputs/apk/release/南枫转写.apk`。
- 隔离模拟器 `emulator-5592` 已安装最终 Release（1600×1800），确认三项选择与对应模型详情切换，截图见 `docs/qa/filetrans-v0145/settings-qwen-expanded.png`；不把 MockWebServer 或页面切换当作真实百炼收费服务成功。
- 下一件事：在用户真实音频和已保存 Key 下验证新模型服务开通、临时上传权限、转写结果和费用；长期稳定高并发仍需独立私有存储，当前不创建该基础设施。
- 本轮无真实收费请求、百炼真实音频上传、OPPO 操作、Git 提交或 GitHub 发布。关键规则、许可/证据边界与官方来源见 `docs/asr-accuracy-strategy.md`。

## 当前增量：本地 ASR 准确率 P0

- 用户确认结合 2026-09-24 优化方案检查当前代码并实施。当前模型为 SenseVoiceSmall INT8；以实时源码为准，历史记忆里的 Whisper 默认信息已过期。
- 已修复：`SpeechPauseSegmenter` 不再删除短音节、低能量区域或停顿音频；30 秒以内完整保留上下文；独立长输入具有 30 秒上限，窗口连续、无重叠、无样本丢失。外层原本已有 30 秒分块，真正的缺陷是内部再次裁剪。
- 证据：旧实现上 3 个新增回归失败，最小修复后 5 项定向测试通过；另外补齐了精确重建及静音/空输入测试。2026-10-09 `testDebugUnitTest lintDebug assembleDebug` 全部通过；这是 JVM/静态检查/编译证据，没有进行真实模型推理或设备验收。
- 当前缺口：未收到极氪 9X 原片与独立人工 Ground Truth，尚未执行真实模型 A/B 或统计 CER；代码契约不能代替真实识别质量。
- 未扩展：重叠、文字去重、热词、二次识别、AI 改写、云端回退。未执行 OPPO 操作、安装、Git 提交、推送或发布。本轮工作树保留之前的 UI/关于页未提交成果。
- 当前事实源：`docs/asr-accuracy-strategy.md`、`engine/SpeechPauseSegmenter.kt`、`engine/SenseVoiceEngine.kt`、`engine/PcmTranscriptionCoordinator.kt` 及 `SpeechPauseSegmenterTest.kt`。
- 下一步：获得原片/人工全文路径后，在隔离环境固定模型与 PCM 做旧策略/完整窗口 A/B；先记录真实音频保留率、CER、术语保留率与耗时，再决定下一变量。
- 上下文闸门输出 `HANDOFF`（累计有效 Token 超过阈值），本轮仅完成当前最小修复和验证，不再开启新的优化分支。

下列内容为 2026-08-30 历史交接，版本、安装与验证记录不能当作当前证据。

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
- 2026-08-30 16:25 已按用户明确授权，将最新正式签名包以 `adb push` + `pm install -r --user 0` 同签名覆盖 OPPO `设备序列号已脱敏`。预检确认包名 `com.nanzhufeng.transcriber`、版本 `15`、Release `debuggable=false` 与证书 SHA-256 一致；安装后回读 base APK 的 SHA-256 与本机产物一致：`3d6522a1f2f2336a5604a8d21d2b2e71510dcc5c714b3573ba86c39cadb030a4`。`firstInstallTime` 仍为 `2026-07-20 01:09:40`；未卸载、未清数据、未运行 instrumentation，也未进入业务路径。
- 2026-08-30 16:52 已按用户明确授权覆盖包含默认输出目录修复的 Release。仍以 `adb push` + `pm install -r --user 0` 完成；预检证书一致、`debuggable=false`，安装后回读 base APK 与本机哈希一致：`4bd44df913063dafd2ccf411e7f30bbbbc26d597a5a6069862974193eba40fea`。`firstInstallTime=2026-07-20 01:09:40` 保持不变；未卸载、未清数据、未做 OPPO 业务操作或 instrumentation。真实 OPPO 目录写入仍需用户自行点一次导出确认，因为没有代替用户在其实际目录创建文件。
- 2026-08-30 17:16 已按用户明确授权覆盖包含内外屏视频续播修复的 Release。继续使用 `adb push` + `pm install -r --user 0`；预检证书一致、`debuggable=false`，安装后回读 base APK 与本机哈希一致：`7c67c0cdbfcfb35a354899add11b614917264d797ada87f99550837878732055`。`firstInstallTime=2026-07-20 01:09:40` 保持不变；未卸载、未清数据、未做 OPPO 业务操作或 instrumentation。OPPO 实际内外屏切换续播仍待用户从正在播放的视频手动切换屏幕确认。
- 已执行 OPPO Find N5（序列号 `设备序列号已脱敏`）同签名保数据覆盖：先从 `0.13.0-media-card-subtitles (14)` 升至 `0.14.0-final-regression (15)`，再以包含当前模型设置重组的最新 Release 覆盖。均使用 `/data/local/tmp` + `pm install -r --user 0`，未使用 `adb install`、未卸载、未清数据；`firstInstallTime=2026-07-20 01:09:40` 始终不变。最近一次覆盖（2026-08-30 15:41）包含调用记录入口、Qwen 调用审计、Whisper 选择移除及单项转写精简；设备 base APK SHA-256 与本地 Release 一致：`4f5926c1b2f89aed1f3bfeeae1986bce858597fa75d2c6b60a378e6889628339`。这只证明覆盖和安装包一致，不构成业务链路验收。
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
