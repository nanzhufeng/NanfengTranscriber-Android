# 第三方依赖声明

## whisper.cpp

- 来源：https://github.com/ggml-org/whisper.cpp
- 固定版本：`v1.9.1`
- 固定提交：`f049fff95a089aa9969deb009cdd4892b3e74916`
- 许可证：MIT，完整文本位于 `third_party/whisper.cpp/LICENSE`

## sherpa-onnx

- 来源：https://github.com/k2-fsa/sherpa-onnx
- 固定版本：`v1.13.6`
- Android 包装、Kotlin API 与 `libsherpa-onnx-jni.so` 仅用于 `arm64-v8a` 与 `x86_64`。
- 许可证：Apache-2.0，完整文本位于 `third_party/sherpa-onnx/LICENSE`。

## SenseVoice Small INT8 模型文件

- 上游：https://huggingface.co/csukuangfj/sherpa-onnx-sense-voice-zh-en-ja-ko-yue-int8-2025-09-09
- 固定修订：`355f4d4884d8afd08aef04b9007a8556d7b463b2`；文件清单、长度与 SHA-256 由 `OfficialModelCatalog` 固定并在安装时逐文件校验。
- 不随 APK、源码或 Release 附件分发；仅由用户设备从上述上游下载，或从经校验的本地备份导入。
- **模型权重许可状态：待上游提供可归档的模型文件许可证。** 该 Hugging Face 修订未附 `LICENSE`/`NOTICE`；不得把 sherpa-onnx 的 Apache-2.0 自动延伸到模型权重。FunAudioLLM 维护者曾说明 SenseVoice 权重适用 FunASR Model License、应保留来源与模型名，但该说明不是此精确 ONNX 制品附带的许可证。详见 `docs/model-license-status.md`。

模型权重不随当前 APK 分发。每个模型清单必须分别记录下载来源、文件长度、SHA-256 和适用许可证；未经许可门禁通过，不得把相关模型宣传为可正式发布或随软件再分发。
