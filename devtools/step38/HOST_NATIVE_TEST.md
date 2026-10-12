# Host test of the native modules (Linux, not Android)
Used in Step 38 to prove bson/cjson/crypt/regex link against libluajava.so and run. Needs gcc, a JRE, jni.h/jni_md.h (OpenJDK), android.jar.
1. Build liblua.a from talkback/src/main/jni/lua/*.c (-fPIC -include float.h -DLUA_USE_POSIX -DLUA_USE_DLOPEN).
2. Build libluajava.so from luajava/luajava.c with -Wl,--whole-archive liblua.a (stub <android/log.h>).
3. Build each module as lib<name>.so with -L. -lluajava.
4. Compile HostNativeTest.java (class T) against the compiled com.luajava classes + android.jar; run with -Djava.library.path=<dir>.
Result in Step 38: PASS cjson, bson, crypt, regex. This does NOT replace an NDK/Gradle build.
