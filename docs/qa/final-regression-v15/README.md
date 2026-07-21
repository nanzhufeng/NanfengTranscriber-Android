# 版本 15 最终回归证据

日期：2026-07-21

环境：`NanzhufengFindN5Api35` / API 35 / `1140 × 2616` / 442dpi / font scale 1.0

安装包：`0.14.0-final-regression (15)` Debug，与 Release 共用同一份业务源码

## 图形证据

- `emulator-home-subtitle-ready.png`：模型未下载，已选中视频仍可开始字幕预检；媒体位于主页列表卡内左侧。
- `emulator-history-subtitle.png`：完成任务只进历史，卡内视频预览、暗陶土橙文字摘要和时间线层级成立。
- `emulator-media-editor.png`：历史内二级核对页，视频在上、可编辑全文在下，复制全部和四格式导出可达。
- `emulator-settings.png`：设置首项为皮肤，模型/语言/CPU 为纵向选择框，底部导航的图标和文字共享完整选中容器。
- `emulator-no-subtitle-waits-model.png`：无字幕 WAV 允许启动预检，未发现字幕后转为“等待模型”，不误进历史。

## 真实文件结果

- 输入：8 秒 MP4，3 条中英混合内嵌文字 Cue。
- 提取：0.15 秒，未安装用户语音模型，未进入 Whisper 推理。
- TXT：108 bytes，完整中英文。
- MD：139 bytes。
- SRT：202 bytes，3 条原始时间轴 Cue。
- DOCX：1129 bytes，`unzip -t` 通过，`word/document.xml` 含完整转写文字。
- 同名再次保存由系统文件提供器生成 `(1)` 副本，没有静默覆盖旧文件。
- 复制全部后粘贴回历史搜索框，得到完整转写文字。

## 自动化对应

- JVM：45/45。
- instrumentation：8/8。
- Debug lint / Release vital lint：通过。
- Debug / Release assemble：通过。
- 模拟器最近目标进程无 `FATAL EXCEPTION`、`AndroidRuntime`、native fatal signal；`dumpsys activity lastanr` 为 `<no ANR has occurred since boot>`。

## 证据边界

本目录只是版本 15 模拟器证据，不代表 OPPO Find N5 覆盖、真机四格式落盘、分块恢复或 60 分钟/两小时长测已通过。
