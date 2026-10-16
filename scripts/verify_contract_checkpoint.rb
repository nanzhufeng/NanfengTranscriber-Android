#!/usr/bin/env ruby
# Read-only developer gate. Does not build/install, call providers, or alter APK/Git.
require 'optparse'
require 'open3'
require 'pathname'
require 'json'
require 'digest'
require 'rexml/document'

ROOT = Pathname.new(__dir__).parent.expand_path
options = {}
OptionParser.new do |parser|
  parser.on('--doc-ref REF', 'Read the three contracts from a historical Git snapshot') { |v| options[:ref] = v }
  parser.on('--doc-root PATH', 'Read docs from a preserved workspace snapshot; sources stay current') { |v| options[:doc_root] = Pathname.new(v).expand_path }
  parser.on('--baseline REF', 'Require production code/config to match this checkpoint') { |v| options[:baseline] = v }
  parser.on('--apk PATH', 'Verify a frozen APK without rebuilding it') { |v| options[:apk] = v }
  parser.on('--release-record PATH', 'Read the expected APK digest from this versioned record') { |v| options[:release_record] = v }
  parser.on('--unit-results PATH', 'Require successful JUnit XML reports') { |v| options[:unit] = v }
end.parse!
abort('Choose only one of --doc-ref and --doc-root') if options[:ref] && options[:doc_root]
abort('Invalid --doc-root directory') if options[:doc_root] && !options[:doc_root].directory?

errors = []
checks = []
read = ->(path) { ROOT.join(path).read }
contract = lambda do |path|
  if options[:ref]
    output, error, status = Open3.capture3('git', 'show', "#{options[:ref]}:#{path}", chdir: ROOT.to_s)
    raise "Cannot read scoped contract #{path}: #{error.lines.first}" unless status.success?
    output
  elsif options[:doc_root] && path.start_with?('docs/')
    options[:doc_root].join(path).read
  else
    read.call(path)
  end
end
assert = lambda do |condition, label|
  condition ? checks << label : errors << label
end

ui = contract.call('docs/ui-contract.md')
domain = contract.call('docs/domain-rules.md')
acceptance = contract.call('docs/product-acceptance.md')
schema = read.call('app/src/main/java/com/nanzhufeng/transcriber/data/task/TranscriptionDatabase.kt')[/version\s*=\s*(\d+)/, 1]
assert.call(acceptance.include?("Room v#{schema}"), "验收合同与实际 Room schema v#{schema} 一致")
assert.call(%w[SenseVoice Qwen Paraformer].all? { |name| ui.include?(name) } && !ui.include?('极速 Base'), 'UI 模型合同是现行三档，不是旧 Whisper 档位')
default_model = read.call('app/src/main/java/com/nanzhufeng/transcriber/data/settings/TranscriptionSettingsRepository.kt')[/const val DEFAULT_MODEL = "([^"]+)"/, 1]
assert.call(ui.include?(default_model), 'UI 合同默认模型与当前设置一致')
state_source = read.call('app/src/main/java/com/nanzhufeng/transcriber/domain/task/TranscriptionTaskState.kt')
states = state_source[/enum class TranscriptionTaskState\s*\{([^}]+)\}/m, 1].scan(/\b[A-Z][A-Z_]+\b/)
pipeline = domain.lines.find { |line| line.include?('`QUEUED ->') }.to_s.scan(/\b[A-Z][A-Z_]+\b/)
assert.call(!pipeline.empty? && (pipeline - states).empty?, '领域主流程只使用真实持久化状态')
assert.call(%w[WAITING_INPUT WAITING_MODEL RECOVERY_REQUIRED NO_SPEECH CANCELLED].all? { |s| domain.include?(s) }, '领域等待/恢复/无语音/取消没有被省略')
assert.call(acceptance.include?('DashScopeFileTranscriptionEngine') && !acceptance.include?('| 云端高精度模型 | `Qwen3AsrApiEngine`'), '新云端与旧任务兼容所有者分开')
assert.call(!acceptance.include?('约 5 分钟分块') && acceptance.include?('30 秒'), '验收合同不沿用旧固定五分钟分块')
assert.call(ui.include?('内屏移除左侧栏') && !acceptance.include?('左侧导航和语义重排'), '导航合同没有复活旧内屏左栏')
assert.call(ui.include?('不再显示“隐私与诊断”') && ui.include?('四段小字'), '设置精简合同已固化')

