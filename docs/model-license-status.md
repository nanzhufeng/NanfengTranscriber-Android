# SenseVoice 与 sherpa-onnx 许可状态

核查日期：2026-08-30。范围为本项目的本地标准 SenseVoice 路径；不把代码库许可证、模型权重许可证和下载方式混为一谈。

| 项目 | 固定来源 | 已确认事实 | 当前处理 |
| --- | --- | --- | --- |
| sherpa-onnx 代码与 Android JNI | `k2-fsa/sherpa-onnx` `v1.13.6` | 仓库附带 Apache-2.0，副本在 `third_party/sherpa-onnx/LICENSE` | 允许以该条款分发项目中对应源文件与 ABI 二进制，并在 `THIRD_PARTY_NOTICES.md` 署名 |
| SenseVoice 原始项目 | `FunAudioLLM/SenseVoice` | 上游项目另有代码许可；其维护者公开说明其自有模型权重遵守 FunASR Model License，并要求保留来源与模型名 | 只把这作为上游政策线索，不能替代精确制品许可 |
| SenseVoice Small INT8 ONNX 与 `tokens.txt` | `csukuangfj/sherpa-onnx-sense-voice-zh-en-ja-ko-yue-int8-2025-09-09` `355f4d4884d8afd08aef04b9007a8556d7b463b2` | 该固定目录只有模型、词表、测试音频与短 README；未发现附带 `LICENSE` 或 `NOTICE` | 不放入 APK、Git 或任何 Release；用户端仅按固定 URL 下载并做长度、SHA-256 校验 |

## 发布门禁

1. 当前可作为用户选择的本地标准模式，用于开发、隔离模拟器验收和用户主动下载；不得宣称权重许可已完成，也不得把模型随 APK、备份样例或 GitHub 附件再分发。
2. 要将其随 APK 再分发、用于正式商业发布或声称制品许可已完备，必须取得并归档与该精确 ONNX 制品匹配的许可文本，或改用带明确许可的等价制品；同时更新本文件、`THIRD_PARTY_NOTICES.md`、文件清单和产品验收合同。
3. 即使许可门禁通过，仍必须完成同素材准确度、实时系数、内存、温度、断点恢复与 Find N5 长测；许可通过不代表模型质量门禁通过。

## 可复核来源

- sherpa-onnx LICENSE：<https://github.com/k2-fsa/sherpa-onnx/blob/v1.13.6/LICENSE>
- 固定 ONNX 制品目录：<https://huggingface.co/csukuangfj/sherpa-onnx-sense-voice-zh-en-ja-ko-yue-int8-2025-09-09/tree/355f4d4884d8afd08aef04b9007a8556d7b463b2>
- FunAudioLLM 对 SenseVoice 权重条款的公开说明：<https://github.com/FunAudioLLM/SenseVoice/issues/286>

链接是核查线索，不能替代应随模型制品提供的许可文本。
