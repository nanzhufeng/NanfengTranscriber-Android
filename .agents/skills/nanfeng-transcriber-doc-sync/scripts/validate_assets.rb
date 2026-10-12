#!/usr/bin/env ruby
# Read-only validation for the project-local review assets; no application or device access.
require 'yaml'
require 'pathname'

root = File.expand_path('../../../..', __dir__)
Dir.chdir(root)
failures = []
skills = Dir['.agents/skills/nanfeng-transcriber-*/SKILL.md'].sort
skills.each do |path|
  source = File.read(path)
  match = source.match(/\A---\n(.*?)\n---\n/m)
  begin
    front = match && YAML.safe_load(match[1], permitted_classes: [], aliases: false)
    name = File.basename(File.dirname(path))
    unless front.is_a?(Hash) && front['name'] == name && name.match?(/\A[a-z0-9]+(?:-[a-z0-9]+)*\z/) && name.length <= 64
      failures << "Invalid skill name/frontmatter: #{path}"
    end
    description = front.is_a?(Hash) ? front['description'] : nil
    unless description.is_a?(String) && !description.strip.empty? && description.length <= 1024 && !description.match?(/[<>]/)
      failures << "Invalid description: #{path}"
    end
    failures << "Oversized skill: #{path}" if source.lines.count > 500
    ui = YAML.safe_load(File.read(File.join(File.dirname(path), 'agents/openai.yaml')), permitted_classes: [], aliases: false)
    interface = ui.fetch('interface')
    unless %w[display_name short_description default_prompt].all? { |k| interface[k].is_a?(String) && !interface[k].empty? } && interface['default_prompt'].include?("$#{name}")
      failures << "Invalid UI metadata: #{path}"
    end
  rescue StandardError => error
    failures << "Skill parse failed: #{path}: #{error.class}"
  end
end
paths = (skills + %w[AGENTS.md README.md docs/qa/project-review-20261011/README.md] + Dir['docs/*.md'] + Dir['docs/qa/contracts-20261012/*.md']).uniq
links = 0
paths.each do |path|
  unless File.file?(path)
    failures << "Missing document: #{path}"
    next
  end
  source = File.read(path)
  source.scan(/!?\[[^\]\n]*\]\(([^)\n]+)\)/).flatten.each do |url|
    next if url.match?(/\A(?:https?:|mailto:|#)/)
    target = url.sub(/#.*/, '').sub(/\A</, '').sub(/>\z/, '')
    links += 1
    failures << "Missing local link in #{path}: #{target}" unless File.exist?(File.expand_path(target, File.dirname(path)))
  end
  failures << "Unfinished scaffold: #{path}" if source.match?(/\b(?:TODO|FIXME|TBD)\b|<App 名称>/)
  failures << "Possible secret: #{path}" if source.match?(/sk-(?:proj-)?[A-Za-z0-9_-]{20,}|-----BEGIN (?:RSA |EC )?PRIVATE KEY-----/)
end
puts "skills=#{skills.length} local_links=#{links} failures=#{failures.length}"
failures.each { |failure| warn failure }
exit(failures.empty? ? 0 : 1)
