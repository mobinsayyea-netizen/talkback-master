package com.androlua;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;

/**
 * Background image for loadlayout.lua: new LuaBitmapDrawable(context, pathOrUrl). Loads a local
 * file (relative to the script folder, or absolute) or an http(s) address through LuaBitmap. If the
 * image cannot be loaded it simply draws nothing (the layout still opens).
 */
public class LuaBitmapDrawable extends BitmapDrawable {
  public static final int MATRIX = 0;
  public static final int FIT_XY = 1;
  public static final int FIT_START = 2;
  public static final int FIT_CENTER = 3;
  public static final int FIT_END = 4;
  public static final int CENTER = 5;
  public static final int CENTER_CROP = 6;
  public static final int CENTER_INSIDE = 7;

  public LuaBitmapDrawable(LuaContext luaContext, String path) {
    super(luaContext.getContext().getResources(), load(luaContext, path));
  }

  private static Bitmap load(LuaContext luaContext, String path) {
    try {
      if (path == null || path.length() == 0) {
        return null;
      }
      return LuaBitmap.getBitmap(luaContext, path);
    } catch (Throwable t) {
      return null;
    }
  }

  public static long getCacheTime() {
    return LuaBitmap.getCacheTime();
  }
}
