"""Drive the installed development app through a small real-input smoke test."""
from pathlib import Path
import subprocess, time, re, xml.etree.ElementTree as ET
ADB=str(Path.home()/'AppData/Local/Android/Sdk/platform-tools/adb.exe')
OUT=Path(__file__).resolve().parent
def adb(*args): return subprocess.check_output([ADB,*args])
def nodes():
    adb('shell','uiautomator','dump','/sdcard/workout-ui.xml')
    return ET.fromstring(adb('shell','cat','/sdcard/workout-ui.xml')).iter('node')
def tap(key):
    for n in nodes():
        if n.get('resource-id')=='com.cs533.workoutlog:id/'+key or n.get('text','').casefold()==key.casefold():
            x1,y1,x2,y2=map(int,re.findall(r'\d+',n.get('bounds')))
            adb('shell','input','tap',str((x1+x2)//2),str((y1+y2)//2));time.sleep(.2);return
    raise RuntimeError('Visible control not found: '+key)
def shot(name):
    time.sleep(.4);(OUT/(name+'.png')).write_bytes(adb('exec-out','screencap','-p'))
adb('shell','am','force-stop','com.cs533.workoutlog')
adb('shell','am','start','-n','com.cs533.workoutlog/.MainActivity')
time.sleep(3.5);shot('history-empty')
tap('add_workout');tap('exercise_spinner');tap('Squat');shot('workout-form')
tap('sets');adb('shell','input','text','3');adb('shell','input','keyevent','4')
tap('reps');adb('shell','input','text','12');adb('shell','input','keyevent','4')
adb('shell','input','swipe','550','1500','550','500','350')
tap('weight');adb('shell','input','text','45');adb('shell','input','keyevent','4')
tap('notes');adb('shell','input','text','Walkthrough%stest');adb('shell','input','keyevent','4')
tap('save_workout');time.sleep(3.5);shot('history-with-workout')
tap('Squat');shot('workout-details')
adb('shell','input','swipe','550','1500','550','500','350');shot('workout-details-actions')
tap('delete');tap('Delete');time.sleep(3.5)
assert any(n.get('text')=='0 workouts recorded' for n in nodes()), 'Smoke test entry was not removed'
shot('history-empty')
print('Real-input walkthrough passed: add, save, history, details, confirmed delete. Temporary workout removed.')
