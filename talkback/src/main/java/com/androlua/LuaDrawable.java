package com.androlua;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import com.luajava.LuaException;
import com.luajava.LuaFunction;
import com.luajava.LuaObject;

/**
 * Drawable whose drawing is done by a Lua function (loadlayout.lua and Jieshuo scripts use it).
 * Same behaviour as Jieshuo's com.androlua.LuaDrawable: the function is called once with
 * (canvas, paint, drawable); if it returns a function, that function draws every later frame.
 */
public class LuaDrawable extends Drawable {
  private final LuaContext mContext;
  private final LuaObject mDraw;
  private LuaFunction mOnDraw;
  private final Paint mPaint = new Paint();

  public LuaDrawable(LuaFunction function) {
    this.mDraw = function;
    this.mContext = function.getLuaState().getContext();
  }

  @Override
  public void draw(Canvas canvas) {
    try {
      if (mOnDraw == null) {
        Object result = mDraw.call(canvas, mPaint, this);
        if (result instanceof LuaFunction) {
          mOnDraw = (LuaFunction) result;
        }
      }
      if (mOnDraw != null) {
        mOnDraw.call(canvas);
      }
    } catch (LuaException e) {
      if (mContext != null) {
        mContext.sendError("onDraw", e);
      }
    } catch (Throwable t) {
      com.google.android.accessibility.talkback.MsErrorLog.log("LuaDrawable onDraw", t);
    }
  }

  @Override
  public int getOpacity() {
    return PixelFormat.TRANSLUCENT;
  }

  public Paint getPaint() {
    return mPaint;
  }

  @Override
  public void setAlpha(int alpha) {
    mPaint.setAlpha(alpha);
  }

  @Override
  public void setColorFilter(ColorFilter filter) {
    mPaint.setColorFilter(filter);
  }
}
