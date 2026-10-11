# 南枫转写 Android 项目 context

## 项目身份

独立 Android 音视频转写工具；第一用户任务是本地文件进入同一队列、后台处理、私有结果落盘后进入历史、明确复制或导出。[验收合同](../product-acceptance.md) 定义成功条件，不以通知文字代替真实结果。

## 技术栈

Kotlin / Compose、Room v7、DataStore、SAF、Android Keystore、C++ JNI / sherpa-onnx，固定本地模型清单；云端 DashScope 文件客户端与统一 SpeechEngine 接口。whisper.cpp 和旧 Qwen3 路径仅是兼容能力，不是新的用户模型选择。

## 目录结构

- app/src/main/java：ui、domain、data、engine、service。
- app/src/test：当前领域、引擎 Mock 和队列生命周期回归。
- docs：当前合同、状态、版本化证据与本档案。
- scripts：开发用检查；不作为应用运行时依赖。

## 架构与唯一所有者

任务真相在 Room；模型安装由 ModelAssetManager / FileModelStore；真实片段提交由协调器与 checkpoint store；私有结果由 TranscriptDocumentStore；外部输出只由明确导出动作触发。后台执行由独立进程前台服务；通知和岛共享 NanfengIslandState，模式由 IslandDisplayController 管理。[架构治理](../architecture-governance.md) 是权威正文。

## 构建与回归

本轮只运行 testDebugUnitTest / lintDebug、合同与经验校验，并复核冻结 APK；不再次 assembleRelease 或重签名。构建需要 Android SDK 和兼容 JBR。正式包直接叫南枫转写.apk，凭据来自完整环境变量或 App 专属用户 Gradle 命名空间，禁止在仓库保存实际凭据。

## 数据安全

原音视频和已导出的外部文件不归任务删除所有；私有 PCM、断点和结果按对应领域规则清理。模型缓存经长度与 SHA 检查后原子提交，覆盖安装不能触发清数据或重下。API 凭据仅 Keystore 加密保存，不进入 Room、DataStore、日志或备份。

## 风险与证据边界

真实云端服务/费用、OPPO 长时后台/OEM 原生岛、实际系统时限及人工准确率基准尚未覆盖。仪器测试永久禁止；手机安装、权限修改和收费请求都不能从文档维护授权中推导。既有设备/模拟器证据保持原日期和版本，本次文档通过不是设备重跑。

## 关联事实

[项目状态](../PROJECT_STATUS.md)、[发布验证](../releases/v0.14.8-verification.md)、[后台实现与边界](../background-island.md)、[最终回归](../qa/final-regression-v0148/README.md)。
