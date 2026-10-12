# 项目复盘全量文件与 Git 索引

基线：`2bad9c2d559f69642ede2e4acd7a37b2599d9b71`；分支：`main`。

这是逐文件枚举、内容摘要与结构扫描，不是逐行语义审计或第三方二进制安全认证。文本完整读取用于结构统计与 JSON 解析；二进制只核对类型/大小/哈希。业务结论由完整档案中的具体源码和测试另行证明。文件表是复盘开始阶段的工作树快照，包含当时已更新的状态文件，后续本轮文档修改不倒灌该表；基线 SHA 表示底层提交，不冒充整表均为提交内字节。未跟踪、忽略文件和供应商子模块内部不纳入主仓库表。

Git 跟踪项：314；可达提交：17。
- binary：121 项，86925148 字节。
- config-tool-license：21 项，89153 字节。
- docs-evidence：56 项，612431 字节。
- instrumented-source-only：7 项，25936 字节。
- production：80 项，621755 字节。
- submodule：1 项，0 字节。
- unit：28 项，69582 字节。

## 每个跟踪项

| 路径 | 类别 | 字节 | 文本行 | SHA-256 | 内容检查 |
| --- | --- | --- | --- | --- | --- |
| .gitignore | config-tool-license | 142 | 12 | 9ca8bbdc790f140290cacbfc40476b019cdfcebeb07f570ac8f60ac9dd49770c | - |
| .gitmodules | config-tool-license | 121 | 3 | 52f73892dde53d03b394858618e790c4f81aa5d9ae73ace19920fd3531cb819e | - |
| AGENTS.md | config-tool-license | 1004 | 12 | dec4252c847ae047d6f6798035c0573e3084dbab763ebb70ececbe773b88aa75 | headings=1 |
| README.md | config-tool-license | 1226 | 27 | dac8d7d10c42e4813598f2037c9ef20e62b38b7a266a610e53df31a10deb9321 | headings=4 |
| THIRD_PARTY_NOTICES.md | config-tool-license | 1599 | 24 | 69b8dd2e9e5a7a0b87961afc0655ad774fcdef047ea5df11a8e68e02feeee056 | headings=4 |
| app/build.gradle.kts | config-tool-license | 5566 | 158 | 665a8db5918298b48210c50b2a1820ee0bb822f85f3b28f4b69332cbd0273ce6 | - |
| app/schemas/com.nanzhufeng.transcriber.data.task.TranscriptionDatabase/1.json | config-tool-license | 5924 | 182 | 9b5ddfcd36539c5bd521d36f9c0b68a59bb50d3cfc1f890c35a504848b64bd97 | JSON parse OK |
| app/schemas/com.nanzhufeng.transcriber.data.task.TranscriptionDatabase/2.json | config-tool-license | 6125 | 188 | d90a334144455fb6088601d4fa838a9312e59cbe6a6fc9a15f61004a8238ccc9 | JSON parse OK |
| app/schemas/com.nanzhufeng.transcriber.data.task.TranscriptionDatabase/3.json | config-tool-license | 6555 | 200 | b9d003ea57f6ca6bb089fae99a5aa809b8a507654849a0610193019c8e922d26 | JSON parse OK |
| app/schemas/com.nanzhufeng.transcriber.data.task.TranscriptionDatabase/4.json | config-tool-license | 7203 | 218 | 08f46361829e93dd797d4b80dbd64a2c7733ac49c3a61c7d22218cae83e3f3e6 | JSON parse OK |
| app/schemas/com.nanzhufeng.transcriber.data.task.TranscriptionDatabase/5.json | config-tool-license | 10248 | 312 | 5101bbc8ab4e4cee9b4ea3de34aadcbddd104fe03fcb0cc3c7d7f311433b4829 | JSON parse OK |
| app/schemas/com.nanzhufeng.transcriber.data.task.TranscriptionDatabase/7.json | config-tool-license | 12019 | 363 | dc13633cc4b346266f21c6f89679b88236fcfb6345e2143cb51d000a178beb98 | JSON parse OK |
| app/src/androidTest/assets/jfk.wav | binary | 352078 | - | 59dfb9a4acb36fe2a2affc14bacbee2920ff435cb13cc314a08c13f66ba7860e | - |
| app/src/androidTest/java/com/nanzhufeng/transcriber/data/media/MediaCodecAudioDecoderInstrumentedTest.kt | instrumented-source-only | 2123 | 47 | 416e5d246a557fe59c94f159d6f38b272088724c0b0e826e80d5c4cabac3d5d1 | declarations=2; tests=1 |
| app/src/androidTest/java/com/nanzhufeng/transcriber/data/settings/SecureApiCredentialStoreInstrumentedTest.kt | instrumented-source-only | 1080 | 31 | 23995cc5a0d157fa4b5773d46978da82c02942bfd9ee977910118489dc350bf5 | declarations=2; tests=1 |
| app/src/androidTest/java/com/nanzhufeng/transcriber/data/task/RoomTranscriptionTaskRepositoryInstrumentedTest.kt | instrumented-source-only | 3866 | 94 | 5b87fcb5f84e57a725387cb8348ce3d07a0e2138a3de78fa56fe091763876adb | declarations=2; tests=1 |
| app/src/androidTest/java/com/nanzhufeng/transcriber/data/task/TranscriptionDatabaseMigrationInstrumentedTest.kt | instrumented-source-only | 7376 | 164 | 8e69ab211df0a10255692ede70cccd49a6182d7bba1d6a34c3c4e10eac637538 | declarations=4; tests=3 |
| app/src/androidTest/java/com/nanzhufeng/transcriber/engine/EndToEndTranscriptionInstrumentedTest.kt | instrumented-source-only | 3690 | 77 | 2520d69d6ae5b8d4b91aeed9b40cb69794b4801bfb1cc9f31a120708e687486c | declarations=2; tests=1 |
| app/src/androidTest/java/com/nanzhufeng/transcriber/engine/SenseVoiceEngineInstrumentedTest.kt | instrumented-source-only | 3500 | 79 | 169875db5a8c2f474efc730ad38a9440ea5faddec3e3cc0488a3df9e04ba5b4d | declarations=3; tests=1 |
| app/src/androidTest/java/com/nanzhufeng/transcriber/engine/WhisperCppEngineInstrumentedTest.kt | instrumented-source-only | 4301 | 95 | a220690630f0bbecdddb138072cc6292e56e9e0ccdb20af5ac8cf320405d2975 | declarations=3; tests=1 |
| app/src/main/AndroidManifest.xml | production | 2439 | 51 | db4d4263513bccc1c0bb98e919b49e7a8a9fc5e6da43ebae8d15724e38a1a9db | - |
| app/src/main/cpp/CMakeLists.txt | production | 1180 | 22 | 4f2d04ac25fa6a7654525c86fe418c464f1cd8e097f8169c24e3b0e3e342256f | - |
| app/src/main/cpp/nanzhufeng_transcriber_jni.cpp | production | 7803 | 211 | c7bdccd1960d5e53213d68147c489bbe0d85170970471280c2f93d07428aa4d8 | declarations=0; tests=0 |
| app/src/main/java/com/k2fsa/sherpa/onnx/FeatureConfig.kt | production | 301 | 11 | 829c7adc4db1324f21b9a9cbf5bf10eacf759714fe50956f48317c1ffef044f6 | declarations=2; tests=0 |
| app/src/main/java/com/k2fsa/sherpa/onnx/HomophoneReplacerConfig.kt | production | 170 | 7 | fe926b6413f4d87e67c41feb21b8363923a67cbd3a5eb687b76390a5f1f7db60 | declarations=1; tests=0 |
| app/src/main/java/com/k2fsa/sherpa/onnx/OfflineRecognizer.kt | production | 56380 | 1512 | 85054fa9722913bce96c94c5c7eaaa7fd99606b79fbfbeac6cfb60040eae2164 | declarations=28; tests=0 |
| app/src/main/java/com/k2fsa/sherpa/onnx/OfflineStream.kt | production | 1080 | 42 | 5c90b7c060cdd0a2222cc935b54f69465a4a7544de29240bafe4d3a8d86dbb6e | declarations=6; tests=0 |
| app/src/main/java/com/k2fsa/sherpa/onnx/QnnConfig.kt | production | 156 | 7 | 86c963f27677f690eb0d6639735052eba5e60d9f81ccd4c1df0aa831fd680d6f | declarations=1; tests=0 |
| app/src/main/java/com/nanzhufeng/transcriber/MainActivity.kt | production | 2755 | 67 | ae7138a22578e3335b4ea01c17002b7014957969d88d2a7ab4e28f1bf8579909 | declarations=5; tests=0 |
| app/src/main/java/com/nanzhufeng/transcriber/NanfengTranscriberApplication.kt | production | 4346 | 93 | d2ec5f7cbdcec024bdb29f489b7920c9d0db1017517f2ee617cfd3a0f6532b88 | declarations=3; tests=0 |
| app/src/main/java/com/nanzhufeng/transcriber/data/invocation/AsrInvocationDao.kt | production | 507 | 16 | 472042394ab25607b643f12420aa747e74d8941abed5eeccf407dbf7b96a5c62 | declarations=3; tests=0 |
| app/src/main/java/com/nanzhufeng/transcriber/data/invocation/AsrInvocationRecordEntity.kt | production | 2351 | 75 | 6b2f49d8e45408de3948d7d32e90709908c702d60360cb9139192417950cb9e4 | declarations=3; tests=0 |
| app/src/main/java/com/nanzhufeng/transcriber/data/invocation/RoomAsrInvocationRepository.kt | production | 635 | 18 | 79d968f7eb7b36c729275fa78b9e34ca0e25c182310b4f68ee450706ff1a300b | declarations=3; tests=0 |
| app/src/main/java/com/nanzhufeng/transcriber/data/media/EmbeddedSubtitleExtractor.kt | production | 8492 | 205 | 4df51f7831b293bb41390330523579b077e67515bfc8397faf6138ddf797ba80 | declarations=9; tests=0 |
| app/src/main/java/com/nanzhufeng/transcriber/data/media/EmbeddedSubtitleParser.kt | production | 3101 | 75 | 3ac835f012e12e2cb62663c5a9ed4b139a3a8163ece5f8369dab84b54e19a13e | declarations=6; tests=0 |
| app/src/main/java/com/nanzhufeng/transcriber/data/media/HistoryMediaPreviewStore.kt | production | 7116 | 183 | 5501668469c9212ebdda0855ecaf728bcda4ca43bc2a34e0e3875372f233d232 | declarations=12; tests=0 |
| app/src/main/java/com/nanzhufeng/transcriber/data/media/MediaCodecAudioDecoder.kt | production | 11115 | 259 | 83301c18ac801f7695ae3622219a3ea1d2c91b393c03f9a472074837ac50ac17 | declarations=14; tests=0 |
| app/src/main/java/com/nanzhufeng/transcriber/data/media/StreamingMonoResampler.kt | production | 2647 | 64 | da9b37311d808fc630f5f40637754667b600f749ff03bee21c0cfcd4512bf03a | declarations=3; tests=0 |
| app/src/main/java/com/nanzhufeng/transcriber/data/modelstore/AndroidModelDocumentGateway.kt | production | 1174 | 36 | 74d40f834f1df59fa6bab85437f1ce90a63e3b484a24c56d0d6b7a19d3d64946 | declarations=3; tests=0 |
| app/src/main/java/com/nanzhufeng/transcriber/data/modelstore/FileModelStore.kt | production | 13788 | 336 | 0d9642c9a3c76d912adcfa4bd37ec0baa45f122a89beb674704be136f061ea5f | declarations=26; tests=0 |
| app/src/main/java/com/nanzhufeng/transcriber/data/modelstore/ModelAssetManager.kt | production | 22944 | 591 | 9f0a7d730f253b03f7b30b91426baff810919fd6c017aee65f7d973814f4b2fb | declarations=25; tests=0 |
| app/src/main/java/com/nanzhufeng/transcriber/data/modelstore/ModelInspection.kt | production | 265 | 10 | f03e72b88717a7021c39c96a2678aa459aba1b14b71e658abf5324f8c6632076 | declarations=1; tests=0 |
| app/src/main/java/com/nanzhufeng/transcriber/data/modelstore/ModelManifest.kt | production | 2266 | 67 | 93850a44c700d836ae8756d84bd7e528941d0a2a444ab57efc0b6ec2045f1b43 | declarations=2; tests=0 |
| app/src/main/java/com/nanzhufeng/transcriber/data/modelstore/OfficialModelCatalog.kt | production | 6060 | 154 | 60fcde01a1779da72f072fef0dce0e9f4e0501eadf8c8b9fe383c4fae33976e2 | declarations=10; tests=0 |
| app/src/main/java/com/nanzhufeng/transcriber/data/output/AndroidTranscriptOutputStore.kt | production | 6052 | 130 | e0c1d9002da9a2cc1bfbb9acf49209c811bb02a6a7dcb68b325172ee2f34c48f | declarations=10; tests=0 |
| app/src/main/java/com/nanzhufeng/transcriber/data/postprocess/OpenAiCompatibleTextPostProcessor.kt | production | 6590 | 153 | 5468f5aadda3d31097aa276f8c8741f59558ab9e3ab08b126580c448f8451e0a | declarations=9; tests=0 |
| app/src/main/java/com/nanzhufeng/transcriber/data/result/TranscriptDocumentStore.kt | production | 3536 | 90 | 8af91dfe339a3a427161ef59d98b77c95626718b9628d531749bc05e2289d827 | declarations=7; tests=0 |
| app/src/main/java/com/nanzhufeng/transcriber/data/result/TranscriptionCheckpointStore.kt | production | 6080 | 144 | b1509b056609fbf274320bb5ebcbe6e7474153a53b013462a93d69f8dafdb668 | declarations=9; tests=0 |
| app/src/main/java/com/nanzhufeng/transcriber/data/settings/SecureApiCredentialStore.kt | production | 3314 | 80 | 2b9cbd1f09519aae7b3e1f3ac65410a07d349d5459bde5aaf9a7dda82e43d167 | declarations=6; tests=0 |
| app/src/main/java/com/nanzhufeng/transcriber/data/settings/TranscriptionSettingsRepository.kt | production | 7742 | 172 | 85473e02068df38420f0a3419d343c17131a0c6c6b2d22c6fef8dcc2f7e60221 | declarations=15; tests=0 |
| app/src/main/java/com/nanzhufeng/transcriber/data/task/RoomTranscriptionTaskRepository.kt | production | 10279 | 246 | 371b328c6dbf7183d8bf7d916aa17b66233336f751a2a46c8f660fa45658da8b | declarations=15; tests=0 |
| app/src/main/java/com/nanzhufeng/transcriber/data/task/TranscriptionDatabase.kt | production | 4058 | 97 | 3860aa50f7a30c1fd9e4c42dba42a8fdefc98e3938440f5cf0813eca7bab0395 | declarations=9; tests=0 |
| app/src/main/java/com/nanzhufeng/transcriber/data/task/TranscriptionTaskDao.kt | production | 1192 | 32 | 30978a915f696e3a62e1b82a37df8b72910321f0968b25ad7b01ac3ffac6f331 | declarations=8; tests=0 |
| app/src/main/java/com/nanzhufeng/transcriber/data/task/TranscriptionTaskEntity.kt | production | 3121 | 98 | 27e2f26ee6ed485a243f6733d6e92ab7203fe6c8f897b715640c739fd039f943 | declarations=5; tests=0 |
| app/src/main/java/com/nanzhufeng/transcriber/domain/export/TranscriptDocument.kt | production | 581 | 25 | 3b68d42d89fbb556c99fa0ed760601b30dfd962237a956a82b14819688d22373 | declarations=3; tests=0 |
| app/src/main/java/com/nanzhufeng/transcriber/domain/export/TranscriptExportService.kt | production | 8333 | 209 | 88b012f4d6afcbc0fe17753a4d450bffe9e0546af8544dde11e442c4e2f588e6 | declarations=18; tests=0 |
| app/src/main/java/com/nanzhufeng/transcriber/domain/invocation/AsrCostEstimator.kt | production | 2418 | 54 | ce4f2d50299fd4ce03e2b5f27f1961d1e466afae5b4a11da63b415ee335a6151 | declarations=5; tests=0 |
| app/src/main/java/com/nanzhufeng/transcriber/domain/invocation/AsrInvocationLedger.kt | production | 1207 | 40 | f3b8666dfba4d29fe3fff5a1b27d82f1656879b3c9908a3be41a5274f6022f82 | declarations=6; tests=0 |
| app/src/main/java/com/nanzhufeng/transcriber/domain/model/ModelInstallState.kt | production | 181 | 11 | cf1096268c7dc82c97ee7838736c96ddcd6cd9bd6433591ac23debf1dfea9223 | declarations=1; tests=0 |
| app/src/main/java/com/nanzhufeng/transcriber/domain/task/IslandDisplayController.kt | production | 439 | 15 | ee4c382617a10c0581aeefa8e86cb5f7deffacd7ddd69764bfe1f990a8cbc0fa | declarations=4; tests=0 |
| app/src/main/java/com/nanzhufeng/transcriber/domain/task/MainScreenVisibilityPolicy.kt | production | 261 | 6 | 177464f14cceedcac97d1b71cd9d38d884712b7ac77bb67437d5e6fa3750ae92 | declarations=2; tests=0 |
| app/src/main/java/com/nanzhufeng/transcriber/domain/task/NanfengIslandState.kt | production | 2318 | 55 | cf8a2c31b44f028da5a2b403d887a62ba7bd79eb22edad4cbd304090bee8acdf | declarations=5; tests=0 |
| app/src/main/java/com/nanzhufeng/transcriber/domain/task/TaskNotificationPresentationPolicy.kt | production | 303 | 7 | f077399aac477055679f313c08fe80b42fa3ab1fa57e39f7a49cac8a17f96d6e | declarations=2; tests=0 |
| app/src/main/java/com/nanzhufeng/transcriber/domain/task/TranscriptionTaskState.kt | production | 3058 | 88 | 8a0c53084c8c6f328bde186a18fa5a631578288646b8c7823f1e2b4ba92dc97a | declarations=5; tests=0 |
| app/src/main/java/com/nanzhufeng/transcriber/domain/transcription/LocalTranscriptPunctuationRule.kt | production | 1710 | 36 | 4ca5a725eeaf5b8c70fe0da4e50a4af63dba855951cb1107680e0f7427c77d2c | declarations=6; tests=0 |
| app/src/main/java/com/nanzhufeng/transcriber/domain/transcription/TranscriptCompletionPolicy.kt | production | 209 | 6 | b81a5110c1b3dabb66e35bb5ee0441e5e396b918ada6828018e9c003f9bd654b | declarations=2; tests=0 |
| app/src/main/java/com/nanzhufeng/transcriber/engine/DashScopeFileTranscriptionClient.kt | production | 13651 | 261 | b5be2e4047ec1da0e39824183d6202fd43fce9735508893a119710052cf58fc8 | declarations=24; tests=0 |
| app/src/main/java/com/nanzhufeng/transcriber/engine/DashScopeFileTranscriptionEngine.kt | production | 8507 | 151 | 9c2da12974b393651e6b6b61f0ef242521f33821a64bd1852f1df40f6822263f | declarations=13; tests=0 |
| app/src/main/java/com/nanzhufeng/transcriber/engine/ModelSessionManager.kt | production | 1292 | 48 | 07f2c3330bfde07a309775136a0fcc3cbf7c1e7c78698ba6640fabacd533c80b | declarations=7; tests=0 |
| app/src/main/java/com/nanzhufeng/transcriber/engine/NativeWhisperBridge.kt | production | 1169 | 36 | 1f10d84256b3a713d1c82ca1739f445e28bfa12a4f8edf53b684058cd1aedb4f | declarations=7; tests=0 |
| app/src/main/java/com/nanzhufeng/transcriber/engine/PcmTranscriptionCoordinator.kt | production | 8225 | 197 | 47bf061d0b82d0a112eac8575c7ce4d93a15df2f2645fb97f4595940445d81b9 | declarations=10; tests=0 |
| app/src/main/java/com/nanzhufeng/transcriber/engine/QueueModelSessionOwner.kt | production | 1936 | 64 | d07005573f0ab08636518361294540a6ea37c45106945cec3facf0fcbfe81725 | declarations=6; tests=0 |
| app/src/main/java/com/nanzhufeng/transcriber/engine/Qwen3AsrApiEngine.kt | production | 7839 | 146 | 201c29829e5f76c1cb1cdb57189f49d9f2f7b4d383a12ffa1c12872daa521f84 | declarations=11; tests=0 |
| app/src/main/java/com/nanzhufeng/transcriber/engine/SenseVoiceEngine.kt | production | 5077 | 136 | f132fa9c689bd81a3841103e48421939b996ca2b273b3fd8036bd13c717dd397 | declarations=10; tests=0 |
| app/src/main/java/com/nanzhufeng/transcriber/engine/SpeechEngine.kt | production | 1778 | 63 | fdc86e81e16a58aeae2ea3c9a96c667e9a2ea32193b1fed493fb96edc7927b22 | declarations=12; tests=0 |
| app/src/main/java/com/nanzhufeng/transcriber/engine/SpeechEngineFactory.kt | production | 589 | 13 | c24b7edec0ffb2ae838f21952d5c762e8cc3ed33a4350a3963bd823427ef0f01 | declarations=2; tests=0 |
| app/src/main/java/com/nanzhufeng/transcriber/engine/SpeechPauseSegmenter.kt | production | 3396 | 90 | aab42583dceaecc79281efc013362990c71e67c4e0f5b24f6e5fcdf89943c15b | declarations=7; tests=0 |
| app/src/main/java/com/nanzhufeng/transcriber/engine/WhisperCppEngine.kt | production | 2399 | 67 | 38b21875eb73e4bb399f8d5c608630423a4ce2c4dc5a32102f901dce2369fa6d | declarations=7; tests=0 |
| app/src/main/java/com/nanzhufeng/transcriber/engine/benchmark/AsrBenchmarkEvaluator.kt | production | 3743 | 98 | 63abe55cd92ec17cc74a71a1bcd167897a8d65547fa117cc8f1877697b92f987 | declarations=7; tests=0 |
| app/src/main/java/com/nanzhufeng/transcriber/service/BackgroundTranscriptionQueue.kt | production | 1038 | 30 | a06784f10285a5b89e13f9a4daa448a6c1d828d21e58a8fb34cf6ab3f947fbe0 | declarations=2; tests=0 |
| app/src/main/java/com/nanzhufeng/transcriber/service/TaskIslandOverlay.kt | production | 9015 | 166 | 73e56481240d70f158dcc0f340434beaa655af7a9abb96356a6a270eaa1e810a | declarations=8; tests=0 |
| app/src/main/java/com/nanzhufeng/transcriber/service/TranscriptionForegroundService.kt | production | 17604 | 404 | cb7984a17f1a515b1e4eb2f25e44688470000d97a46fd02d03f7226a1aeb0cfa | declarations=17; tests=0 |
| app/src/main/java/com/nanzhufeng/transcriber/service/TranscriptionTaskRunner.kt | production | 38035 | 772 | bc4a20b868aec8430bddebf1a5ee25931ee8c4b2a1c9cfae3c466f05d18aa80e | declarations=19; tests=0 |
| app/src/main/java/com/nanzhufeng/transcriber/ui/NanfengTranscriberApp.kt | production | 15123 | 326 | 9ed402cdda32e3b4e2cbd69b013f4bde980b4525eaa395c8a0bb410dee285a9c | declarations=4; tests=0 |
| app/src/main/java/com/nanzhufeng/transcriber/ui/TranscriptionViewModel.kt | production | 67316 | 1528 | 929a4c1e7c3fd1b0b561d3da246dacfefd31371521ef0c1c82438f75b4fa74b5 | declarations=74; tests=0 |
| app/src/main/java/com/nanzhufeng/transcriber/ui/components/TaskMediaPreview.kt | production | 4872 | 114 | b630526bfc9c73a7edfb2837b5f9fc0ed58210ceacb23c6b73a7eaff4f30c0aa | declarations=1; tests=0 |
| app/src/main/java/com/nanzhufeng/transcriber/ui/components/TopStatusIsland.kt | production | 3203 | 82 | e850a1424ca13fe5d5561ccc833b03288713cfed7247a60ba2fbd27517687b1b | declarations=1; tests=0 |
| app/src/main/java/com/nanzhufeng/transcriber/ui/components/TranscriptPreviewFormatter.kt | production | 552 | 14 | bc0c7b7dc9e04f73b5f4a70d97236f2def68747c801795ea23349a92fec10ed4 | declarations=1; tests=0 |
| app/src/main/java/com/nanzhufeng/transcriber/ui/components/WorkbenchComponents.kt | production | 12188 | 335 | 6e02404e4188f271d721e1c311c1ba9c8c3ffabb19275b7a4cea458ca44993a2 | declarations=14; tests=0 |
| app/src/main/java/com/nanzhufeng/transcriber/ui/screens/AsrInvocationHistoryScreen.kt | production | 11359 | 248 | fb54c66482e990aab7f41c7b15a39bc079b630f17a663a6af3a6dff3ee939e7b | declarations=13; tests=0 |
| app/src/main/java/com/nanzhufeng/transcriber/ui/screens/BackgroundIslandSettingsCard.kt | production | 3055 | 56 | 43879471f80478e8a1ba75a170e4e0ca2d5f430e7d2814076c7a385aa12e0ff3 | declarations=2; tests=0 |
| app/src/main/java/com/nanzhufeng/transcriber/ui/screens/HistoryScreen.kt | production | 55703 | 1252 | c06ad7c7c6f316cc2d68720c3cf6ef6bca0da9ac7d2eb38cf8eb83f15e285156 | declarations=18; tests=0 |
| app/src/main/java/com/nanzhufeng/transcriber/ui/screens/HomeScreen.kt | production | 45130 | 1061 | e37edfb3119200a16f909d6e08852bd5c28d59a64ab0aaba5d326758547e58d6 | declarations=19; tests=0 |
| app/src/main/java/com/nanzhufeng/transcriber/ui/screens/SettingsScreen.kt | production | 37754 | 960 | 0003056e3922d82459a43ba0fb7074760c64cbe008c849cd02add129cc5c3ca8 | declarations=22; tests=0 |
| app/src/main/java/com/nanzhufeng/transcriber/ui/theme/Theme.kt | production | 5934 | 156 | 751682a5d723212331cdd4384487435a0515a38c95d40c78864d600261af8a86 | declarations=6; tests=0 |
| app/src/main/jniLibs/arm64-v8a/libsherpa-onnx-jni.so | binary | 23647720 | - | 8b0ddcef4f07f05432123e982c00429488e3861390c8c62d32359b5935fb14c0 | - |
| app/src/main/jniLibs/x86_64/libsherpa-onnx-jni.so | binary | 26844496 | - | 8c7e05d04c623eb076a7f76c4b39cd9568d0c1f607242937e4976c7e6c8dadf2 | - |
| app/src/main/res/drawable/ic_transcription_notification.xml | production | 517 | 10 | 9cae533206e26809f7191b76eb8d045de78a97f38f755b90b76b78aeba94d69a | - |
| app/src/main/res/mipmap-hdpi/app_icon.png | binary | 10437 | - | 0acc66c6be621a30c6ccbe8bbeca4e768c726012f6327d0a179490daeef407d9 | - |
| app/src/main/res/mipmap-mdpi/app_icon.png | binary | 5484 | - | 2e70e8f5703ec46f2cb3df5fc588d8c07e98f625fc85196bbc6356b4150edfb6 | - |
| app/src/main/res/mipmap-xhdpi/app_icon.png | binary | 16592 | - | 777ee7fe15e368b935c49f5677e1f02c72171ba04ef7e3458cbb08591e96ac8d | - |
| app/src/main/res/mipmap-xxhdpi/app_icon.png | binary | 32851 | - | 21265a6a0f8d78573bb07873c53aa18076dc92679f2625dba2daae6ea08587b9 | - |
| app/src/main/res/mipmap-xxxhdpi/app_icon.png | binary | 53956 | - | 7a7efffefa28c5b6b29c7c8f9a51bb2c1c70e90b4bcf4d58f567dbf6b68a24d0 | - |
| app/src/main/res/values/strings.xml | production | 75 | 3 | 4e43bf90683f45bf0b3b003336911a6c9210a7dff4e517b2bea852099cffb805 | - |
| app/src/main/res/values/themes.xml | production | 439 | 9 | f47081bc1689d9c202e994b108bea3e4a2b6e63047ade973f0ab7ddcfc9bfa8f | - |
| app/src/main/res/xml/backup_rules.xml | production | 379 | 9 | 818a8aaaa316ab4df5303a2ceb6a026bf7da0d81e4d6f24600e8d7afbff33ca7 | - |
| app/src/main/res/xml/data_extraction_rules.xml | production | 810 | 19 | e1e8ea634744022fb4fb13ce97d3dd2051ee360f18f3915d54adc80140a59d6b | - |
| app/src/test/java/com/nanzhufeng/transcriber/core/TaskTransitionPolicyTest.kt | unit | 3225 | 99 | f0e3d13c60ea74ad3958cafaa4589a321bfb8e60b5213ebcba358bc138154a96 | declarations=7; tests=6 |
| app/src/test/java/com/nanzhufeng/transcriber/data/media/EmbeddedSubtitleParserTest.kt | unit | 1588 | 45 | 5ad746ee4dfb025426180c98849ad359ced22e3e62779f8032cc5449028e6dbc | declarations=5; tests=3 |
| app/src/test/java/com/nanzhufeng/transcriber/data/media/HistoryMediaPreviewStoreTest.kt | unit | 947 | 21 | 554e877fe6b1e6d9e87c66a02daac4e5a8919e727ee6a1fd36ac4d9124ee8d32 | declarations=3; tests=2 |
| app/src/test/java/com/nanzhufeng/transcriber/data/media/StreamingMonoResamplerTest.kt | unit | 1742 | 55 | d258b747468635ad195180a28d6c59e5f054dd7c93e36ccd673f25ba4a965779 | declarations=4; tests=3 |
| app/src/test/java/com/nanzhufeng/transcriber/data/modelstore/FileModelStoreTest.kt | unit | 3522 | 90 | 7ff0952657993542ea984e666914d3d488f1f328c5730420fc2edace43e7f962 | declarations=6; tests=4 |
| app/src/test/java/com/nanzhufeng/transcriber/data/modelstore/ModelAssetManagerTest.kt | unit | 8299 | 208 | 1bd1b3b251afadc19cf71301d1471fb16ffea7ccecadf1ac868a263f0a94ec9f | declarations=10; tests=5 |
| app/src/test/java/com/nanzhufeng/transcriber/data/modelstore/OfficialModelCatalogTest.kt | unit | 1452 | 27 | 302de6d02fad86a3ca144c9408adb6af30bf9b592684ed4f6337f2b638b79068 | declarations=2; tests=1 |
| app/src/test/java/com/nanzhufeng/transcriber/data/postprocess/OpenAiCompatibleTextPostProcessorTest.kt | unit | 3041 | 80 | b161de890332f684cff6c95d6ad519e3806632ce7a9a513717fd6c3fce345e1c | declarations=5; tests=4 |
| app/src/test/java/com/nanzhufeng/transcriber/data/result/TranscriptDocumentStoreTest.kt | unit | 1321 | 36 | 4ea463940ce73d3aada0b549161672d68d9b736286a954c19f8ef87433046c07 | declarations=2; tests=1 |
| app/src/test/java/com/nanzhufeng/transcriber/data/result/TranscriptionCheckpointStoreTest.kt | unit | 1997 | 55 | 8011648d274abbeb78566bd82981df6c5d06d0b4d8a5a8306f36f1ed7a1c30c3 | declarations=3; tests=2 |
| app/src/test/java/com/nanzhufeng/transcriber/domain/export/TranscriptExportServiceTest.kt | unit | 2612 | 81 | d45a90039085e8f6b1c95f7f66190e9c609161fd8a5df5267c8230a9fbc62c8c | declarations=4; tests=3 |
| app/src/test/java/com/nanzhufeng/transcriber/domain/invocation/AsrCostEstimatorTest.kt | unit | 1503 | 37 | 3a778994c3e2b680e810bd6b9b73602354fda774c7d2a9e7fe315dfb83c5ff58 | declarations=5; tests=4 |
| app/src/test/java/com/nanzhufeng/transcriber/domain/task/IslandDisplayControllerTest.kt | unit | 976 | 29 | 46e59cdb7e96b219bdd6a3010ffc264c031f551df29d13f3829be0d73c58358f | declarations=1; tests=2 |
| app/src/test/java/com/nanzhufeng/transcriber/domain/task/MainScreenVisibilityPolicyTest.kt | unit | 592 | 16 | 7db8bbb0361d31dd8c06b5d267d572ed1dea9f275570fffb9fbeb9d685dcf5ec | declarations=1; tests=2 |
| app/src/test/java/com/nanzhufeng/transcriber/domain/task/NanfengIslandStateTest.kt | unit | 1534 | 30 | 4c17be363f478863fc0cdfd7487a131aae5af615aa495ebb5e46d1622765db92 | declarations=2; tests=3 |
| app/src/test/java/com/nanzhufeng/transcriber/domain/task/TaskNotificationPresentationPolicyTest.kt | unit | 751 | 18 | 6a1de9922d28c6a2ec44aea1fab9b2e24b3938bab80151a8c515392c52bf58a2 | declarations=3; tests=2 |
| app/src/test/java/com/nanzhufeng/transcriber/domain/transcription/LocalTranscriptPunctuationRuleTest.kt | unit | 1252 | 35 | 72df42b6f4d4c00342cc70dc7dc4d3dff496a42eebf758c6a8b8bdea6f969d19 | declarations=6; tests=5 |
| app/src/test/java/com/nanzhufeng/transcriber/domain/transcription/TranscriptCompletionPolicyTest.kt | unit | 580 | 18 | 77b6609f661497dacfc7d23d1a8d10e12eb82b264cad0d22b22e09bea39500f3 | declarations=3; tests=2 |
| app/src/test/java/com/nanzhufeng/transcriber/engine/DashScopeFileTranscriptionClientTest.kt | unit | 5923 | 112 | 29e4b25214d96e1857ad6bbd02ae1c941bf1807f376edc1f9d1cd7b9e1a56724 | declarations=9; tests=8 |
| app/src/test/java/com/nanzhufeng/transcriber/engine/DashScopeFileTranscriptionEngineTest.kt | unit | 6752 | 113 | b3180b0930a5c65b36f61225bb6ef6007805de8a3eccb903b37d7d8703223b29 | declarations=9; tests=3 |
| app/src/test/java/com/nanzhufeng/transcriber/engine/PcmTranscriptionCoordinatorTest.kt | unit | 3917 | 98 | 307eeccdcec25f08f9a1cce364216835b7dec4d51c7bc8b27e7f3a35eef0be5c | declarations=11; tests=2 |
| app/src/test/java/com/nanzhufeng/transcriber/engine/QueueModelSessionOwnerTest.kt | unit | 4025 | 111 | 6774b9fcfaebf71725476e714d476e38f920253afe6f8a778f5935fbae593f22 | declarations=11; tests=4 |
| app/src/test/java/com/nanzhufeng/transcriber/engine/Qwen3AsrApiEngineTest.kt | unit | 3256 | 73 | dc721cc6a7a8dd82bc565fa0a24836b6f7fdb2ce3265d4dc276962db4e55c836 | declarations=3; tests=2 |
| app/src/test/java/com/nanzhufeng/transcriber/engine/SpeechPauseSegmenterTest.kt | unit | 3275 | 87 | 7548b5529d58e356f1cc8fe6b859d238ee2dc096fcab6c2f9555aeb1021a089c | declarations=10; tests=7 |
| app/src/test/java/com/nanzhufeng/transcriber/engine/benchmark/AsrBenchmarkEvaluatorTest.kt | unit | 1936 | 52 | 68d8cdc3e74374e74b219ebfe8cbe2ac6e453e4058f1c43b3e872d1f4055f7bb | declarations=4; tests=3 |
| app/src/test/java/com/nanzhufeng/transcriber/service/BackgroundTranscriptionQueueTest.kt | unit | 1805 | 41 | b97d480efdac7f945ca2227041c3128eeb0530e999b7408f330082b9dabcc808 | declarations=1; tests=3 |
| app/src/test/java/com/nanzhufeng/transcriber/ui/TranscriptionUiStateTest.kt | unit | 1118 | 36 | 1245797aaf378910f3b83f66fb9208fb09be21560584bd6b92298a9076e24bc3 | declarations=3; tests=2 |
| app/src/test/java/com/nanzhufeng/transcriber/ui/components/TranscriptPreviewFormatterTest.kt | unit | 641 | 21 | 723bd938f2853ebf5fb9210ff82b01634ba732f8f2dd2cc41d397418d9aa37f0 | declarations=4; tests=3 |
| branding/app_icon_master.png | binary | 1469241 | - | 70d2d94779db1a7bc529b837e969f4cf90235aa367392cc193a4f5a882b98838 | - |
| build.gradle.kts | config-tool-license | 287 | 6 | a914114036a8177cb58701729600afe2454af88a4ca23fe6ea0ba657e0710d7d | - |
| docs/CURRENT_HANDOFF.md | docs-evidence | 23291 | 126 | d681a82809b5b8515a4c95abb9069e1d1ecb5a7600c12473bce37e8e419ebf65 | headings=13 |
| docs/PROJECT_STATUS.md | docs-evidence | 4359 | 60 | 09495d6c31e5b612d7469fe7e27c6b7023f9c677c1a8a53a8159a547cca6954e | headings=9 |
| docs/architecture-governance.md | docs-evidence | 2947 | 39 | db7e09e6817747a674819f344fb8d1dc5fb1f68028722d600e82680895e452a4 | headings=5 |
| docs/asr-accuracy-strategy.md | docs-evidence | 8509 | 71 | 348505db3afb8ec892f6753fb8e23d04e4293405068354fc6749a02896c19552 | headings=7 |
| docs/background-island.md | docs-evidence | 5178 | 41 | e4f1679ca2156c141439d017d8b2983c31de1712e09b8e52e552269e87e8f0c7 | headings=5 |
| docs/decision-log.md | docs-evidence | 9705 | 97 | aca9bc64f27622df953e4cfff637996829028151b905556ee66d89ae8679d0d2 | headings=24 |
| docs/design-reference-index.md | docs-evidence | 4668 | 52 | 8f27b7228f02bfdc6be1cfb159c35847aa4b6abbdaf6cce81edaa33ef54bbf01 | headings=6 |
| docs/development-experience/README.md | docs-evidence | 2082 | 24 | 7331c2ac2f7d20d6ad27166b22ca4d24ee1269e3a6c964be2684febf07b7d64b | headings=5 |
| docs/development-experience/audit-20261011.md | docs-evidence | 7386 | 75 | 7e9c406c53554fec53c163a8db782dbc5e74fa24fa98dacef487c200e8ac0854 | headings=10 |
| docs/development-experience/context.md | docs-evidence | 2570 | 36 | 3471d74a9efa10f49dc1886865a68d95f1211002ba50cef979bc157371e465e2 | headings=9 |
| docs/development-experience/portable-rules.md | docs-evidence | 3177 | 31 | c56f92edd9cbfc92da6373e4b650c96e3ce4a24749313988fc1026f8838d1708 | headings=4 |
| docs/domain-rules.md | docs-evidence | 4759 | 55 | a1bbfc5fbe91a6b7d5feb8bf3198ed114b906edf98d97ab0e40e6dbdce614891 | headings=7 |
| docs/evidence/README.md | docs-evidence | 10264 | 116 | a23031b96809ca16e6125666667e103519bd9e71814b49a21b86c5fff4cdc174 | headings=9 |
| docs/evidence/emulator-engine-probe.png | binary | 194427 | - | c0078d6f054b899793060b0a620b6509b5f668dc0043090edb74379ac35afddc | - |
| docs/evidence/emulator-file-picker.png | binary | 508321 | - | 47e2c7658fa0f8b0af93c81f7e72372b226a199682c6e212f54c4c6e02d9d44b | - |
| docs/evidence/emulator-full-pipeline.json | docs-evidence | 499 | 12 | 7675650c239508b8df7100f1bfe473eba4e13f20678377bb88b0097cfa530509 | JSON parse OK |
| docs/evidence/emulator-main-workflow.png | binary | 259814 | - | 24bf9a30b3b41d3d8290bbe313a0daa94dffbedf25dc99176f4cd673f02f1b71 | - |
| docs/evidence/emulator-operational-transcription.png | binary | 242157 | - | 70d8f4c5bcf0980f434f83a9f49b100862fd3895cb238cfdec3d2e602fdbed53 | - |
| docs/evidence/emulator-real-inference.json | docs-evidence | 792 | 21 | b0db30672eceb0c8d40b90995991fe750b3891fc95d39238ff8c3745e630bfa3 | JSON parse OK |
| docs/evidence/emulator-ui-inner-history.png | binary | 132941 | - | 522ea73016ea240d0f47fbbc5b533939ef152f70bc022742077fe0db1dde1cad | - |
| docs/evidence/emulator-ui-inner-home.png | binary | 284136 | - | f4eeaed333b1d26bf7167816bf35088537946c028f666a66d474aac3ca82a4e4 | - |
| docs/evidence/emulator-ui-inner-settings.png | binary | 418860 | - | 7cdf821f2080b196fdc748684e826fa9e7b476aff738c92e962c7c26d65506f2 | - |
| docs/evidence/emulator-ui-outer-history.png | binary | 119381 | - | cfb37850960ce8f8e388bee8ce240bbb62d627c5f4ca3d147b9e8f2855d9772c | - |
| docs/evidence/emulator-ui-outer-home.png | binary | 230470 | - | 532c566bdfcc40c4e8844dbe756c6fbc42a6a0cab13fe483915eb47cd1e80cbd | - |
| docs/evidence/emulator-ui-outer-settings.png | binary | 311283 | - | d5a24522fc1c504305d0e4e594d43a60965ce1fc4392a58d84e7dccdc7f063d4 | - |
| docs/evidence/find-n5-operational-main.png | binary | 261834 | - | 580851e5e7f89657d73ab10d6f90e3b545dfb8b93769e1f995b8500b2f6a1c6a | - |
| docs/evidence/find-n5-signed-engine-acceptance.png | binary | 282894 | - | fa0298434a4242edc683b1c2a92eb0470ea2d4cb11900be3f5ccd495063a1e1a | - |
| docs/evidence/find-n5-ui-v5-inner-history.png | binary | 160181 | - | f6ea8d4b39d177230feacf9fb1b8fc914e8538235156b42522c4776787c07d2f | - |
| docs/evidence/find-n5-ui-v5-inner-home.png | binary | 297546 | - | a7304e7a6547a1ff7ffd5f2bdf7ebf8c9f92d82edc4d376c0a6bc9c58112f48a | - |
| docs/evidence/find-n5-ui-v5-inner-settings.png | binary | 417505 | - | 6f2dd98ad74de768faa67bead664c710aba894e24723e14d8a240a0f83ef7030 | - |
| docs/evidence/find-n5-ui-v5-outer-history.png | binary | 142190 | - | 51a201fe758e176fc08906ace9f33ccc412a27ea3fc02403f71d4eb3a10d30bf | - |
| docs/evidence/find-n5-ui-v5-outer-home.png | binary | 241727 | - | 092d8823a80e6f68eb2c9356cd8952b9e646651a0207cc4b06f318d9694c20cb | - |
| docs/evidence/find-n5-ui-v5-outer-settings.png | binary | 311469 | - | bf4f5c5aaac52ff8b1bc84d7521ab41138247a12a7340de01eed9d52b9cb44b1 | - |
| docs/evidence/find-n5-ui-v5-settings-persistence.png | binary | 314926 | - | af54e0264c04445e3af052c771b6348ae7e0d05820d23699d90ab7744814a79b | - |
| docs/evidence/find-n5-v6-60m-monitor.csv | docs-evidence | 2428 | 59 | e3490a2bf8c00714801fea923fb8a744a9036edc662cdff3511bf1cb79244576 | - |
| docs/evidence/find-n5-v6-worker-exit.txt | docs-evidence | 256 | 10 | 2c4f381d7dd40d6ff68c3381cdd07019fbc1dea101f3a49e06ceef6925877ce4 | - |
| docs/evidence/simulator-v6-background-active.png | binary | 282037 | - | 21d188abdcdd3ee580549a6164555982568bbb9ce49ad1928932830c3fa94b2e | - |
| docs/model-license-status.md | docs-evidence | 2358 | 23 | b801f0a1fb04b2758d7f7bba8d5b7c0d7262795ac894f4a3145f80f4fa48a91d | headings=3 |
| docs/next-codex-prompt.md | docs-evidence | 1392 | 9 | dc963e5ef79b78aac91bc7ac0fa1d0aeeeb0828fdc244fc294618d7d0e7d1a8a | headings=1 |
| docs/product-acceptance.md | docs-evidence | 7445 | 86 | 604420d0de795edd5791cc659d0b72d3c44ae4bb8e38af5b944cf07022f2003b | headings=9 |
| docs/product-brief.md | docs-evidence | 1655 | 26 | a4750135b920917fad7a7a7e729f44ed8cd7de51d2981eb1bb768342aeb55b88 | headings=5 |
| docs/qa/background-island-v0147/compact-final.png | binary | 1796683 | - | d09e48af30381daccc928d36951ad7f1fa525769f76fd6dae012b943b80ccbda | - |
| docs/qa/background-island-v0147/compact.png | binary | 1801908 | - | 3ecc924e0fd79c6c7a894d30b4ce03972041b15d26f650d2d7d892909cd65123 | - |
| docs/qa/background-island-v0147/contact-sheet.jpg | binary | 42281 | - | 6d4e4d522f7a5a93ed7ad23e4c6d141452ee925b4970e58841246f468e337f92 | - |
| docs/qa/background-island-v0147/expanded-final.png | binary | 1667171 | - | 85243e278bd74b0c6fb41b7d2dac7fae1b9e391764a2c9f84e0923d367ea7050 | - |
| docs/qa/background-island-v0147/expanded.png | binary | 1686471 | - | 757f4fbe001f154f3fcaf1246e1420181f1adbee891462ae42e9b588dfd76dcf | - |
| docs/qa/background-island-v0147/transcribing.png | binary | 1685040 | - | 6c43afbb89c7da9928ee321f5f278f205fc47261e81be8ae4533d8a47779bcdb | - |
| docs/qa/filetrans-v0145/settings-qwen-expanded.png | binary | 261280 | - | b942f17f0f498617c954c6ffa9c72a4f1e7158b0b79d9c7fe08f93fa353c9999 | - |
| docs/qa/final-regression-v0148/README.md | docs-evidence | 2219 | 26 | ecfd83f8a1352c655a602fafe07189cd5b1845756d045de47cae8a75520f6a76 | headings=5 |
| docs/qa/final-regression-v14/emulator-history.png | binary | 101388 | - | 36856a8ccaee41d8390cf69d46c0af6fa3777347e3437ffc4fa128e5a4698d29 | - |
| docs/qa/final-regression-v14/emulator-home.png | binary | 204333 | - | 08f083b3ef81926ad5bb0853b505c8d3fe47bbb7f69ae88baf74722546b89165 | - |
| docs/qa/final-regression-v14/emulator-settings.png | binary | 203044 | - | 682cc367d4880c0b380944d0d64bc3007fc589a3f2c781f6939e864a31777cdd | - |
| docs/qa/final-regression-v14/oppo-history.png | binary | 503348 | - | 77c662e59c6e35d33e720e039a154cef802d9f26bb7e0c8fa0ec42c4a0a9ef93 | - |
| docs/qa/final-regression-v14/oppo-home-completed.png | binary | 197135 | - | e070b3f3718411a8fc108a730be4dab6bec06958a211467af34f452d23506131 | - |
| docs/qa/final-regression-v14/oppo-media-editor.png | binary | 593497 | - | 15555a81e0e4c51614390b3aab37c5edf0d9c9547e0c3da319d1fcffd631e5fc | - |
| docs/qa/final-regression-v15/README.md | docs-evidence | 1963 | 38 | c93a9614584ce5aa9435963c9087918768b38722b0591d4e012fa4259001e40b | headings=5 |
| docs/qa/final-regression-v15/emulator-history-subtitle.png | binary | 139834 | - | cda52a573b6a8ec06b073790a808569baf9231b6c18aa6ec6b811b52f4244973 | - |
| docs/qa/final-regression-v15/emulator-home-subtitle-ready.png | binary | 236952 | - | a40db5ace3e2ade1570331452f3b80d3f0f3557851d035dc3ac6ef009be866a8 | - |
| docs/qa/final-regression-v15/emulator-media-editor.png | binary | 167258 | - | 12812d9d5f40c3a677fe8a85443cc0db5a1092bfed20f28b48a9d43406f3a1d8 | - |
| docs/qa/final-regression-v15/emulator-no-subtitle-waits-model.png | binary | 238236 | - | 801deeaf6b44cc0a91fd1a19739beb9b8ff03cd610570a970485ddf7ed01ea1a | - |
| docs/qa/final-regression-v15/emulator-settings.png | binary | 203993 | - | 37c87425ec7770944c1546959e6c661517584cd5939dc62283fcd98de24076d5 | - |
| docs/qa/model-architecture-v16/emulator-api-key-visible.png | binary | 293018 | - | 1daf533590bde6a3ac3db8dc400f200a39a15c0279120e8bf04a1328f4c2905b | - |
| docs/qa/model-architecture-v16/emulator-home.png | binary | 187344 | - | 841175d6acbf9179341b062c5a766a1aa57658c3b140c4993b98cdd66a48524c | - |
| docs/qa/model-architecture-v16/emulator-home.xml | docs-evidence | 13329 | 1 | 0baed2e4638df5ab7d82a1dfd573843465265a9aba755e7d930cb5e010ea0016 | - |
| docs/qa/model-architecture-v16/emulator-model-panel-local.png | binary | 234829 | - | 82bc818c3d889fd590760ade9711009c1ec5206b7a8a00c8ae87b1733711d252 | - |
| docs/qa/model-architecture-v16/emulator-model-panel-local.xml | docs-evidence | 22110 | 1 | b65c1821b05ed2d3a6135071724cab5b53346df91df72a8b79e84cc9467d0c83 | - |
| docs/qa/model-architecture-v16/emulator-model-panel-qwen.png | binary | 237393 | - | f26497cbb6505ced19ffd64d1e5ab1c0b6dfc3d715237343799b2f8749a7455f | - |
| docs/qa/model-architecture-v16/emulator-model-panel-qwen.xml | docs-evidence | 19875 | 1 | 62607e8ca1ae1de832cd2bdc33616ed17052c5eb2d04340cd22f439f0c2b2364 | - |
| docs/qa/model-architecture-v16/emulator-model-radio-qwen.png | binary | 254130 | - | 796c005f49b09d7c91d1457c7017cfe1cb98ef9ba015452f6375219b4aa1de5a | - |
| docs/qa/model-architecture-v16/emulator-model-radio-qwen.xml | docs-evidence | 20561 | 1 | 55be4013d5d6aff901aabcf65191bb985571ecb088ad75fd07243a5681aea32e | - |
| docs/qa/model-architecture-v16/emulator-model-radio.png | binary | 272295 | - | ce70aa9d2b92118975d6919049c8917da67692fa925796e0dd4bae55435496a7 | - |
| docs/qa/model-architecture-v16/emulator-qwen-confirmation.png | binary | 269477 | - | 63a30f9cccf835546114c110f872b11d975ff7dcb68788ffd700a3fef05483dd | - |
| docs/qa/model-architecture-v16/emulator-qwen-confirmation.xml | docs-evidence | 5684 | 1 | 7938b9b192483f3a7ce48aad49485163b567ffeda5f4ca93742202701877cafb | - |
| docs/qa/model-architecture-v16/emulator-qwen-selected.png | binary | 259461 | - | 87240daf0d1a1872b681960a2a7d3b1054ccaae466d2a97c5251a2b57381e2b5 | - |
| docs/qa/model-architecture-v16/emulator-qwen-selected.xml | docs-evidence | 19611 | 1 | 321ac9b81da878ed140ee5653292dd7776eaacffb70106cb5b58f26d95404aee | - |
| docs/qa/model-architecture-v16/emulator-settings-api-key-visibility.png | binary | 263258 | - | 70c5883ac4aced47dbde94b7934fc07f95f9e79f08c884ab6607fe2c30215d43 | - |
| docs/qa/model-architecture-v16/emulator-settings-appearance-first.png | binary | 216717 | - | c3a5fde1f9f081c48f5adcff0dd884e2dd7e8d8b5a86bff12b128fa3310b0169 | - |
| docs/qa/model-architecture-v16/emulator-settings-final.png | binary | 260486 | - | a852cfaa7370047a3073dd916654207487a68416588ee0ece0037c185ca64ff3 | - |
| docs/qa/model-architecture-v16/emulator-settings-final.xml | docs-evidence | 19614 | 1 | 112989f2690c8a7f2b7d0992915d1516148b54636664fe65374d5b2fcd04a505 | - |
| docs/qa/model-architecture-v16/emulator-settings-top.png | binary | 269377 | - | dbb2fa022280a5669ca3d4159c9b78cc8cecd64bf5b15dafb839c2b5188d4d00 | - |
| docs/qa/model-architecture-v16/emulator-settings.xml | docs-evidence | 21938 | 1 | fb7d093b0d9e8a5fae667514482e71ea20f778bbef99308ca6927b7680e69637 | - |
| docs/qa/model-architecture-v16/emulator-transcription-parameters-compact.png | binary | 189008 | - | 34fc05f5a8618c5db0ff4ca12fbce2fdf3436ba968ee28ceb153fd28231ce815 | - |
| docs/qa/model-architecture-v16/emulator-transcription-parameters-compact.xml | docs-evidence | 18446 | 1 | 04bb5ee94077194ff33ab8365cef3fb4c2b02cd4121ea7b846b5e018fd54ce59 | - |
| docs/qa/navigation-v0146/README.md | docs-evidence | 1181 | 11 | 620d79e44b6904e975287b95b3dad2a011ad90e2cacf685fa24f67d8bba520ce | headings=1 |
| docs/qa/navigation-v0146/acceptance.txt | docs-evidence | 849 | 7 | 455f742e35bf4ad8c364a83e8a4411cf8ee07bf71efc458546bb12e2bf15138b | - |
| docs/qa/navigation-v0146/contact-sheet.jpg | binary | 183154 | - | de46f807fb6b295caf92173dd10d7885eea5130b87ed573779b998c93a3e87c2 | - |
| docs/qa/navigation-v0146/inner-history.png | binary | 130380 | - | 32584cad0c0919cf7b4dec14d1a7b047b9ed8e4213981fa1465fab6d2034ed2a | - |
| docs/qa/navigation-v0146/inner-history.xml | docs-evidence | 9554 | 1 | 3f59a287fb6ad25e2c701862fcbb243dc8e3c40cf59450ebc5c1ea11ee248ee1 | - |
| docs/qa/navigation-v0146/inner-home.png | binary | 248448 | - | b803973b78e76ac7ecd3b3e7c7945d1f97650ae955de5a9c228b9d512ab7a635 | - |
| docs/qa/navigation-v0146/inner-home.xml | docs-evidence | 15456 | 1 | 770f5dec833afb7db2ad46f07e78bac22bf1a78c1ddff198ce7ea18fcce206a8 | - |
| docs/qa/navigation-v0146/inner-settings.png | binary | 318803 | - | 1d29ed371e9ac4296981e6dca41d16144df4483fb03f563c4cba47a60b35deee | - |
| docs/qa/navigation-v0146/inner-settings.xml | docs-evidence | 23684 | 1 | 0d6ab58b8da5673e5aef13e7324c526092e92c5318d6be69bb2bbe330e20d8f6 | - |
| docs/qa/navigation-v0146/outer-history.png | binary | 109545 | - | c23604b4142d92eb73f3b40d67c98ef3905b6be83bafd3002f11b4370f832802 | - |
| docs/qa/navigation-v0146/outer-history.xml | docs-evidence | 9541 | 1 | 18b4daa119a251a6f6f95995d7eda33a007f6e572457aa9305e6c12c1e3af538 | - |
| docs/qa/navigation-v0146/outer-home.png | binary | 180302 | - | 85bf2c985cc3df0eab63dd6d1c1ca8ef151baac670486b632bb97a009102cee9 | - |
| docs/qa/navigation-v0146/outer-home.xml | docs-evidence | 11937 | 1 | 63039d699fb4fcf2e4b7100a2b8ef3bea43c42165300db98e7410e909e7a73f4 | - |
| docs/qa/navigation-v0146/outer-settings.png | binary | 221300 | - | 582988340dafcbd258ab74bff33f15b28063f9aedef11a2d1527bbdacf33a8f3 | - |
| docs/qa/navigation-v0146/outer-settings.xml | docs-evidence | 17487 | 1 | ed6fceefadfe48954a6c9459b039e280fa21752d55c9bec7f82ae62fc69f8b9a | - |
| docs/qa/navigation-v0146/verify_navigation.rb | docs-evidence | 3063 | 66 | daa285c6d384fa3a205140e6f19897dcb4ede5f2cf40dc855a424f41d2ec8556 | - |
| docs/qa/oppo-v14-history-retained.png | binary | 525935 | - | f6397e056007511466df1929dbc940585e7bd6cd51b05304fe495bc5ba7ba7fa | - |
| docs/qa/readme-v0.14.0/home.png | binary | 188859 | - | 96eb46d1a10c88e811c667be2e63a466007abbe8a365087512d86a8110d65e56 | - |
| docs/qa/readme-v0.14.0/settings.png | binary | 202103 | - | 1198c050eab2972aa2e13babe4cafd29927bc502595239e502cf0be02e163212 | - |
| docs/qa/readme-v0.14.1/home-emulator-api35-v0141.png | binary | 185558 | - | 0ad0d7e013be9aea763aa6e8d3ba3826a76e2712f2442acbc5f9b882e4cfa01a | - |
| docs/qa/readme-v0.14.1/settings-emulator-api35-v0141.png | binary | 168387 | - | d8e8ff689f750792d57462147ab8c6a5acf39db55b3350945efbe3959ef0f290 | - |
| docs/qa/readme-v0.14.2/home-expanded-api35-v0142.png | binary | 183019 | - | 37438ebc924d006a215151cddb39c597b45bfa8d492da84af8ec42f3a6983832 | - |
| docs/qa/readme-v0.14.2/settings-expanded-api35-v0142.png | binary | 249749 | - | dfa743ac8c226ab169928ba4216e12df1a43432c47d6d975b46b896e58b77667 | - |
| docs/qa/readme-v0.14.3/home-expanded-api35-v0143.png | binary | 182301 | - | 476c069208bece53786e5aa9edae6e532f9406b40d0045aeeb22bca25efcb561 | - |
| docs/qa/readme-v0.14.3/settings-expanded-api35-v0143.png | binary | 248760 | - | 25cf24f81de2621e192e6d5839b65e2a3bdfdbf10f972d91c7623ef8708f6662 | - |
| docs/qa/readme-v0.14.4/settings-about-api35-v0144.png | binary | 244568 | - | 8f2633db43eecb18c0762b1c1bead97391747a2865bc20788b88d5454b237040 | - |
| docs/qa/readme-v0.14.8/home-expanded-api35.png | binary | 176537 | - | 7beca78a750d77b0a495c5e2c61e6829547326e3969647de02baf528e1c492e2 | - |
| docs/qa/readme-v0.14.8/settings-island-api35.png | binary | 200542 | - | 029085ca574224cb81c785baa9d8bdff43a81c44121f3716e66fc43de9fd52fa | - |
| docs/qa/sensevoice-final-regression/emulator-home.png | binary | 200880 | - | 13661f0f3f4d1f5951309555726b970d40f5c61705f07f821762e46a5a017845 | - |
| docs/qa/sensevoice-final-regression/emulator-model-selector.png | binary | 216113 | - | 2b277d182dd5b87da7d71d399bb7535a455aea86236b84c7c23a8fd60a3bd888 | - |
| docs/qa/sensevoice-final-regression/emulator-model-selector.xml | docs-evidence | 17583 | 1 | cd86cada087ae9fc64f22bd5f8f1e866d8ea6fc5a92c28a155f792f7382d8574 | - |
| docs/qa/sensevoice-final-regression/emulator-settings.png | binary | 264198 | - | e9145bf8e4ed9dcebda06ba5e381d5ed7bcbc42a2b8b76d16991434a52699fb8 | - |
| docs/qa/sensevoice-final-regression/emulator-settings.xml | docs-evidence | 22269 | 1 | b43b1fc1d72e5abc28d03fc86694b71326ea2d8a755ef17608e40269a0dbd149 | - |
| docs/qa/v10-ui/current.xml | docs-evidence | 26575 | 1 | 4824ca6e9a751c5c7ca7947680e2f492682a4e3b1dd3cc3f66340f5d50f15beb | - |
| docs/qa/v10-ui/history-edit-dialog.png | binary | 233586 | - | 4ae9be383fdfe8dcd072d67b38b8b30a775bc1903d889919ba2e2552ce5fc11d | - |
| docs/qa/v10-ui/history-edit-dialog.xml | docs-evidence | 10828 | 1 | 802f886d79fd56be9da64e6bd6bfe78f23e9949d4a7a770d729dc3b8d6ee593c | - |
| docs/qa/v10-ui/history-edited.xml | docs-evidence | 11160 | 1 | 0852823274096e70bb94181728bf4015aee51d3df1834d6cdad0591db8ebcc95 | - |
| docs/qa/v10-ui/history-muted-preview.png | binary | 262360 | - | 87608587a6a891a877b3eb5d307ef742d057ea401a2ac68b4d1deecb47216307 | - |
| docs/qa/v10-ui/history-muted-preview.xml | docs-evidence | 25185 | 1 | 626954c128590e820dcead35039d097b4d26b9abfa80f08115ce1ca994ee53f5 | - |
| docs/qa/v10-ui/history-result-seconds-copy.png | binary | 209999 | - | af32da85702644f0d17d63db37cec3b83c9749ae1f842483f1af8d3059920026 | - |
| docs/qa/v10-ui/history-result-seconds-copy.xml | docs-evidence | 11431 | 1 | cb95d7c5df37cd40d579ce4ec13193cd32493b102ed31e10feb8b5b527513886 | - |
| docs/qa/v10-ui/history.png | binary | 253770 | - | d3f13bbab1c90be19bb17a3a5c9e87681ed22d4e17d629f841ef6ba1fb838187 | - |
| docs/qa/v10-ui/history.xml | docs-evidence | 25185 | 1 | 626954c128590e820dcead35039d097b4d26b9abfa80f08115ce1ca994ee53f5 | - |
| docs/qa/v10-ui/home-all-selected.xml | docs-evidence | 7293 | 1 | bad7cca4dc1c15a09c606f04e9415686bf3d5d2fbf8b1bd203754809bbb84455 | - |
| docs/qa/v10-ui/home-muted-add-task.png | binary | 250643 | - | 04dbf9ee6cc13652715845dcc9ac3c9060ac971cf084f2a6778f67f351cd0a63 | - |
| docs/qa/v10-ui/home-muted-add-task.xml | docs-evidence | 25558 | 1 | d3da2bd02a6fa88a14a45ec05526b86c79b2c9b2db23d1785509fb2371fd0ea9 | - |
| docs/qa/v10-ui/home-selected.png | binary | 258588 | - | 423ddb5faea83576bf1eb72f5c82531afcfa1c9469c27fb1a4c8a91ca85115f5 | - |
| docs/qa/v10-ui/home-selected.xml | docs-evidence | 26575 | 1 | 4824ca6e9a751c5c7ca7947680e2f492682a4e3b1dd3cc3f66340f5d50f15beb | - |
| docs/qa/v10-ui/home.png | binary | 249233 | - | 1a4eec8e2cf58be2eddd80c343504cfb67aabd24d72da05f12527551c5d499d1 | - |
| docs/qa/v10-ui/home.xml | docs-evidence | 25556 | 1 | 9c1e9df8c601e1e61add321105c48fe3ac228f353d4dea31c6085548a6281d9c | - |
| docs/qa/v11-ui/emulator-burgundy-frost-settings.png | binary | 304941 | - | f655e913399f57e09e9c1b691a9aeda33c7eea0fc4f345a700e85bc483f00ce5 | - |
| docs/qa/v11-ui/emulator-home-unified-import.png | binary | 200415 | - | 5bf416b190efe31b4bdd8689b90192126883bca04c4de834eb01a45740afa5d5 | - |
| docs/qa/v11-ui/emulator-pending-model-reeditable.png | binary | 218383 | - | 8d5ec6b5dfe8a8ed4bfd59fda9195a9d1c718eeb4d05e39125dcbe5e3164e181 | - |
| docs/qa/v11-ui/oppo-v11-background-transcription.png | binary | 223729 | - | 6911ac5bbe9899254ada25d9878b31558e708b5c3fa3f492d2404320a0afc87a | - |
| docs/qa/v12-ui/emulator-secure-postprocess-settings.png | binary | 264904 | - | 01610e6b0b09ad8e310e46240efe17dcf574734c2a69a045614f745312d4aed8 | - |
| docs/qa/v13-ui/emulator-audio-review.png | binary | 80413 | - | 92ce321a8ed152bd0f3867cc7188c16656e310cf809cf2cbb85cbf73659c4997 | - |
| docs/qa/v13-ui/emulator-history.png | binary | 286939 | - | c09b2bf44c7f07c908c1e482804c0fa0ee803551d9f2aff4e82e60e588956362 | - |
| docs/qa/v13-ui/emulator-home.png | binary | 251523 | - | a77dbdd95daa7f8c53c2d0c6c89672ad581ab39552ad490b1085778ed2f4e1ea | - |
| docs/qa/v13-ui/emulator-media-review.png | binary | 96895 | - | 002f26035bd57793d5c97fb4456a83d08aec9aeb2e4a6edad54098790f94115f | - |
| docs/qa/v13-ui/emulator-settings.png | binary | 213415 | - | 29c4297dce212cc86fb7e50f0e588655561ad82c2659e113aaa5aed24a6842c5 | - |
| docs/qa/v13-ui/emulator-video-history.png | binary | 292562 | - | fecd5cb044cef8f3a717c0d4ec074602df7dee1619b7fa1913aa49ff5a2e3226 | - |
| docs/qa/v13-ui/emulator-video-review.png | binary | 98290 | - | 1029a1244b4915a7c4c7e824c8ee710d72047d669a97f9b6c9f073ef096e80c4 | - |
| docs/qa/v13-ui/oppo-v13-after-install.png | binary | 189448 | - | 2edf66f84fb9a0141d33d04457bd376a05baac1aae5f9dd6b87f3e34e2ce63d8 | - |
| docs/qa/v13-ui/oppo-v13-history-retained.png | binary | 218448 | - | 0f9b342f0b9c15117eda25bba6848e3d53bd002f378809a08b78fc68d7020f39 | - |
| docs/qa/v14-ui-emulator-history-media-card.png | binary | 315746 | - | a8a60d6cbd3cc1cfe4a1bb4501d2fb0a282a8197e40e917d945f1a0cd441ca9e | - |
| docs/qa/v14-ui-emulator-home-media-card.png | binary | 258389 | - | c42b0e75da20f148ab7bb31fe960cac870bb06cb5a42c409d932986c1e6b92c1 | - |
| docs/qa/v14-ui-emulator-media-review-editor.png | binary | 180446 | - | e7815eb12ff00838e41ad5c79b4b832fcb2349796d26d17b647eefb930863c82 | - |
| docs/qa/v8-ui/history-inner.png | binary | 167756 | - | 65b883ee9490f329245214e06107b8b8c0ed8b9b6932ebe516ce90e49d7e7755 | - |
| docs/qa/v8-ui/history-outer.png | binary | 197678 | - | 3252fa750c07b2218bb300e9bfaffb812288476c8f2ac7d17e52e41f8c49056c | - |
| docs/qa/v8-ui/history-result-secondary.png | binary | 147729 | - | 661c0da6d3b3a80e72b2809fa839a929c4fb7a75b1e11017d49c8df4fcc0a6ff | - |
| docs/qa/v8-ui/home-inner.png | binary | 375525 | - | 2c0835c6e06e9bb36039f0ac682a9dbf1eef08364f8127ac7d24ce8e22e54575 | - |
| docs/qa/v8-ui/home-outer.png | binary | 264623 | - | 1e75de5077704d5d014b3c677432cd25bb49c37db314839868675d9e79eba54a | - |
| docs/qa/v8-ui/settings-inner.png | binary | 389190 | - | ab6925610c0d9ac0feb0e9dc32fee68fbbc1fe5c032864c36d9a8eec3687a657 | - |
| docs/qa/v8-ui/settings-outer.png | binary | 286183 | - | 8eb85028b5d8c63e19a9ce5fe553760b3e3bb0eaeb05e5ea18d1424b07b9f5b0 | - |
| docs/qa/v9-ui/history-inner.png | binary | 333071 | - | 82db1d59823fac9ea4ddae1818ab908a86e89965ae6615941a43f8a9d5669be9 | - |
| docs/qa/v9-ui/history-outer.png | binary | 279245 | - | 8e0a8cf9ef397e39b54477e66d2356f6182b882cc30848d9448dfe2d7bc0d0d1 | - |
| docs/qa/v9-ui/history-result-secondary.png | binary | 181890 | - | d06737b7034b06b7a8fcdce2067ebb3df746d2e48924b7be8e14aaf815eed82d | - |
| docs/qa/v9-ui/home-inner.png | binary | 276385 | - | 5de60f2b0554fec812e2a3316e540dea0d71ac4631014af68c00c88a5159852b | - |
| docs/qa/v9-ui/home-outer.png | binary | 277137 | - | e59546a9ba97b60f5c9dd30cf6f56576305bed5d0bbc81e540d1f3005df751a5 | - |
| docs/qa/v9-ui/settings-inner.png | binary | 383296 | - | a7ee32f622b9f02a7e21e0d4ae30ee8e90af1c2b52ba176a9f471617309bda08 | - |
| docs/qa/v9-ui/settings-outer.png | binary | 295882 | - | b424174a7e7a9aee2149f932a0deda24315227586ac9c5413c07f0fcffc7b29f | - |
| docs/releases/v0.14.8-verification.md | docs-evidence | 2837 | 35 | 577233abc540a463a5b3a06d7ec90e121055b3069727ecbdff4b5b8979c18d7e | headings=4 |
| docs/signing-and-device-acceptance.md | docs-evidence | 2519 | 51 | 87610dbaf38a2f39ea1ec3e590546474711a771b35f955793d88c6b041e802f4 | headings=6 |
| docs/ui-contract.md | docs-evidence | 8055 | 67 | 6f1450aa585de600eaf68e1a539e2711e3e555078f7a3d856c61d03fbabf5dca | headings=9 |
| gradle.properties | config-tool-license | 184 | 5 | a0130d49ab892d5bf7141ae018891c8b6b5de780fc8aaa1922ae83eeaa2dfa3a | - |
| gradle/wrapper/gradle-wrapper.jar | binary | 43453 | - | cb0da6751c2b753a16ac168bb354870ebb1e162e9083f116729cec9c781156b8 | - |
| gradle/wrapper/gradle-wrapper.properties | config-tool-license | 250 | 7 | 712d9bc912d4b0d42b0faf8c6afeed13ae31ced75ca28463b1dfc4ced1a4bc9a | - |
| gradlew | config-tool-license | 8692 | 249 | fc977a94723af68aaffa4e5d60496fb4aeed1884b6b19e5e2f2fd7612673313d | - |
| gradlew.bat | config-tool-license | 2826 | 92 | 95e8ae8e8a256a2f066cdd745d56f7a3fc87aa058a2e4beb483300db7171bd1f | - |
| scripts/verify_contract_checkpoint.rb | config-tool-license | 6995 | 102 | 81306743d29b850f7f6e31581abbd3726b8b8cda0da870699dbb325b27336707 | - |
| scripts/复制南枫转写签名密码.command | config-tool-license | 484 | 15 | f0bea702e7367ad8873a7bbfad18002df8d0c5faa58aa5b39570973068ade0d2 | - |
| settings.gradle.kts | config-tool-license | 345 | 18 | 2b6459eb45c5a5b5974df84a18afb164836f17ff9f247d8b9df2b8c186477499 | - |
| third_party/sherpa-onnx/LICENSE | config-tool-license | 11358 | 202 | cfc7749b96f63bd31c3c42b5c471bf756814053e847c10f3eb003417bc523d30 | - |
| third_party/whisper.cpp | submodule | - | - | - | - |

