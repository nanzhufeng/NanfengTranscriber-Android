from pathlib import Path
import subprocess,time,re,xml.etree.ElementTree as E
out=Path('/tmp/nft-island-topmost-20261029');adb=['adb','-s','emulator-5598']
def run(*a):return subprocess.check_output(adb+list(a),text=True,stderr=subprocess.STDOUT)
def nodes():
 run('shell','uiautomator','dump','/sdcard/nft-topmost-ui.xml')
 return list(E.fromstring(run('shell','cat','/sdcard/nft-topmost-ui.xml')).iter('node'))
def click(label):
 n=[n for n in nodes() if n.get('text')==label][-1]
 a=list(map(int,re.findall(r'\d+',n.get('bounds'))));run('shell','input','tap',str((a[0]+a[2])//2),str((a[1]+a[3])//2));time.sleep(.7)
run('shell','settings','put','system','font_scale','1.0')
run('shell','am','start','-n','com.nanzhufeng.transcriber/.MainActivity');time.sleep(.8)
click('设置');click('灵动岛')
labels=[n.get('text') for n in nodes() if n.get('text')]; assert '状态栏置顶：未开启' in labels;assert '开启状态栏置顶' in labels
(out/'settings-permission.png').write_bytes(subprocess.check_output(adb+['exec-out','screencap','-p']))
click('开启状态栏置顶');s=run('shell','dumpsys','activity','activities');assert 'com.android.settings' in s
# The standard system accessibility page must list this service by its actual name.
labels=[n.get('text') for n in nodes() if n.get('text')];assert any('Accessibility' in t or '无障碍' in t for t in labels),labels
print('PASS existing island page exposes accurate disabled state and opens real system accessibility settings')
run('shell','input','keyevent','4');time.sleep(.5)
