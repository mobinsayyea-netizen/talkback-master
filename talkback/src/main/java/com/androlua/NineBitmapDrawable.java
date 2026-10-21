package com.androlua;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

public class NineBitmapDrawable extends Drawable implements LuaGcable {
    private Bitmap mBitmap;
    private boolean mGc;

    /* renamed from: mH */
    private int f1346mH;
    private Paint mPaint;
    private Rect mRect1;
    private Rect mRect2;
    private Rect mRect3;
    private Rect mRect4;
    private Rect mRect5;
    private Rect mRect6;
    private Rect mRect7;
    private Rect mRect8;
    private Rect mRect9;

    /* renamed from: mW */
    private int f1347mW;
    private int mX1;
    private int mX2;
    private int mY1;
    private int mY2;

    public NineBitmapDrawable(Bitmap bitmap) {
        int i5;
        int i6;
        int i7;
        this.mPaint = new Paint();
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int i8 = 0;
        for (int i9 = 0; i9 < width; i9++) {
            int pixel = bitmap.getPixel(i9, 0);
            if (pixel == -16777216) {
                i5 = i9;
                break;
            } else {
                if (pixel != -1 && pixel != 0) {
                    break;
                }
            }
        }
        i5 = 0;
        if (i5 == 0 || i5 == width - 1) {
            throw new IllegalArgumentException("not found x1");
        }
        int i10 = i5;
        while (true) {
            if (i10 >= width) {
                i6 = 0;
                break;
            } else {
                if (bitmap.getPixel(i10, 0) != -16777216) {
                    i6 = width - i10;
                    break;
                }
                i10++;
            }
        }
        if (i6 == 0 || i6 == 1) {
            throw new IllegalArgumentException("not found x2");
        }
        for (int i11 = 0; i11 < height; i11++) {
            int pixel2 = bitmap.getPixel(0, i11);
            if (pixel2 == -16777216) {
                i7 = i11;
                break;
            } else {
                if (pixel2 != -1 && pixel2 != 0) {
                    break;
                }
            }
        }
        i7 = 0;
        if (i7 == 0 || i7 == height - 1) {
            throw new IllegalArgumentException("not found y1");
        }
        int i12 = i7;
        while (true) {
            if (i12 >= height) {
                break;
            }
            if (bitmap.getPixel(0, i12) != -16777216) {
                i8 = height - i12;
                break;
            }
            i12++;
        }
        int i13 = i8;
        if (i13 == 0 || i13 == 1) {
            throw new IllegalArgumentException("not found y2");
        }
        init(bitmap, i5, i7, i6, i13);
    }

    public NineBitmapDrawable(Bitmap bitmap, int i5, int i6, int i7, int i8) {
        this.mPaint = new Paint();
        init(bitmap, i5, i6, i7, i8);
    }

    public NineBitmapDrawable(String str) throws java.io.IOException {
        this(LuaBitmap.getLocalBitmap(str));
    }

    private void init(Bitmap bitmap, int i5, int i6, int i7, int i8) {
        this.mBitmap = bitmap;
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        this.mRect1 = new Rect(1, 1, i5, i6);
        this.mRect2 = new Rect(i5, 1, i7, i6);
        int i9 = width - 1;
        this.mRect3 = new Rect(i7, 1, i9, i6);
        this.mRect4 = new Rect(1, i6, i5, i8);
        this.mRect5 = new Rect(i5, i6, i7, i8);
        this.mRect6 = new Rect(i7, i6, i9, i8);
        int i10 = height - 1;
        this.mRect7 = new Rect(1, i8, i5, i10);
        this.mRect8 = new Rect(i5, i8, i7, i10);
        this.mRect9 = new Rect(i7, i8, i9, i10);
        this.mX1 = i5;
        this.mY1 = i6;
        this.mX2 = width - i7;
        this.mY2 = height - i8;
        this.f1347mW = width;
        this.f1346mH = height;
    }

    @Override // android.graphics.drawable.Drawable
    public void draw(Canvas canvas) {
        Rect bounds = getBounds();
        int i5 = bounds.right;
        int i6 = bounds.bottom;
        float min = Math.min((i5 * 1.0f) / this.f1347mW, (i6 * 1.0f) / this.f1346mH);
        int i7 = (int) (this.mX1 * min);
        int i8 = (int) (this.mX2 * min);
        int i9 = (int) (this.mY1 * min);
        Rect rect = new Rect(0, 0, i7, i9);
        int i10 = i5 - i8;
        Rect rect2 = new Rect(i7, 0, i10, i9);
        Rect rect3 = new Rect(i10, 0, i5, i9);
        int i11 = i6 - ((int) (this.mY2 * min));
        Rect rect4 = new Rect(0, i9, i7, i11);
        Rect rect5 = new Rect(i7, i9, i10, i11);
        Rect rect6 = new Rect(i10, i9, i5, i11);
        Rect rect7 = new Rect(0, i11, i7, i6);
        Rect rect8 = new Rect(i7, i11, i10, i6);
        Rect rect9 = new Rect(i10, i11, i5, i6);
        canvas.drawBitmap(this.mBitmap, this.mRect1, rect, this.mPaint);
        canvas.drawBitmap(this.mBitmap, this.mRect2, rect2, this.mPaint);
        canvas.drawBitmap(this.mBitmap, this.mRect3, rect3, this.mPaint);
        canvas.drawBitmap(this.mBitmap, this.mRect4, rect4, this.mPaint);
        canvas.drawBitmap(this.mBitmap, this.mRect5, rect5, this.mPaint);
        canvas.drawBitmap(this.mBitmap, this.mRect6, rect6, this.mPaint);
        canvas.drawBitmap(this.mBitmap, this.mRect7, rect7, this.mPaint);
        canvas.drawBitmap(this.mBitmap, this.mRect8, rect8, this.mPaint);
        canvas.drawBitmap(this.mBitmap, this.mRect9, rect9, this.mPaint);
    }

    @Override // com.androlua.LuaGcable
    public boolean isGc() {
        return this.mGc;
    }

    @Override // com.androlua.LuaGcable
    public void gc() {
        try {
            this.mBitmap.recycle();
            this.mGc = true;
        } catch (Exception e5) {
            e5.printStackTrace();
        }
    }

    @Override // android.graphics.drawable.Drawable
    public int getOpacity() {
        return 0;
    }

    @Override // android.graphics.drawable.Drawable
    public void setAlpha(int i5) {
        this.mPaint.setAlpha(i5);
    }

    @Override // android.graphics.drawable.Drawable
    public void setColorFilter(ColorFilter colorFilter) {
        this.mPaint.setColorFilter(colorFilter);
    }
}
