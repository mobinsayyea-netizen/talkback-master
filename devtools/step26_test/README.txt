Step 26 test (multi-part state machine, no Android needed). From the repo root:
  mkdir -p /tmp/mp && java -m jdk.compiler/com.sun.tools.javac.Main -d /tmp/mp \
    talkback/src/main/java/com/google/android/accessibility/talkback/editor/MultiPartCore.java \
    devtools/step26_test/MultiPartTests.java
  java -cp /tmp/mp com.google.android.accessibility.talkback.editor.MultiPartTests
