/*
 * Copyright 2026 MS Screen Reader. Licensed under the Apache License, Version 2.0.
 */
package com.google.android.accessibility.talkback;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Turns an Android tombstone (the protobuf that ApplicationExitInfo.getTraceInputStream gives for a
 * native crash) into a short readable text: signal, cause, and the stack of the thread that
 * crashed first, then only the top frames of the other threads. Pure Java, never throws. Returns
 * null when nothing readable is found (the caller then falls back to the raw text).
 *
 * <p>Field numbers follow AOSP tombstone.proto: Tombstone {tid=6, signal_info=10, causes=15,
 * threads=16 (map entry: key=1, value=2)}; Signal {name=2, code_name=4}; Cause {human_readable=1};
 * Thread {id=1, name=2, current_backtrace=4}; BacktraceFrame {function_name=4, file_name=6}.
 */
final class TombstoneSummary {
  private TombstoneSummary() {}

  private static final class Reader {
    final byte[] b;
    int p;
    final int end;

    Reader(byte[] b, int start, int end) {
      this.b = b;
      this.p = start;
      this.end = end;
    }

    boolean more() {
      return p < end;
    }

    long varint() {
      long v = 0;
      int shift = 0;
      while (p < end && shift < 64) {
        int x = b[p++] & 0xff;
        v |= (long) (x & 0x7f) << shift;
        if ((x & 0x80) == 0) {
          return v;
        }
        shift += 7;
      }
      throw new IllegalStateException("bad varint");
    }
  }

  private static final class Field {
    int num;
    int wire;
    long value; // varint value
    int start; // payload start (wire 2)
    int len; // payload length (wire 2)
  }

  private static Field next(Reader r) {
    Field f = new Field();
    long tag = r.varint();
    f.num = (int) (tag >>> 3);
    f.wire = (int) (tag & 7);
    switch (f.wire) {
      case 0:
        f.value = r.varint();
        break;
      case 1:
        r.p += 8;
        break;
      case 5:
        r.p += 4;
        break;
      case 2:
        f.len = (int) r.varint();
        f.start = r.p;
        if (f.len < 0 || f.start + f.len > r.end) {
          throw new IllegalStateException("bad length");
        }
        r.p += f.len;
        break;
      default:
        throw new IllegalStateException("bad wire type");
    }
    if (r.p > r.end) {
      throw new IllegalStateException("overrun");
    }
    return f;
  }

  private static String str(byte[] b, Field f) {
    return new String(b, f.start, f.len, StandardCharsets.UTF_8);
  }

  private static final class Thr {
    long id;
    String name = "";
    final List<String> frames = new ArrayList<>();
  }

  /** Short text for the tombstone, or null if it could not be read. */
  static String summarize(byte[] b, int maxChars) {
    try {
      if (b == null || b.length == 0) {
        return null;
      }
      long crashTid = -1;
      String signal = "";
      List<String> causes = new ArrayList<>();
      List<Thr> threads = new ArrayList<>();
      Reader top = new Reader(b, 0, b.length);
      while (top.more()) {
        Field f = next(top);
        if (f.num == 6 && f.wire == 0) {
          crashTid = f.value;
        } else if (f.num == 10 && f.wire == 2) {
          signal = signal(b, f);
        } else if (f.num == 15 && f.wire == 2) {
          Reader c = new Reader(b, f.start, f.start + f.len);
          while (c.more()) {
            Field cf = next(c);
            if (cf.num == 1 && cf.wire == 2) {
              causes.add(str(b, cf));
            }
          }
        } else if (f.num == 16 && f.wire == 2) {
          Thr t = thread(b, f);
          if (t != null) {
            threads.add(t);
          }
        }
      }
      if (threads.isEmpty() && signal.isEmpty() && causes.isEmpty()) {
        return null;
      }
      StringBuilder sb = new StringBuilder();
      if (!signal.isEmpty()) {
        sb.append("signal: ").append(signal).append('\n');
      }
      for (String c : causes) {
        sb.append("cause: ").append(c).append('\n');
      }
      sb.append("crashing thread id: ").append(crashTid).append('\n');
      // Crashing thread first, in full.
      for (Thr t : threads) {
        if (t.id == crashTid) {
          sb.append("\n== CRASHED THREAD ").append(t.name).append(" (").append(t.id).append(")\n");
          for (String fr : t.frames) {
            sb.append("  ").append(fr).append('\n');
          }
        }
      }
      for (Thr t : threads) {
        if (t.id != crashTid) {
          sb.append("-- ").append(t.name).append(": ");
          int n = Math.min(3, t.frames.size());
          for (int i = 0; i < n; i++) {
            if (i > 0) {
              sb.append(" < ");
            }
            sb.append(t.frames.get(i));
          }
          sb.append('\n');
        }
        if (sb.length() > maxChars) {
          break;
        }
      }
      if (sb.length() > maxChars) {
        sb.setLength(maxChars);
        sb.append("\n...(cut)");
      }
      return sb.toString();
    } catch (RuntimeException e) {
      return null;
    }
  }

  private static String signal(byte[] b, Field f) {
    Reader r = new Reader(b, f.start, f.start + f.len);
    String name = "";
    String code = "";
    while (r.more()) {
      Field x = next(r);
      if (x.num == 2 && x.wire == 2) {
        name = str(b, x);
      } else if (x.num == 4 && x.wire == 2) {
        code = str(b, x);
      }
    }
    return (name + " " + code).trim();
  }

  private static Thr thread(byte[] b, Field mapEntry) {
    Reader e = new Reader(b, mapEntry.start, mapEntry.start + mapEntry.len);
    while (e.more()) {
      Field v = next(e);
      if (v.num == 2 && v.wire == 2) {
        Thr t = new Thr();
        Reader r = new Reader(b, v.start, v.start + v.len);
        while (r.more()) {
          Field x = next(r);
          if (x.num == 1 && x.wire == 0) {
            t.id = x.value;
          } else if (x.num == 2 && x.wire == 2) {
            t.name = str(b, x);
          } else if (x.num == 4 && x.wire == 2) {
            t.frames.add(frame(b, x));
          }
        }
        return t;
      }
    }
    return null;
  }

  private static String frame(byte[] b, Field f) {
    Reader r = new Reader(b, f.start, f.start + f.len);
    String fn = "";
    String file = "";
    while (r.more()) {
      Field x = next(r);
      if (x.num == 4 && x.wire == 2) {
        fn = str(b, x);
      } else if (x.num == 6 && x.wire == 2) {
        file = str(b, x);
      }
    }
    int slash = file.lastIndexOf('/');
    if (slash >= 0) {
      file = file.substring(slash + 1);
    }
    return fn.isEmpty() ? file : fn + " [" + file + "]";
  }
}
