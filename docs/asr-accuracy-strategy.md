# 中英混合转写准确率策略

当前业务语义见 [领域规则](domain-rules.md)，文档归属见 [合同索引](contracts-index.md)。服务/价格来源保持原2026-10-09核查版本，本次没有实时核验供应商或收费调用；建议/门禁不等于已实现/实测。

## 2026-10-09 云端模式升级（v0.14.5）

- 高精度模式替换为 `qwen-audio-3.1-asr-flash-filetrans`。
- 增加低成本模式 `paraformer-v2`；本地标准 SenseVoice 保留。
- 两项均为异步文件 URL 转写，不能替换当前同步 Base64 请求中的模型字符串冒充接入完成。

`DashScopeFileTranscriptionClient` 与 `DashScopeFileTranscriptionEngine` 已接入实际任务运行器、模型目录、默认设置和单项任务。沿用既有 30 秒 PCM 分块、同一 Android Keystore Key、同一任务队列/结果/导出，以及按完整源文件聚合的调用记录。每块上传与提交绑定同一个模型，查询子任务成功后下载句级时间戳；请求必须有 `parameters`，只处理单声道。上传和结果下载不携带百炼 API Key。

用户明确要求按现有 Qwen3-ASR 标准直接接入，不增加独立后端配置。因此本轮采用官方 REST 支持的百炼临时上传：用同一 Key 获取绑定模型的上传凭证，上传匿名 UUID 文件，再以 `oss://` 和 `X-DashScope-OssResourceResolve: enable` 提交。文件最长保留 48 小时，设置详情明确告知。该选择满足当前个人客户端无需额外 OSS 账号的使用方式；官方仍明确不建议生产/高并发使用临时上传，本轮不能声称已满足规模化生产门禁。没有创建私有 OSS 或新后端，未取得长期存储或高并发能力。

实际任务链为：解码/恢复 PCM → 逐块获取临时上传凭证 → 上传 WAV → 原子保存提交意图 → 提交所选文件模型 → 原子保存远端 task ID → 查询/恢复该 ID → 下载并缓存结果 → 写入原有完整文件结果/调用账本。缓存键包含任务目录、模型、音频指纹、源样本位置和账号指纹，不保存 Key 或临时签名 URL。完成缓存可以弥补结果已下载但外层断点尚未提交时的进程退出。提交响应丢失保留不确定状态，不自动重发；明确 4xx 拒绝或远端终态失败才允许用户重试创建新任务。远端结果超过有效期/404 时告知重转可能再次计费。旧设置的 `qwen3-asr-api` 选择解析为新 Qwen；已经创建的旧任务继续通过保留的旧模型/协议恢复，旧历史模型信息不重写。

北京区 2026-10-09 官方标价：Qwen 3.1 Filetrans 输入 0.8 元/百万 Token、输出 2.7 元/百万 Token；Paraformer-v2 为 0.00008 元/秒，按服务端报告的语音时长估算。新费用函数独立于旧 Qwen3 按秒账本；没有真实 Token/语音时长就返回未知，不用源文件时长伪造扣费。标价估算不扣除免费额度，也不等于服务商结算。“低成本”是产品定位，不承诺所有素材都比按 Token 计价的新 Qwen 更便宜。

自动测试使用 MockWebServer 覆盖两模型上传→提交→等待→结果的完整接口链路、任务恢复与防重复提交，另覆盖计费口径、未知用量和模型选择迁移。未读取真实 Key、未向百炼上传真实音频、未请求收费服务；真实账号开通、上传凭证权限、计费和识别质量仍需真实服务验证。

