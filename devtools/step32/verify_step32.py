from pathlib import Path
root = Path(__file__).resolve().parents[2]
checks = {
    'LuaAsyncTask constructors and callbacks': ('talkback/src/main/java/com/androlua/LuaAsyncTask.java', ['LuaAsyncTask(LuaContext context, String source, LuaObject callback)', 'doInBackground(Object... args)', 'onPostExecute(Object result)']),
    'LuaThread implements LuaMetaTable': ('talkback/src/main/java/com/androlua/LuaThread.java', ['implements Runnable, LuaMetaTable, LuaGcable', 'public void quit()', 'public void call(String name, Object[] values)']),
    'LuaTimer APIs': ('talkback/src/main/java/com/androlua/LuaTimer.java', ['start(long delay)', 'start(long delay,long period)', 'setEnabled(boolean value)', 'getPeriod()']),
    'HTTP common facade': ('talkback/src/main/java/com/androlua/Http.java', ['get(String url, LuaObject cb)', 'post(String url,String data,LuaObject cb)', 'download(String url,String path,LuaObject cb)', 'class HttpTask']),
    'Download API': ('talkback/src/main/java/com/androlua/Download.java', ['onDownloadComplete(String url, String path)', 'start(boolean showNotification)', 'setDestinationInExternalFilesDir']),
    'Lua service call now invokes Lua': ('talkback/src/main/java/com/google/android/accessibility/talkback/editor/LuaServiceApi.java', ['@Override public void call(String func, Object... args)', 'L.getGlobal(func)', 'L.pcall(args == null ? 0 : args.length, 0, 0)']),
    'Jieshuo runtime Smali references retained': ('reference/jieshuo_smali/com/androlua/LuaAsyncTask.smali', ['.class public Lcom/androlua/LuaAsyncTask;']),
}
failed=[]
for label,(rel,needles) in checks.items():
    p=root/rel
    data=p.read_text(errors='replace') if p.exists() else ''
    ok=bool(data) and all(n in data for n in needles)
    print(('PASS' if ok else 'FAIL')+': '+label)
    if not ok: failed.append(label)
print('LIMITATION: source-level checks only; no Android Gradle build or device/runtime test.')
raise SystemExit(1 if failed else 0)