overlay = read.call('app/src/main/java/com/nanzhufeng/transcriber/service/TaskIslandOverlay.kt')
width = overlay[/root.minimumWidth.*?dp\((\d+)\)/, 1].to_i
touch = overlay[/root.minimumHeight.*?dp\((\d+)\)/, 1].to_i
inset = overlay[/InsetDrawable\(surface, 0, dp\((\d+)\)/, 1].to_i
padding = overlay[/root.setPadding\(dp\(if \(expanded\) \d+ else (\d+)\)/, 1].to_i
assert.call([width, touch, touch - inset * 2, padding].all? { |v| v.positive? && ui.include?("#{v}dp") }, '灵动岛合同几何与实际所有者一致')
background_position = overlay.index('root.background = if')
padding_position = overlay.index('root.setPadding')
assert.call(background_position && padding_position && background_position < padding_position, '背景赋值后恢复实际内边距')

# Source-derived boundaries, not proof of runtime correctness. Missing statements are drift.
unless options[:ref]
  prefix = 'app/src/main/java/com/nanzhufeng/transcriber/'
  runner = read.call(prefix + 'service/TranscriptionTaskRunner.kt')
  view_model = read.call(prefix + 'ui/TranscriptionViewModel.kt')
  history = read.call(prefix + 'ui/screens/HistoryScreen.kt')
  output = read.call(prefix + 'data/output/AndroidTranscriptOutputStore.kt')
  draft = view_model[/fun updateTranscriptDraft\([^}]+\}/m].to_s
  assert.call(history.include?('state.historyPreviews[it.id]') && domain.include?('不是全文索引') && ui.include?('不是全文搜索'), '历史搜索合同限定文件名与短摘要')
  assert.call(draft.include?('_uiState.update') && !draft.include?('documentStore.save') && view_model.include?('documentStore.saveEdited(path, text)') && domain.include?('点击保存才原子替换transcript.json') && ui.include?('内存草稿'), '输入草稿与明确持久保存分离')
  key_check = runner.index('usesQwenApi && !container.textApiCredentials.hasApiKey()')
  subtitle = runner.index('embeddedSubtitleExtractor.extract(')
  assert.call(key_check && subtitle && key_check < subtitle && domain.include?('云端模式还先检查Key密文是否存在') && acceptance.include?('云端还需Key存在'), '字幕预检合同保留真实云端Key顺序')
  assert.call(output.include?('val outputDirectory = rootDocumentUri(treeUri)') && domain.include?('不额外创建媒体子目录') && !domain.include?('每个媒体建立独立结果目录'), '外部输出合同不虚构媒体子目录')
  assert.call(output.include?('AutomaticExportResult.Skipped') && domain.include?('返回Skipped，不写新字节') && domain.include?('UI按Written/Skipped显示'), 'SKIP与UI文案的已知差异没有被假定为新写入')
  queue = read.call(prefix + 'engine/QueueModelSessionOwner.kt')
  assert.call(queue.include?('current.provider != provider') && queue.include?('current.threadCount != threadCount') && domain.include?('provider、模型真实路径和有效线程数'), '会话合同包括provider/路径/线程键')
  client = read.call(prefix + 'engine/DashScopeFileTranscriptionClient.kt')
  assert.call(!client.match?(/(?:suspend\s+)?fun\s+cancel\s*\(/) && domain.include?('没有服务端取消接口'), '取消合同不承诺远端撤销或免计费')
  index_path = options[:doc_root] ? options[:doc_root].join('docs/contracts-index.md') : ROOT.join('docs/contracts-index.md')
  index = index_path.file? ? index_path.read : ''
  entries = index.scan(/^\| `([^`]+\.md)` \| (CURRENT|GUARD|ROUTE|STATUS|HISTORY|SNAPSHOT) \|/)
  names = entries.map(&:first)
  actual_root = options[:doc_root] || ROOT
  actual_docs = actual_root.join('docs').glob('*.md').map { |p| p.relative_path_from(actual_root).to_s }
  assert.call(!entries.empty? && names.uniq.size == names.size && (actual_docs - names).empty?, '所有根级合同/状态/快照已唯一登记职责')
  entries.each { |path, _role| assert.call(ROOT.join(path).file?, "登记文档存在：#{path}") }
  prompt = contract.call('docs/next-codex-prompt.md')
  assert.call(prompt.include?('contracts-index.md') && !prompt.include?('当前候选为') && !prompt.include?('暂时搁置'), '续接入口不复活旧候选或暂停约定')
end

core_names = %w[domain/task/IslandDisplayController domain/task/MainScreenVisibilityPolicy domain/task/NanfengIslandState domain/task/TaskNotificationPresentationPolicy engine/DashScopeFileTranscriptionClient engine/DashScopeFileTranscriptionEngine service/BackgroundTranscriptionQueue service/TaskIslandOverlay service/TranscriptionForegroundService service/TranscriptionTaskRunner ui/components/TopStatusIsland ui/screens/BackgroundIslandSettingsCard]
core = core_names.map { |path| "app/src/main/java/com/nanzhufeng/transcriber/#{path}.kt" }
_, _, tracked = Open3.capture3('git', 'ls-files', '--error-unmatch', '--', *core, chdir: ROOT.to_s)
assert.call(tracked.success? && core.all? { |path| ROOT.join(path).file? }, "#{core.size} 个核心新增/所有者文件已纳入 Git")
untracked, _, result = Open3.capture3('git', 'ls-files', '--others', '--exclude-standard', '--', 'app/src/main', 'app/src/test', chdir: ROOT.to_s)
assert.call(result.success? && untracked.strip.empty?, '没有遗漏未跟踪的生产或测试核心文件')
if options[:baseline]
  _, _, result = Open3.capture3('git', 'diff', '--quiet', options[:baseline], '--', 'app/src/main', 'app/src/test', 'app/build.gradle.kts', chdir: ROOT.to_s)
  assert.call(result.success?, '生产代码和测试与代码 checkpoint 一致')
end

unless options[:ref]
  docs = (%w[docs/ui-contract.md docs/domain-rules.md docs/product-acceptance.md docs/architecture-governance.md docs/development-experience/README.md docs/development-experience/context.md docs/development-experience/audit-20261011.md docs/development-experience/portable-rules.md] + entries.map(&:first)).uniq
  docs.each do |path|
    text = contract.call(path)
    text.scan(/\[[^\]]+\]\(([^)]+)\)/).flatten.each do |target|
      next if target.match?(/\A(?:https?:|mailto:|#)/)
      linked = ROOT.join(path).dirname.join(target.split('#').first).cleanpath
      assert.call(linked.to_s.start_with?(ROOT.to_s + '/') && linked.exist?, "持久文档链接有效：#{path} -> #{target}")
    end
    assert.call(!text.match?(/gh[op]_[A-Za-z0-9]{20,}|sk-[A-Za-z0-9]{20,}|BEGIN [A-Z ]*PRIVATE KEY|\/Users\/|`\/tmp\//), "文档没有疑似实际秘密或临时私有路径：#{path}")
  end
end

if options[:unit]
  files = Pathname.new(options[:unit]).glob('TEST-*.xml')
  totals = Hash.new(0)
  files.each do |path|
    attributes = REXML::Document.new(path.read).root.attributes
    %w[tests failures errors skipped].each { |key| totals[key] += attributes[key].to_i }
  end
  assert.call(totals['tests'].positive? && totals['failures'].zero? && totals['errors'].zero? && totals['skipped'].zero?, "JUnit 最终回归：#{totals}")
end
if options[:apk]
  record = read.call(options[:release_record] || 'docs/releases/v0.14.8-verification.md')
  expected = record[/APK SHA-256.*?([a-f0-9]{64})/, 1] || record[/SHA256\s*`([a-f0-9]{64})`/, 1]
  assert.call(expected && File.file?(options[:apk]) && Digest::SHA256.file(options[:apk]).hexdigest == expected, '冻结 Release 字节未改变')
end

puts JSON.pretty_generate({result: errors.empty? ? 'PASS' : 'FAIL', passed: checks.size, failures: errors})
exit(errors.empty? ? 0 : 1)
