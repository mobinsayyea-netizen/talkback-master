#!/usr/bin/env python3
"""Step 45 check on a computer: runs the real settings mapper and gesture planner on a real Jieshuo backup zip.
Usage: python3 devtools/step45/verify_step45.py path/to/Jieshuo_backup.zip [installed,apps,comma,separated]
Needs a JDK (or a JRE with jdk.compiler) and internet once for android.jar (same as devtools/compile_check.py)."""
import glob, json, os, re, shutil, subprocess, sys, tempfile, zipfile, urllib.request
ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
B = os.path.join(ROOT, "talkback/src/main/java/com/google/android/accessibility/talkback")
zip_path = sys.argv[1]
apps = sys.argv[2] if len(sys.argv) > 2 else "WhatsApp,PhonePe,Gemini"
jar = os.path.join(tempfile.gettempdir(), "android-33.jar")
if not os.path.exists(jar) or os.path.getsize(jar) < 5_000_000:
    urllib.request.urlretrieve("https://raw.githubusercontent.com/Sable/android-platforms/master/android-33/android.jar", jar)
work = tempfile.mkdtemp()
z = zipfile.ZipFile(zip_path)
rows, plug, tools = [], os.path.join(work, "plug"), os.path.join(work, "tools")
for n in z.namelist():
    p = n.replace("\\", "/").split("/")
    if p[-1].endswith("设置.dat") or p[-1] == "\u8bbe\u7f6e.dat":
        open(os.path.join(work, "settings.dat"), "wb").write(z.read(n))
    for i in range(min(2, len(p))):
        if p[i] == "Gestures" and len(p) > i + 2 and p[-1] == "config":
            for k, v in json.loads(z.read(n).decode("utf8")).items():
                rows.append(f"{p[i+1].strip()}\t{k}\t{v}")
        for top, dest in (("Plugins", plug), ("Tools", tools)):
            if p[i] == top and len(p) > i + 2 and p[-1] == "main.lua":
                os.makedirs(os.path.join(dest, p[i + 1]), exist_ok=True)
                open(os.path.join(dest, p[i + 1], "main.lua"), "wb").write(z.read(n))
open(os.path.join(work, "gestures.tsv"), "w", encoding="utf8").write("\n".join(rows))
out = os.path.join(work, "out")
sp = ":".join(glob.glob(os.path.join(ROOT, "**/src/*/java"), recursive=True))
jc = ["javac"] if shutil.which("javac") else ["java", "-m", "jdk.compiler/com.sun.tools.javac.Main"]
src = [f"{B}/backup/JxSettingsMapper.java", f"{B}/backup/MsPrefCatalog.java", f"{B}/editor/JxGestureImport.java",
       f"{B}/editor/JxFunctionAliases.java", os.path.join(ROOT, "devtools/step45/check/backup/Step45SettingsCheck.java"),
       os.path.join(ROOT, "devtools/step45/check/editor/Step45GestureCheck.java")]
r = subprocess.run(jc + ["-proc:none", "-nowarn", "-cp", jar, "-d", out, "-sourcepath", sp] + src, capture_output=True, text=True)
if r.returncode:
    print(r.stdout + r.stderr); sys.exit(1)
props = ["-Dfile.encoding=UTF-8", f"-Djx.commands.src={B}/editor/JxCommands.java", "-cp", out]
s = subprocess.run(["java"] + props + ["com.google.android.accessibility.talkback.backup.Step45SettingsCheck",
                    os.path.join(work, "settings.dat"), "com.vnspeak.autotts,es.codefactory.eloquencetts"], capture_output=True, text=True)
g = subprocess.run(["java"] + props + ["com.google.android.accessibility.talkback.editor.Step45GestureCheck",
                    os.path.join(work, "settings.dat"), os.path.join(work, "gestures.tsv"), plug, tools, apps], capture_output=True, text=True)
print(s.stdout[:3000] if len(sys.argv) > 3 else "\n".join(s.stdout.splitlines()[:2]))
print("\n".join(g.stdout.splitlines()[:1]))
ok = True
def check(c, msg):
    global ok
    print(("PASS " if c else "FAIL ") + msg); ok = ok and c
m = re.search(r"applied: (\d+)\s+rejected: (\d+)\s+noMatch: (\d+)\s+private: (\d+)", s.stdout)
check(s.returncode == 0 and "SAFETY OK" in s.stdout, "no key, token or account value is ever mapped")
check(bool(m) and int(m.group(1)) > 100, "more than 100 settings map to MS")
check("git_token" not in s.stdout.split("--- applied")[1].split("--- rejected")[0], "git_token is not in the applied list")
check("ms_op_down_volume_key -> " not in s.stdout and "down_volume_key -> ms_op_down_volume_key = assistant" in s.stdout, "volume key action converted")
check(g.returncode == 0 and "global  15  cmd  command_to_back" in g.stdout, "swipe down-then-left -> Go back")
check("multi(5,0)  mp  on" in g.stdout, "multi-part switch for left-then-right turned on")
sys.exit(0 if ok else 1)
