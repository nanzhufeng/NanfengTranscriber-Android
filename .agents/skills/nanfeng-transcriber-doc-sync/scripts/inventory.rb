#!/usr/bin/env ruby
# Read-only project inventory. Emits Markdown; never builds, deploys or edits files.
require 'open3'
require 'digest'
require 'json'

root = File.expand_path('../../../..', __dir__)
Dir.chdir(root)
def git(*args)
  out, status = Open3.capture2('git', *args)
  abort("Git inventory failed: #{args.first}") unless status.success?
  out
end
def cell(value)
  value.to_s.gsub('|', '\\|').gsub(/[\r\n]/, ' ')
end
paths = git('ls-files', '-z').split("\0")
rows = paths.map do |path|
  if File.directory?(path)
    [path, 'submodule', '-', '-', '-', '-']
  else
    size = File.size(path)
    sample = File.binread(path, [size, 8192].min)
    binary = sample.include?("\0") || %w[.png .jpg .wav .jar .so].include?(File.extname(path))
    source = binary ? nil : File.read(path, encoding: 'UTF-8')
    abort("Invalid UTF-8: #{path}") if source && !source.valid_encoding?
    kind = if binary then 'binary'
           elsif path.start_with?('app/src/test/') then 'unit'
           elsif path.start_with?('app/src/androidTest/') then 'instrumented-source-only'
           elsif path.start_with?('app/src/main/') then 'production'
           elsif path.start_with?('docs/') then 'docs-evidence'
           else 'config-tool-license' end
    detail = if source && path.end_with?('.kt', '.cpp')
               "declarations=#{source.scan(/^\s*(?:(?:data|sealed|enum|abstract|private|internal|override|suspend)\s+)*(?:class|object|interface|fun)\s+/).length}; tests=#{source.scan(/@Test\b/).length}"
             elsif source && path.end_with?('.json')
               begin
                 JSON.parse(source)
                 'JSON parse OK'
               rescue JSON::ParserError
                 'JSON parse FAILED'
               end
             elsif source && path.end_with?('.md')
               "headings=#{source.scan(/^#+ /).length}"
             else '-'
             end
    [path, kind, size, source ? source.lines.count : '-', Digest::SHA256.file(path).hexdigest, detail]
  end
end
puts '# 项目复盘全量文件与 Git 索引'
puts
puts "基线：`#{git('rev-parse', 'HEAD').strip}`；分支：`#{git('branch', '--show-current').strip}`。"
puts
puts '这是逐文件枚举、内容摘要与结构扫描，不是逐行语义审计或第三方二进制安全认证。文本完整读取用于结构统计与 JSON 解析；二进制只核对类型/大小/哈希。业务结论由完整档案中的具体源码和测试另行证明。未跟踪、忽略文件和供应商子模块内部不纳入主仓库表。'
puts
puts "Git 跟踪项：#{paths.length}；可达提交：#{git('rev-list', '--all', '--count').strip}。"
rows.group_by { |r| r[1] }.sort.each do |kind, items|
  puts "- #{kind}：#{items.length} 项，#{items.sum { |r| r[2].is_a?(Integer) ? r[2] : 0 }} 字节。"
end
puts
puts '## 每个跟踪项'
puts
puts '| 路径 | 类别 | 字节 | 文本行 | SHA-256 | 内容检查 |'
puts '| --- | --- | --- | --- | --- | --- |'
rows.each { |r| puts "| #{r.map { |v| cell(v) }.join(' | ')} |" }
puts
puts '## 全部本地可达提交（不读取聊天或私有环境）'
puts
puts '| 提交 | 日期 | 主题 | 相对首父提交的文件数 |'
puts '| --- | --- | --- | --- |'
git('log', '--all', '--format=%H%x09%ad%x09%s', '--date=short').lines.each do |line|
  sha, date, subject = line.strip.split("\t", 3)
  changed = git('diff-tree', '--root', '-m', '--no-commit-id', '--name-only', '-r', '-z', sha).split("\0").uniq.length
  puts "| #{sha} | #{date} | #{cell(subject)} | #{changed} |"
end
puts
puts '## 供应商边界'
puts
puts "whisper.cpp 固定提交：`#{git('-C', 'third_party/whisper.cpp', 'rev-parse', 'HEAD').strip}`；内部跟踪项：#{git('-C', 'third_party/whisper.cpp', 'ls-files', '-z').split("\0").length}。本轮核对 submodule 来源、集成 CMake/JNI 与许可，不声称逐行审计上游或重现 JNI 二进制构建。"
puts
puts '## 未跟踪范围'
puts
git('ls-files', '--others', '--exclude-standard', '-z').split("\0").each { |p| puts "- `#{cell(p)}`：不自动纳入已提交基准。" }
