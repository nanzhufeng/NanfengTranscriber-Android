# 2026-10-12 全部合同统一审计

## 范围与唯一入口

底层HEAD `2bad9c2d559f69642ede2e4acd7a37b2599d9b71`；开始已有上轮25个文件的未提交文档/Skill增量和原未跟踪design-qa.md，全部保留。当前业务源码与v0.14.8一致；本轮只改合同、路由、开发检查和动态摘要，未改App/测试/Gradle、冻结APK、账号、设备或远端。

核对所有根级产品/领域/架构/UI/设计来源/后台/ASR/许可/签名/验收合同，并登记README、AGENTS、状态、交接、决策、档案/经验职责。版本化QA、Release、增量经验、供应商与未跟踪草稿分类处理，不把旧证据改成新结果。唯一入口为 [合同索引](../../contracts-index.md)。本轮不重新联网验证平台文档、模型价格或许可证，不推翻原核查日期。

## 新发现冲突与统一结果

| 冲突/不一致 | 实际代码依据 | 处理与保留的限制 |
| --- | --- | --- |
| 历史可按“转写文字”搜索容易被误解为全文 | HistoryScreen只contains(historyPreviews)；ViewModel常规读取360字符压缩120 | domain/UI改成文件名与短摘要；全文搜索缺口保留 |
| 编辑正文与私有完整结果混用 | updateTranscriptDraft只更新uiState；非SRT导出复制草稿 | 明确不写回JSON、不保证重启保存；不是已实现修订协议 |
| 字幕“无需模型”被写成无条件启动 | runner先查源/模型版本/云端Key，再extract | 明确先于本地资产加载，不绕过云端Key存在检查 |
| 每媒体独立输出目录 | OutputStore.rootDocumentUri后直接createFile | 私有task目录与SAF目标目录分开；不新增子目录 |
| SKIP可被“已保存”文案误报新写入 | OutputStore返回Skipped；ViewModel统一成功消息 | 记录文案与事实差异；不改UI，不把跳过当新写入 |
| 热复用只写模型/线程 | QueueModelSessionOwner同时核对provider，ModelSessionManager核对路径 | 改为provider/真实路径/有效线程匹配，远端任务不跨文件复用 |
| 取消可被理解成服务端撤销/免计费 | Client没有cancel方法，Service取消本地工作 | domain/后台明确远端仍可能执行/计费 |
| 去重被泛化成全任务保证 | enqueueSources仅当前URI/PendingSource.key去重 | 明确没有全Room/历史内容哈希去重 |
| 历史状态/模型筛选、重试的旧设计描述 | HistoryScreen只有时间/名称/摘要筛选与完成记录 | 来源索引与UI删除不存在的入口承诺，重试属首页 |
| 所有设置控件写DataStore的泛化 | 模型存储/Keystore/system intents分别持有事实 | UI/架构按实际所有者区分 |
| 原生分层已完全达标的表述 | ViewModel仍协调文件清理、输入/历史/导出 | 区分目标边界与既有大文件职责，保留技术债 |
| 后续降噪/响度建议像已接入能力 | 当前解码/引擎链未新增该处理 | ASR策略标记后续建议，不虚构功能 |
| 旧v15暂停/私有远端/设备断开续接路线 | 当前状态Active、公开0.14.8历史发布 | next prompt改为实时路由，不自动授权覆盖或业务验收 |
| 来源/几何/历史与现行多处正文 | design来源7月/10月身份、UI现有布局 | 设计来源不再重复尺寸，旧决策/复盘标明历史/快照 |

源码根目录：[生产代码](../../../app/src/main/java/com/nanzhufeng/transcriber)。精确入口：ui/screens/HistoryScreen.kt、ui/TranscriptionViewModel.kt、service/TranscriptionTaskRunner.kt、data/output/AndroidTranscriptOutputStore.kt、engine/QueueModelSessionOwner.kt、engine/ModelSessionManager.kt、engine/DashScopeFileTranscriptionClient.kt。这些是本轮静态对照，不冒充新设备/真实服务运行。

## 防漂移与验证

- 扩展既有 [verify_contract_checkpoint.rb](../../../scripts/verify_contract_checkpoint.rb)，新增 `--doc-root` 用于保存的工作树文档对照；与 `--doc-ref` 互斥，源码始终用当前checkout。没有第二套竞争合同检查脚本。
- 对本轮开始根级合同快照运行新增检查：预期FAIL，9项，包括七个实现边界、唯一职责登记及旧续接入口；原有检查仍通过。相关增量context用于链接检查，不将新增README路由当旧语义。
- 更新后同一门禁、`--baseline checkpoint-v0.14.8-20261011-final --apk 'app/build/outputs/apk/release/南枫转写.apk' --unit-results app/build/test-results/testDebugUnitTest`：97项PASS（扩展链接覆盖前的阶段结果）。JUnit读取既有91项零失败/错误/跳过报告，未将报告日期刷新为今日重新执行。
- 原有56/57计数是各自旧阶段事实，不倒改旧QA。脚本断言只发现结构/源码边界/文字漂移，不证明业务运行、全文能力、收费取消或所有未来冲突。
- 两个新参数互斥/非法目录需返回失败；Ruby语法、文档/Skill链接与空白检查在收尾确认。最终完整检查数量由本节末尾实际执行结果记录，不预设固定数量。

最终执行：扩展到索引登记的全部根级合同/状态/历史/快照后，合同门禁306项PASS；五类Skill及233个本地链接检查PASS；两个脚本Ruby语法、Git空白检查PASS；互斥参数与不存在快照目录两项负向参数测试PASS。业务源码/测试/构建差异为空，冻结包摘要匹配。以上数量属于本次命令/文件集合，不硬编码为未来完成门槛。

本轮没有重新执行JVM/lint/编译或设备测试；读取并复核既有JUnit报告，不冒称新回归运行。官方Skill quick_validate仍缺PyYAML，Ruby结构校验不是官方PASS。工作流审计原有全局软警告保持不动；没有新增全球规则。

## 本轮文件范围

新增：docs/contracts-index.md、本审计记录。修改：根AGENTS/README；domain、architecture、UI、product brief/acceptance、design-reference、ASR策略、background、decision-log、next prompt、PROJECT_STATUS/CURRENT_HANDOFF；增量README与完整档案仅加历史/现行路由；既有合同检查和文档资产校验脚本。沿用上一轮未提交成果，不自动提交或推送。

## 保留风险与停止条件

真实百炼/费用、OPPO长测/系统续期与超时、准确率Ground Truth、精确权重许可与JNI可重现性仍缺证据；编辑持久化、全文搜索、远端取消、SKIP反馈、跨文件/数据库事务属于明确实现限制，未借合同整理实现或“消除”。后续只在具体功能改动时选择相应修复和行为回归。全球Skill/记忆、冻结交付和主设备均不动；历史design-qa.md保留。
