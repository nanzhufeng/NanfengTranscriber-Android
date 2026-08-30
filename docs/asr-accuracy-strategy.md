# 中英混合转写准确率策略

## 固定产品架构

每次任务只选择一个转写模型；不允许完成本地转写后再自动调用 API 润色、翻译或覆盖结果。

1. **本地标准：SenseVoiceSmall INT8 + sherpa-onnx**。下载后在本机离线转写。
2. **高精度：Qwen3-ASR API**。直接接收音频分段并返回转写。用户切换即生效；保存自己的千问 API Key 并开始任务后才请求，可能产生服务费用。
3. **对照与回滚：Whisper Small Q5_1**。保留现有本地链路，供兼容、对照和回滚；不立即删除。

内嵌文字字幕仍优先提取；没有可读字幕才进入所选 ASR。硬字幕 OCR 与外挂字幕自动关联不在当前范围。

## 准确率与资源门禁

同一批真实素材必须包含普通话、英语、句内中英混合、专业名词、多人声、背景音乐和较差录音。分别记录中文 CER、英文 WER、术语保留率、时间戳偏差、实时系数、峰值 PSS、温度、模型大小、断点恢复和两小时稳定性。

- SenseVoice 与 Whisper：在同一设备完成本地准确率、速度、内存和热稳定性比较。
- Qwen：只在用户自有 Key、明确同意外发音频且可控费用的场景，记录实际请求成功率、延迟和准确率；不得把没有真实 API 请求的构建验证写成服务验证。
- 完整 Whisper、Qwen 大尺寸本地变体、Paraformer 只作研发对照，不进入普通设置入口。

SenseVoice 当前精确 ONNX 制品缺少可归档的制品级许可，不能随 APK、Git 或 Release 再分发；详见 [`model-license-status.md`](model-license-status.md)。

## 低成本补强

- 用 VAD 按真实语音停顿切成约 15–30 秒片段，并保留少量相邻上下文。
- 保留原始音轨，做可控响度归一化和轻量降噪。
- 后续本地术语映射、数字/标点规则和邻段一致性校正必须独立、可撤销，不能调用 Qwen 改写结果。

## 官方依据

- SenseVoice：<https://github.com/FunAudioLLM/SenseVoice>
- sherpa-onnx SenseVoice Android / INT8：<https://k2-fsa.github.io/sherpa/onnx/sense-voice/pretrained.html>
- Qwen3-ASR API：<https://help.aliyun.com/zh/model-studio/qwen-asr-api-reference>
- whisper.cpp：<https://github.com/ggml-org/whisper.cpp>
