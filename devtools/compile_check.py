#!/usr/bin/env python3
"""Quick javac check that catches real errors before a Gradle/Actions build.
Usage: python3 devtools/compile_check.py [file.java ...]   (default: the files changed in steps 38-41)
Needs a JDK (or a JRE with the jdk.compiler module) and internet once, to fetch android.jar (API 33).
Prints only errors that are NOT caused by what this check cannot see (generated R, androidx/guava/errorprone
annotations, and TalkBackService whose parent class comes from an external Google library)."""
import glob, os, re, shutil, subprocess, sys, tempfile, urllib.request
ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
JAR = os.path.join(tempfile.gettempdir(), "android-33.jar")
if not os.path.exists(JAR) or os.path.getsize(JAR) < 5_000_000:
    print("downloading android.jar ...")
    urllib.request.urlretrieve("https://raw.githubusercontent.com/Sable/android-platforms/master/android-33/android.jar", JAR)
E = "talkback/src/main/java/com/google/android/accessibility/talkback/editor"
default = glob.glob(os.path.join(ROOT, E, "*.java")) + glob.glob(os.path.join(ROOT, "talkback/src/main/java/com/androlua/**/*.java"), recursive=True) \
    + glob.glob(os.path.join(ROOT, "utils/src/main/java/com/google/android/accessibility/utils/gestures/*.java"))
files = [os.path.abspath(f) for f in sys.argv[1:]] or default
sp = ":".join(glob.glob(os.path.join(ROOT, "**/src/*/java"), recursive=True))
jc = ["javac"] if shutil.which("javac") else ["java", "-m", "jdk.compiler/com.sun.tools.javac.Main"]
out = tempfile.mkdtemp()
r = subprocess.run(jc + ["-XDshould-stop.ifError=FLOW", "-proc:none", "-nowarn", "-Xmaxerrs", "100000", "-cp", JAR, "-d", out, "-sourcepath", sp] + files,
                   capture_output=True, text=True)
lines = (r.stdout + r.stderr).splitlines()
mine = tuple(os.path.join(ROOT, d) for d in (E, "talkback/src/main/java/com/androlua", "utils/src/main/java/com/google/android/accessibility/utils/gestures"))
NOISE_SRC = re.compile(r"^\s*(import |@RequiresApi|@IntDef|@Nullable|@NonNull|@StringRes|@CanIgnore|.*\bR\.[a-z]+\.|.*ImmutableList)")
NOISE_MSG = re.compile(r"package R does not exist|TalkBackService cannot be converted|AccessibilityServiceCompat|package (androidx|com\.google\.(common|errorprone)|org\.checkerframework)[A-Za-z.]* does not exist|\((TalkBack|Jx)?[A-Za-z]*Service[,)]|\(TalkBackService[,)]")
cascade = re.compile(r"\.(performGlobalAction|getWindows|getServiceInfo|setServiceInfo|getRootInActiveWindow|startActivity|getSystemService|getResources|getSoftKeyboardController|getApplicationContext|getContentResolver|getPackageName|getMainLooper)\(")
real = []
for i, l in enumerate(lines):
    m = re.match(r"(.+\.java):(\d+): error: (.*)", l)
    if not m: continue
    f, n, msg = os.path.abspath(m.group(1)), int(m.group(2)), m.group(3)
    if not f.startswith(mine) or NOISE_MSG.search(msg): continue
    src = open(f, encoding="utf-8", errors="replace").read().splitlines()[n - 1]
    if NOISE_SRC.match(src) or cascade.search(src): continue
    ctx = " ".join(lines[i + 1:i + 4])
    if re.search(r"symbol:\s+(class|variable) (R|ImmutableList|Nullable|NonNull|RequiresApi|StringRes|IntDef|CanIgnoreReturnValue|[A-Z][A-Za-z]*Compat[A-Za-z]*)\b", ctx): continue
    real.append(f"{os.path.relpath(f, ROOT)}:{n}: {msg}\n    {src.strip()}")
print("\n".join(real) if real else "no real errors found (external-library noise ignored)")
sys.exit(1 if real else 0)
