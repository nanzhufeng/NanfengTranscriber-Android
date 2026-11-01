from pathlib import Path
import subprocess,re,time,json
out=Path('/tmp/nft-island-topmost-20261029'); adb=['adb','-s','emulator-5598']; events=[]
def run(*a): return subprocess.check_output(adb+list(a),text=True,stderr=subprocess.STDOUT)
def info(title='NanfengTranscriptionIsland'):
 s=run('shell','dumpsys','window','windows')
 m=re.search(r'Window #\d+ Window\{[^\n]*\b'+re.escape(title)+r'\b[^\n]*\}:(.*?)(?=\n  Window #|\Z)',s,re.S)
 if not m:return None
 b=m.group(1);f=re.search(r'Frames:.*?\bframe=\[(\d+),(\d+)\]\[(\d+),(\d+)\]',b); layer=re.search(r'mBaseLayer=(\d+)',b); typ=re.search(r'\bty=(\w+)',b)
 return {'frame':list(map(int,f.groups())),'layer':int(layer[1]),'type':typ[1]}
def wait(predicate):
 for _ in range(40):
  v=info()
  if v and predicate(v):return v
  time.sleep(.25)
 raise AssertionError(v)
def capture(name):
 time.sleep(.6)
 v=info();assert v;v['case']=name;v['statusbar']=info('StatusBar'); events.append(v)
 (out/(name+'.png')).write_bytes(subprocess.check_output(adb+['exec-out','screencap','-p']))
 (out/'geometry.json').write_text(json.dumps(events,indent=2)+'\n');print('PASS',name,v,flush=True);return v
run('shell','am','start','-a','android.intent.action.MAIN','-c','android.intent.category.HOME')
service='com.nanzhufeng.transcriber/.service.IslandTopmostService'
def enable(on):
 if on: run('shell','settings','put','secure','enabled_accessibility_services',service)
 else: run('shell','settings','delete','secure','enabled_accessibility_services')
 run('shell','settings','put','secure','accessibility_enabled','1' if on else '0')
def header_tap():
 f=info()['frame'];run('shell','input','tap',str(f[0]+100),'60')
 time.sleep(.6)
wait(lambda v:v['type']=='APPLICATION_OVERLAY');v=capture('ordinary-safe');assert v['frame'][1]>=v['statusbar']['frame'][3]
enable(True)
wait(lambda v:v['type']=='ACCESSIBILITY_OVERLAY');v=capture('topmost-dark');assert v['layer']>v['statusbar']['layer'];assert 0<=v['frame'][1]<=6
# A real white system app changes status-bar icon colors; no App-under-test automation.
run('shell','am','start','-a','android.settings.SETTINGS');time.sleep(.8);capture('topmost-white')
header_tap();wait(lambda v:v['frame'][3]-v['frame'][1]>250);capture('topmost-expanded')
run('shell','am','start','-a','android.intent.action.MAIN','-c','android.intent.category.HOME');time.sleep(.5)
enable(False);v=wait(lambda v:v['type']=='APPLICATION_OVERLAY');assert v['frame'][1]>=info('StatusBar')['frame'][3];assert v['frame'][3]-v['frame'][1]>250;capture('revoked-expanded-safe')
enable(True);wait(lambda v:v['type']=='ACCESSIBILITY_OVERLAY');v=capture('reconnected-expanded');assert v['frame'][3]-v['frame'][1]>250
run('shell','wm','size','2248x2480');time.sleep(1.2);v=capture('inner-expanded');assert v['frame'][1]>=v['statusbar']['frame'][3]
run('shell','wm','size','1140x2616');time.sleep(1.2);v=capture('outer-after-fold');assert v['frame'][1]<=6;assert v['frame'][3]-v['frame'][1]>250
header_tap();wait(lambda v:v['frame'][3]-v['frame'][1]<200)
run('shell','settings','put','system','font_scale','1.5');run('shell','am','start-foreground-service','-n','com.nanzhufeng.transcriber/.qa.IslandLayoutQaService','--ez','unknown','true');time.sleep(1.1);capture('unknown-large')
s=run('shell','dumpsys','accessibility');(out/'accessibility.txt').write_text(s)
run('shell','appops','set','com.nanzhufeng.transcriber','SYSTEM_ALERT_WINDOW','deny')
wait(lambda v:v['type']=='ACCESSIBILITY_OVERLAY');time.sleep(1);assert info()['type']=='ACCESSIBILITY_OVERLAY'
print('PASS topmost works independently of ordinary overlay permission',flush=True)
header_tap();wait(lambda v:v['frame'][3]-v['frame'][1]>250)
f=info()['frame'];run('shell','input','tap',str(f[0]+(f[2]-f[0])*3//4),str(f[3]-86));time.sleep(1);assert info() is None
print('PASS QA cancel closes overlay, no real transcription or network',flush=True)
enable(False)
