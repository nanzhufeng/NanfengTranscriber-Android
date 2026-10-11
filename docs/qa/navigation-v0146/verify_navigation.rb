# Isolated emulator UI acceptance, never a physical-device or instrumentation test.
require 'open3'
require 'rexml/document'
require 'fileutils'

SERIAL = 'emulator-5592'
OUTPUT = File.expand_path(__dir__)
def adb(*args)
  output, error, status = Open3.capture3('adb', '-s', SERIAL, *args)
  raise error unless status.success?
  output
end
def snapshot
  adb('shell', 'uiautomator', 'dump', '/sdcard/nanfeng-navigation-qa.xml')
  adb('shell', 'cat', '/sdcard/nanfeng-navigation-qa.xml')
end
def rect(node)
  node.attributes['bounds'].scan(/\d+/).map(&:to_i)
end
def nav_node(xml, label)
  nodes = REXML::Document.new(xml).get_elements('//node').select { |node| node.attributes['text'] == label }
  node = nodes.max_by { |candidate| rect(candidate)[3] }
  raise "missing navigation #{label}" unless node
  until node.attributes['clickable'] == 'true' || node.attributes['selected'] == 'true'
    node = node.parent
    raise "missing tab semantics #{label}" unless node.respond_to?(:attributes)
  end
  node
end
FileUtils.mkdir_p(OUTPUT)
original_size = adb('shell', 'wm', 'size')[/Override size: (\S+)/, 1]
original_density = adb('shell', 'wm', 'density')[/Override density: (\d+)/, 1]
rows = []
begin
  [['outer', 1140, 2616, 52], ['inner', 2248, 2480, 56]].each do |form, width, height, item_dp|
    adb('shell', 'wm', 'size', "#{width}x#{height}")
    adb('shell', 'wm', 'density', '442')
    xml = snapshot
    if form == 'inner'
      raise 'fold lost selected Settings route' unless nav_node(xml, '设置').attributes['selected'] == 'true'
    end
    [['home', '首页'], ['history', '历史'], ['settings', '设置']].each do |page, label|
      bounds = rect(nav_node(xml, label))
      adb('shell', 'input', 'tap', ((bounds[0] + bounds[2]) / 2).to_s, ((bounds[1] + bounds[3]) / 2).to_s)
      xml = snapshot
      chosen = nav_node(xml, label)
      raise "#{form}/#{page} not selected" unless chosen.attributes['selected'] == 'true'
      actual = rect(chosen)
      expected_height = item_dp * 442.0 / 160
      raise "#{form}/#{page} wrong height #{actual}" if ((actual[3] - actual[1]) - expected_height).abs > 2
      raise 'navigation intrudes into screen edge' unless actual[3] < height
      geometry = ['首页', '历史', '设置'].map { |name| rect(nav_node(xml, name)) }
      raise 'navigation is not a shared bottom row' unless geometry.map { |r| r[1] }.uniq.size == 1
      raise 'navigation width exceeds canonical 720dp' if geometry.last[2] - geometry.first[0] > 720 * 442.0 / 160 + 2
      File.write(File.join(OUTPUT, "#{form}-#{page}.xml"), xml)
      File.binwrite(File.join(OUTPUT, "#{form}-#{page}.png"), adb('exec-out', 'screencap', '-p'))
      row = "#{form}/#{page} viewport=#{width}x#{height}@442 selected=true navigation=#{geometry.inspect}"
      rows << row
      puts row
    end
  end
  File.write(File.join(OUTPUT, 'acceptance.txt'), rows.join("\n") + "\nfold Settings selection preserved\n")
ensure
  adb('shell', 'wm', 'size', original_size || 'reset')
  adb('shell', 'wm', 'density', original_density || 'reset')
end