官方来源：
- [Qwen 文件转写 HTTP API](https://help.aliyun.com/en/model-studio/fun-asr-recorded-speech-recognition-http-api)
- [Paraformer 文件转写 HTTP API](https://help.aliyun.com/en/model-studio/paraformer-recorded-speech-recognition-restful-api)
- [临时 URL 的生产限制](https://help.aliyun.com/zh/model-studio/get-temporary-file-url)
- [北京区模型价格](https://help.aliyun.com/zh/model-studio/model-pricing)

## 固定产品架构

每次任务只选择一个转写模型；不允许完成本地转写后再自动调用 API 润色、翻译或覆盖结果。

1. **本地标准：SenseVoiceSmall INT8 + sherpa-onnx**。下载后在本机离线转写。
2. **高精度：Qwen-Audio-3.1-ASR-Flash-Filetrans**。沿用现有 Key，通过百炼临时上传和异步文件接口直接转写。
3. **低成本：Paraformer-v2**。同一 Key、上传/异步/历史/导出路径，按服务端报告语音时长估算。
4. **兼容与对照：旧 Qwen3-ASR / Whisper Small Q5_1**。旧 Qwen3 仅供已创建旧任务恢复，不再出现在新选择列表；Whisper 保留研发对照链路。

内嵌文字字幕在本地资产加载前优先提取；但runner先核对源可读、有效模型配置和云端Key存在。选择云端且无Key时不会到达字幕探测，不能声称无条件无Key提取。硬字幕OCR与外挂字幕关联不在当前范围。

## 准确率与资源门禁

同一批真实素材必须包含普通话、英语、句内中英混合、专业名词、多人声、背景音乐和较差录音。分别记录中文 CER、英文 WER、术语保留率、时间戳偏差、实时系数、峰值 PSS、温度、模型大小、断点恢复和两小时稳定性。

- SenseVoice 与 Whisper：在同一设备完成本地准确率、速度、内存和热稳定性比较。
- Qwen：只在用户自有 Key、明确同意外发音频且可控费用的场景，记录实际请求成功率、延迟和准确率；不得把没有真实 API 请求的构建验证写成服务验证。
- 完整 Whisper、Qwen 大尺寸本地变体及其他本地对照模型不进入普通设置入口；云端 `paraformer-v2` 已是当前公开低成本模式，不属于该排除项。实际公开列表由 OfficialModelCatalog 定义。

SenseVoice 当前精确 ONNX 制品缺少可归档的制品级许可，不能随 APK、Git 或 Release 再分发；详见 [`model-license-status.md`](model-license-status.md)。

## 当前窗口与后续补强建议

- 本地分块保留全部 PCM；能量阈值只能辅助寻找切点，不能删除低音量或短音节。当前外层按 30 秒分块，SenseVoice 对每块完整识别；超过 30 秒的独立引擎输入优先在 15–30 秒内至少 500 ms 的停顿中点切分，连续语音在 30 秒处切分，末尾允许不足 15 秒。
- 当前没有新增响度归一化或降噪执行链；若后续尝试，必须保留原始音轨、单变量比较，再决定是否接入。
- 后续本地术语映射、数字/标点规则和邻段一致性校正必须独立、可撤销，不能调用 Qwen 改写结果。

## 2026-10-09 本地管线排查与 P0 修复

输入方案：用户提供的《南枫转写_本地ASR准确率优化方案_2026-09-24.md》（原件未入库）。其中的优先级是待验证假设，示例 CER 不是实测；同音词列表的方向前后矛盾，不能直接生成自动替换词典。

确认的调用链为 `TranscriptionTaskRunner → PcmTranscriptionCoordinator → SenseVoiceEngine → SpeechPauseSegmenter`。外层本来就是 30 秒，原内部实现仍以 380 ms 停顿拆分、仅保留能量阈值以上且至少 80 ms 的区域，再补 120 ms 上下文，能稳定复现样本遗漏与短句碎裂。当前修复只改变内部窗口规则，不改变模型、音频解码、云端请求、历史记录或导出。

- 回归证据：旧代码上新增 3 个行为测试全部失败；修复后转绿。窗口首尾、拼接连续性、最大时长、低音量、短音节、全静音及不足一帧的尾部均有测试。
- 这里的覆盖率指窗口保留样本数 / 输入 PCM 样本数，不是方案中需要人工语音标注才能计算的“语音覆盖率”。全部保留不等于模型能够全部正确识别。
- 不增加重叠或文字去重：先隔离这一变量，避免重复识别或启发式去重删掉真实重复语句。30 秒硬边界仍可能切断词语，需要固定原片进一步评估。
- 当前模型目录声明 `supportsHotwords=false`；不能把 Whisper 的 prompt 参数直接套进 SenseVoice。上下文、热词、异常片段重识别都需各自建立真实支持与样本证据后推进。
- 自动 LLM 改写和 API 回退仍不在当前执行范围，不得推断用户同意上传音频或产生费用。

下一项证据是极氪 9X 原始音视频与独立人工 Ground Truth。对同一解码后的 PCM、同一模型制品和语言参数，先比较旧裁剪策略与完整 30 秒窗口，再单独测试重叠；分别保存输入哈希、模型哈希、源码状态（包括脏工作树）、参数、样本覆盖、CER、术语保留率及耗时。完整音频官方 CLI 对照只在该输入长度受支持时执行，长片采用同样完整窗口，避免把长度差异误归因于切片策略。没有原片和 Ground Truth 时不得填写或承诺 CER 提升。

## 官方依据

- SenseVoice：<https://github.com/FunAudioLLM/SenseVoice>
- sherpa-onnx SenseVoice Android / INT8：<https://k2-fsa.github.io/sherpa/onnx/sense-voice/pretrained.html>
- Qwen3-ASR API：<https://help.aliyun.com/zh/model-studio/qwen-asr-api-reference>
- whisper.cpp：<https://github.com/ggml-org/whisper.cpp>