## 全部本地可达提交（不读取聊天或私有环境）

| 提交 | 日期 | 主题 | 相对首父提交的文件数 |
| --- | --- | --- | --- |
| 2bad9c2d559f69642ede2e4acd7a37b2599d9b71 | 2026-10-11 | docs: checkpoint final regression contracts and incremental development experience | 14 |
| 5152a3d73626c561124c2fb6aa4546fc5d6497d5 | 2026-10-11 | docs: record verified v0.14.8 GitHub release delivery | 3 |
| 290e03f93fe6e9df43c243d3a04d2839ba9720be | 2026-10-11 | Release v0.14.8: file transcription, background island and settings cleanup | 74 |
| b02799b5df953504716fa9b879d484dc3799bfde | 2026-09-15 | feat(settings): add about information | 4 |
| 6b920b194a28362a572beebf785f50632368ca5e | 2026-09-11 | 发布 v0.14.3 明确导出与界面优化 | 14 |
| dc89a5b288d294def35694ec5b60a22ca9e5360d | 2026-09-01 | 发布 v0.14.2 正式版 | 7 |
| cca8754c9985b6c40d4b2530e1ffa14ed73195c6 | 2026-08-31 | 发布 v0.14.1 正式版 | 17 |
| 2436f69cc189b5278300d568ae48dd7297e068e7 | 2026-08-30 | docs: refresh official README and screenshots | 3 |
| 990f23faec13122195aa4a8edae01562fe18f93a | 2026-08-30 | merge: SenseVoice and history export experience | 87 |
| ceaebf3e2a0bab4aabb80067a7b9d669427ed31f | 2026-08-30 | feat: finalize SenseVoice and history export experience | 87 |
| 78c79471aaf2ddbff1bda584fbd1195871f4ca13 | 2026-07-21 | docs: mark unfinished build as visible release | 2 |
| a5b3cb91242e6850273da9eac83986370206ae34 | 2026-07-21 | docs: link unfinished Android prerelease | 2 |
| 6a6071831396dbb7f1183938b0eab4cd6248b8f9 | 2026-07-21 | docs: add current interface previews | 1 |
| d5e440904460ec6a3a195a3161a6d236e71f5a45 | 2026-07-21 | docs: record GitHub publication state | 2 |
| aa5cb64128d7cba86f01c343a30c4098c7c3c951 | 2026-07-21 | docs: mark project unfinished and paused | 4 |
| 42ae0d2c830efe895d8cf2c0a1b00547728020e3 | 2026-07-21 | docs: record v15 checkpoint handoff | 2 |
| 59ecf86e502e364a43502558bcc385cb451f6e15 | 2026-07-21 | checkpoint: freeze Android v15 final regression baseline | 198 |

## 供应商边界

whisper.cpp 固定提交：`f049fff95a089aa9969deb009cdd4892b3e74916`；内部跟踪项：1882。本轮核对 submodule 来源、集成 CMake/JNI 与许可，不声称逐行审计上游或重现 JNI 二进制构建。

## 未跟踪范围

- `.agents/skills/nanfeng-transcriber-doc-sync/scripts/inventory.rb`：不自动纳入已提交基准。
- `design-qa.md`：不自动纳入已提交基准。
