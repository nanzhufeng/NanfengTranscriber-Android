from pathlib import Path
import subprocess,re,time,json,xml.etree.ElementTree as E
out=Path('/tmp/nft-island-20261025');adb=['adb','-s','emulator-5598'];events=[]
def run(*a):return subprocess.check_output(adb+list(a),text=True,stderr=subprocess.STDOUT)
def window(title='NanfengTranscriptionIsland'):
 s=run('shell','dumpsys','window','windows')
 m=re.search(r'Window #\d+ Window\{[^\n]*\b'+re.escape(title)+r'\b[^\n]*\}:(.*?)(?=\n  Window #|\Z)',s,re.S)
 if not m:return None
 f=re.search(r'Frames:.*?\bframe=\[(\d+),(\d+)\]\[(\d+),(\d+)\]',m.group(1));return tuple(map(int,f.groups())) if f else None
def wait_width(width):
 for _ in range(15):
  f=window()
  if f and f[2]-f[0]==width:return f
  time.sleep(.3)
 raise AssertionError(f'window width {width}: {f}')
def capture(name):
 f=window();assert f;events.append({'case':name,'frame':f})
 (out/f'{name}.png').write_bytes(subprocess.check_output(adb+['exec-out','screencap','-p']))
 (out/'geometry.json').write_text(json.dumps(events,indent=2)+'\n');print('PASS',name,f,flush=True);return f
def tap_lip():
 f=window();bar=window('StatusBar');assert f and bar,(f,bar)
 run('shell','input','tap',str(f[0]+110),str(bar[3]+10));time.sleep(.5)
def nodes():
 run('shell','uiautomator','dump','/sdcard/nft-island.xml');run('pull','/sdcard/nft-island.xml',str(out/'current.xml'))
 return list(E.parse(out/'current.xml').getroot().iter('node'))
def start(unknown=False):
 args=['shell','am','start-foreground-service','-n','com.nanzhufeng.transcriber/.qa.IslandLayoutQaService']
 if unknown:args+=['--ez','unknown','true']
 run(*args);time.sleep(.8)
start();f=wait_width(607);assert 0<=f[1]<=6;capture('outer-compact')
tap_lip();f=wait_width(698);assert 0<=f[1]<=6
assert f[3]-f[1]>200;capture('outer-expanded')
tap_lip();wait_width(607)
run('shell','settings','put','system','font_scale','1.5');start(True);capture('outer-unknown-large')
run('shell','settings','put','system','font_scale','1.0');start()
# Same state, same task, only the screen geometry changes.
run('shell','wm','size','2248x2480');time.sleep(1.1);f=capture('inner-compact');bar=window('StatusBar');assert f[1]>=bar[3]
run('shell','input','tap',str((f[0]+f[2])//2),str(f[1]+50));time.sleep(.5);f=wait_width(884);assert f[1]>=window('StatusBar')[3];capture('inner-expanded')
run('shell','wm','size','1140x2616');time.sleep(1.1);f=wait_width(698);assert 0<=f[1]<=6
assert f[3]-f[1]>200;capture('outer-expanded-after-fold')
# Cancel callback belongs to the isolated driver, not a real transcription queue.
f=window();run('shell','input','tap',str(f[0]+(f[2]-f[0])*3//4),str(f[3]-86));time.sleep(.7);assert window() is None
print('PASS cancel callback closes QA overlay; no real task or paid service',flush=True)
