package com.google.android.accessibility.talkback.editor;

import java.util.ArrayList;
import java.util.List;

/** Run: see README.txt in this folder. Tests the multi-part state machine without Android. */
public class MultiPartTests {
  static int fail = 0;

  static void check(boolean ok, String what) {
    if (!ok) {
      fail++;
      System.out.println("FAIL: " + what);
    }
  }

  /** A fake host that records what happened. */
  static class FakeHost implements MultiPartCore.Host {
    boolean enabled = true;
    List<Integer> firstOn = new ArrayList<>();
    List<String> seconds = new ArrayList<>(); // "first:second"
    List<String> log = new ArrayList<>();
    boolean timeoutScheduled = false;
    long duration = 500;

    public boolean enabled() { return enabled; }
    public boolean isFirstPart(int g) { return firstOn.contains(g); }
    public boolean hasSecond(int f, int s) { return seconds.contains(f + ":" + s); }
    public void runSecond(int f, int s) { log.add("second " + f + ":" + s); }
    public void runNormal(int g) { log.add("normal " + g); }
    public void soundFirstPart() { log.add("sound1"); }
    public void soundSecondPart() { log.add("sound2"); }
    public void scheduleTimeout(long ms) { timeoutScheduled = true; log.add("wait " + ms); }
    public void cancelTimeout() { timeoutScheduled = false; }
    public long durationMs() { return duration; }
  }

  public static void main(String[] a) {
    // 1. Part 1 + part 2 in time: part 2 action runs, part 1 normal does not.
    MultiPartCore c = new MultiPartCore();
    FakeHost h = new FakeHost();
    h.firstOn.add(13);
    h.seconds.add("13:1");
    check(c.onGesture(h, 13), "part 1 taken");
    check(c.isWaiting(), "waiting after part 1");
    check(h.log.toString().equals("[sound1, wait 500]"), "part 1 log " + h.log);
    check(c.onGesture(h, 1), "part 2 taken");
    check(!c.isWaiting(), "not waiting after part 2");
    check(h.log.toString().equals("[sound1, wait 500, sound2, second 13:1]"), "log " + h.log);
    check(!h.timeoutScheduled, "timeout cancelled");

    // 2. Timeout: part 1's normal action runs once.
    c = new MultiPartCore();
    h = new FakeHost();
    h.firstOn.add(14);
    c.onGesture(h, 14);
    c.onTimeout(h);
    check(h.log.toString().equals("[sound1, wait 500, normal 14]"), "timeout log " + h.log);
    c.onTimeout(h);
    check(h.log.size() == 3, "second timeout does nothing");
    check(!c.isWaiting(), "idle after timeout");

    // 3. Part 2 not assigned: part 1 runs normally, then the gesture starts fresh.
    c = new MultiPartCore();
    h = new FakeHost();
    h.firstOn.add(13);
    c.onGesture(h, 13);
    boolean taken = c.onGesture(h, 2); // 2 is not a part 1 and has no action
    check(!taken, "unassigned part 2 falls through to normal handling");
    check(h.log.toString().equals("[sound1, wait 500, normal 13]"), "log " + h.log);
    check(!c.isWaiting(), "idle");

    // 4. Part 2 unassigned but itself a part 1: part 1 runs, new wait starts.
    c = new MultiPartCore();
    h = new FakeHost();
    h.firstOn.add(13);
    h.firstOn.add(14);
    c.onGesture(h, 13);
    taken = c.onGesture(h, 14);
    check(taken && c.isWaiting(), "new part 1 waits");
    check(h.log.toString().equals("[sound1, wait 500, normal 13, sound1, wait 500]"), "log " + h.log);

    // 5. Same gesture twice (right, right) works.
    c = new MultiPartCore();
    h = new FakeHost();
    h.firstOn.add(4);
    h.seconds.add("4:4");
    c.onGesture(h, 4);
    c.onGesture(h, 4);
    check(h.log.toString().equals("[sound1, wait 500, sound2, second 4:4]"), "right right " + h.log);

    // 6. Switch off for a gesture: nothing is taken, no delay.
    c = new MultiPartCore();
    h = new FakeHost();
    check(!c.onGesture(h, 3), "gesture without switch is not taken");
    check(h.log.isEmpty() && !c.isWaiting(), "no sound, no wait");

    // 7. Global switch off: nothing is taken.
    c = new MultiPartCore();
    h = new FakeHost();
    h.firstOn.add(13);
    h.enabled = false;
    check(!c.onGesture(h, 13), "global off");

    // 8. Duration comes from the host.
    c = new MultiPartCore();
    h = new FakeHost();
    h.firstOn.add(13);
    h.duration = 1000;
    c.onGesture(h, 13);
    check(h.log.contains("wait 1000"), "duration 1000");

    // 9. Reset drops a waiting part 1 without running it.
    c = new MultiPartCore();
    h = new FakeHost();
    h.firstOn.add(13);
    c.onGesture(h, 13);
    c.reset(h);
    c.onTimeout(h);
    check(!h.log.contains("normal 13"), "reset runs nothing");

    // 10. Key numbers are unique for all pairs and never clash with plain gesture ids.
    java.util.Set<Integer> seen = new java.util.HashSet<>();
    int[] firsts = {1, 2, 16, 29, 30, 31, 32, 33, 34, 35, 36};
    int[] seconds = {0, 1, 16, 25, 26, 40};
    for (int f : firsts) {
      for (int s : seconds) {
        int k = 100000 + f * 1000 + s;
        check(seen.add(k), "key unique " + k);
        check(k > 100, "key above plain ids");
      }
    }

    System.out.println(fail == 0 ? "ALL PASS" : fail + " FAILED");
    if (fail != 0) {
      System.exit(1);
    }
  }
}
