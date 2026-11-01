require 'rexml/document'
require 'open3'
ADB='adb'
OUT='/tmp/nft-ui-preview-final-20261020'
def adb(*a)
 o,s=Open3.capture2e(ADB,'-s','emulator-5596',*a);raise o unless s.success?;sleep 0.5 if a[0]=='shell' && a[1]=='input';o
end
def nodes
 adb('shell','uiautomator','dump','/sdcard/nft-surfaces.xml')
 adb('pull','/sdcard/nft-surfaces.xml',"#{OUT}/current.xml")
 REXML::XPath.match(REXML::Document.new(File.read("#{OUT}/current.xml")),'//node')
end
def click_label(t)
 n=nodes.select{|v|v.attributes['text']==t || v.attributes['content-desc']==t}.last;raise "missing #{t}" unless n
 a,b,c,d=n.attributes['bounds'].scan(/\d+/).map(&:to_i);adb('shell','input','tap',((a+c)/2).to_s,((b+d)/2).to_s)
end
def shot(n)
 adb('shell','uiautomator','dump','/sdcard/nft-surfaces.xml');adb('pull','/sdcard/nft-surfaces.xml',"#{OUT}/#{n}.xml")
 o,s=Open3.capture2(ADB,'-s','emulator-5596','exec-out','screencap','-p');raise 'screenshot' unless s.success?;File.binwrite("#{OUT}/#{n}.png",o)
end
def scroll_up
 adb('shell','input','swipe','550','700','550','2100','260')
end
def scroll_down
 adb('shell','input','swipe','550','2100','550','800','260')
end
# App is opened on the root by the invoking command.
initial=nodes
click_label('青林木语') if initial.any?{|v|v.attributes['text']=='选择皮肤'}
click_label('返回设置') if nodes.any?{|v|v.attributes['content-desc']=='返回设置'}
click_label('设置');scroll_up
skins=%w[玄岩浮影 青林木语 枫绯凝霜 绛绡叠韵 铜棕弦影 青玉流岚]
skins.each_with_index do |skin,i|
 click_label('选择皮肤');shot("picker-#{i}");click_label(skin)
 ns=nodes;raise 'picker did not close' if ns.any?{|v|v.attributes['text']=='选择皮肤'}
 raise "current skin missing #{skin}" unless ns.any?{|v|v.attributes['text']==skin}
 raise 'skin list leaked into root' unless skins.count{|v| ns.any?{|n|n.attributes['text']==v}}==1
 shot("skin-#{i}");puts "PASS picker and immediate selection #{skin}"
end
click_label('选择皮肤');click_label('青林木语');shot('root-outer')
click_label('选择皮肤');adb('shell','input','keyevent','4');raise 'back did not cancel popup' if nodes.any?{|v|v.attributes['text']=='选择皮肤'}
scroll_down
ns=nodes; model=ns.find{|v|v.attributes['text']=='转写模型'};ledger=ns.find{|v|v.attributes['text']=='调用记录'};params=ns.find{|v|v.attributes['text']=='转写参数'}
raise 'model/ledger order' unless model && ledger && params && model.attributes['bounds'].scan(/\d+/)[1].to_i < ledger.attributes['bounds'].scan(/\d+/)[1].to_i && ledger.attributes['bounds'].scan(/\d+/)[1].to_i < params.attributes['bounds'].scan(/\d+/)[1].to_i
shot('root-lower');puts 'PASS model, ledger, parameters order'
%w[转写模型 调用记录 转写参数 输出与文件 关于].each_with_index do |name,i|
 scroll_down;click_label(name);shot("page-#{i}")
 if name=='转写模型'
  opts=nodes.select{|v|v.attributes['text'].to_s.include?('Paraformer')};raise 'no cloud model' if opts.empty?
  click_label(opts.first.attributes['text']);shot('model-cloud')
  local=nodes.find{|v|v.attributes['text'].to_s.== '本地标准'};raise 'no local model' unless local;click_label(local.attributes['text'])
 end
 if name=='转写参数'
  click_label('识别语言');shot('language-menu');click_label('中文');click_label('识别语言');click_label('自动识别')
 end
 if i.even? then click_label('返回设置') else adb('shell','input','keyevent','4') end
 puts "PASS child and back #{name}"
end
scroll_up;click_label('灵动岛');shot('island');click_label('返回设置')
adb('shell','wm','size','2248x2480');shot('root-inner')
adb('shell','wm','size','1140x2616');adb('shell','settings','put','system','font_scale','1.5');scroll_up;shot('root-large');click_label('选择皮肤');shot('picker-large');adb('shell','input','keyevent','4');scroll_down;scroll_down;click_label('转写参数');shot('parameters-large');click_label('返回设置')
adb('shell','settings','put','system','font_scale','1.0');click_label('历史');shot('history');click_label('全部时间');shot('history-menu');adb('shell','input','keyevent','4');click_label('首页');shot('home');puts 'PASS inner/outer and large text; home/history'
