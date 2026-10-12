#!/usr/bin/env python3
"""Static regression checks for Step 35 Lua asynchronous-object lifecycle.
These checks inspect source markers only; they are not Android/runtime tests.
"""
from pathlib import Path
root = Path(__file__).resolve().parents[2]
api = (root / 'talkback/src/main/java/com/google/android/accessibility/talkback/editor/LuaServiceApi.java').read_text()
ctx = (root / 'talkback/src/main/java/com/androlua/LuaContext.java').read_text()
task = (root / 'talkback/src/main/java/com/androlua/LuaAsyncTask.java').read_text()
thread = (root / 'talkback/src/main/java/com/androlua/LuaThread.java').read_text()
timer = (root / 'talkback/src/main/java/com/androlua/LuaTimer.java').read_text()
timertask = (root / 'talkback/src/main/java/com/androlua/LuaTimerTask.java').read_text()
checks = {
    'LuaContext has unregister lifecycle method': 'void unregGc(LuaGcable obj)' in ctx,
    'LuaServiceApi tracks registered Lua objects': 'gcables.add(obj)' in api and 'gcables.remove(obj)' in api,
    'Lua state closes only when pending tasks and objects are idle': 'mainDone && pending.get() == 0 && gcables.isEmpty()' in api,
    'Async task unregisters on completion and cancellation': 'context.unregGc(this)' in task and 'onCancelled(Object result)' in task,
    'Lua thread unregisters after its run loop exits': 'finally {' in thread and 'context.unregGc(this)' in thread,
    'One-shot timer task releases its registration': 'setOneShot(boolean value)' in timertask and 'if(oneShot)' in timertask,
    'LuaTimer configures one-shot versus repeating task': 'setOneShot(this.period<=0)' in timer,
}
for label, ok in checks.items():
    print(('PASS' if ok else 'FAIL') + ': ' + label)
if not all(checks.values()):
    raise SystemExit(1)
print(f'{len(checks)}/{len(checks)} static checks passed')
print('LIMITATION: source-marker checks only; Android compile, Lua runtime, and device behavior remain unverified.')
