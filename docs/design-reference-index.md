# 设计参考唯一入口

更新时间：2026-10-09（Asia/Shanghai）

## 最新导航基准（覆盖下方旧左栏约定）

用户本轮要求内外屏统一按最新南枫下载落地。已读原任务 `019f6c95-868f-7aa2-a5cb-fd7e32640914`，其 7 月底部/左栏约定已被权威持续开发源码的 2026-10-09 导航增量取代。当前参考文件：`NanzhufengVideoDownloader-Android/android/app/src/main/java/com/nanzhufeng/videodownloader/navigation/NanzhufengApp.kt` 的 `PrimaryFloatingNavigation`，并核对该项目 `PROJECT_HANDOFF.md` 与 `docs/verification/2026-10-09-pornhub-download-playback-and-navigation.md`。

参考源码属于 `HEAD d9a66a1` 加未提交工作区，不能用该 SHA 冒充最新导航完整来源；导航文件 SHA-256 为 `ec9e13659ac9dbac6eb3650fc6f98804a0157e43ab2084b62b68760047ad28a3`。OPPO 当前未连接，未宣称当前已装包或主设备视觉已核验。当前用户指定采用最新源码，旧 `0.1.0-probe`、7 月截图和左栏规则仅保留历史溯源。

内外屏均使用独立底部白色悬浮胶囊，项目高度外屏 52dp、内屏 56dp；外层半径 32dp、内部选中项 28dp、内边距 3dp、阴影 6dp；横向外屏 32dp/内屏 24dp，最大宽度 720dp，顶部间距 4dp、底部额外间距外屏 4dp/内屏 6dp，保留系统导航安全区。一级标签统一“首页／历史／设置”；当前路由由原根状态保存，布局变化不重置选中页。皮肤保留现有强调色令牌，导航承载面始终纯白。

本文件是“南枫转写参考南枫下载”时的唯一设计来源入口。任何后续 Codex 任务在修改主页、历史、设置、导航、卡片、状态或折叠屏布局前，先读本文件，再读 `ui-contract.md`。

## 权威来源顺序

1. 南烛枫在当前任务中的明确要求：一级结构为“主页、历史、设置”，视觉与工作台交互参考南枫下载，并结合南枫记皮肤体系。
2. 南枫下载 Codex 原项目任务：`019f6c95-868f-7aa2-a5cb-fd7e32640914`。
3. 最终确认回合：`019f7509-a178-7af0-8d21-cdf2779dd69e`。该回合明确只使用 OPPO 当前安装版外屏/内屏六个真实页面与当前代码，排除旧预览和拼图。
4. 当前安装版：OPPO Find N5 上的 `com.nanzhufeng.videodownloader`，2026-07-20 复核为 `0.1.0-probe (1)`。
5. 权威持续开发源码：`NanzhufengVideoDownloader-Android/android`，2026-07-20 基准提交 `20a85c8`。
6. Android 导出仓库：`NanfengDownload-Android/android`，本次核对与权威源码逐文件一致；它是交付镜像，不覆盖原 Codex 确认回合和当前安装版。
7. 南枫转写自身当前数据模型、真实状态和平台能力。参考产品只提供结构与视觉原则，不能替代转写业务真相。

## 禁用来源

以下文件不得作为南枫转写的实现基准：

- `NanzhufengVideoDownloader-Android/docs/assets/android/find-n5-home-*.png`
- `NanfengDownload-Android/docs/assets/android/find-n5-home-*.png`
- `.artifacts/**/reference/**`
- `.artifacts/**/reference-normalized/**`
- `.artifacts/**/final-pairs/**`
- 任何把参考图、实现图、多个页面或不同视口拼在一起的对比板
- 没有包版本、设备、视口和确认回合的“current/final”图片

这些内容只可用于复盘，不可直接驱动代码。

## 对南枫转写的适配结论

- 外屏：一次只显示一个一级页面，使用底部导航；主页、历史、设置各自拥有完整根视口。
- 内屏：一次只显示一个一级页面，按最新基准使用同一底部悬浮导航；内容获得完整宽度，保持原有双栏/网格适配。
- 主页：模型状态与转写准备是紧凑摘要；文件/任务工作区占主要空间；开始转写等主要动作固定且可达。
- 历史：Room 中的真实转写记录为唯一数据源，提供搜索、状态/模型筛选、打开详情、重试和导出。
- 设置：模型与存储、转写参数、外观皮肤、隐私和诊断按语义分组；所有控件必须真实持久化。
- 空状态仍保留完整工作区骨架；失败任务原位保留并提供中文原因与恢复操作。
- 页面底使用低饱和灰绿；内容卡纯白；强调色只用于标题、图标、选中态和状态，不给整张卡染色。
- 六皮肤只替换 token、材质与有限装饰，不复制页面树和业务状态。

## 变更门禁

如果南枫下载安装包版本、权威源码提交或用户最终确认发生变化，先更新本索引和 `ui-contract.md`，再修改 UI。若三个来源互相冲突，停止视觉实现并先解决版本身份，不凭截图文件名猜测。
