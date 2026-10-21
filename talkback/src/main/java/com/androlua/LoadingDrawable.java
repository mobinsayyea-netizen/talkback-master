package com.androlua;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;
import android.util.TypedValue;

public class LoadingDrawable extends Drawable {
    static final int STATE_FAIL = -1;
    static final int STATE_LOADING = 0;
    static final int STATE_SUCCE = 1;

    /* renamed from: dm */
    private final DisplayMetrics f1310dm;
    private int mState;

    /* renamed from: p */
    private Paint f1313p;

    /* renamed from: n */
    private int f1312n = 0;

    /* renamed from: m */
    private int f1311m = 0;

    /* renamed from: x */
    private int f1316x = 0;

    /* renamed from: y */
    private int f1317y = 0;

    /* renamed from: sn */
    private int f1315sn = 3;

    /* renamed from: sm */
    private int f1314sm = 1;

    public LoadingDrawable(Context context) {
        this.f1310dm = context.getResources().getDisplayMetrics();
        Paint paint = new Paint();
        this.f1313p = paint;
        paint.setStyle(Paint.Style.STROKE);
        this.f1313p.setAntiAlias(true);
        this.f1313p.setStrokeWidth(m1371dp(8.0f));
        this.f1313p.setColor(-2004318072);
    }

    /* renamed from: dp */
    private int m1371dp(float f5) {
        return (int) TypedValue.applyDimension(1, f5, this.f1310dm);
    }

    @Override // android.graphics.drawable.Drawable
    public void draw(Canvas canvas) {
        int i5;
        int i6;
        Rect bounds = getBounds();
        float f5 = 0.15f * r2;
        float f6 = r2 * 0.85f;
        RectF rectF = new RectF(f5, f5, f6, f6);
        int i7 = this.f1312n;
        if (i7 >= 360 && this.mState == 0) {
            this.f1314sm = 8;
            this.f1315sn = -6;
        } else if (i7 <= 6) {
            this.f1315sn = 6;
            this.f1314sm = 2;
        }
        if (i7 < 360 || this.mState == 0) {
            if (this.mState == 0) {
                this.f1312n = i7 + this.f1315sn;
                i5 = this.f1311m;
                i6 = this.f1314sm;
            } else {
                this.f1312n = i7 + (this.f1315sn * 2);
                i5 = this.f1311m;
                i6 = this.f1314sm * 2;
            }
            int i8 = i5 + i6;
            this.f1311m = i8;
            this.f1311m = i8 % 360;
        }
        canvas.drawArc(rectF, this.f1311m, this.f1312n, false, this.f1313p);
        if (this.f1312n >= 360) {
            this.f1315sn = -6;
            this.f1314sm = 8;
            int i9 = this.mState;
            if (i9 == 1) {
                Path path = new Path();
                path.moveTo(bounds.right * 0.3f, bounds.bottom * 0.5f);
                path.lineTo(bounds.right * 0.45f, bounds.bottom * 0.7f);
                path.lineTo(bounds.right * 0.75f, bounds.bottom * 0.4f);
                canvas.drawPath(path, this.f1313p);
            } else if (i9 == -1) {
                int i10 = bounds.right;
                int i11 = bounds.bottom;
                canvas.drawLine(i10 / 2, i11 * 0.25f, i10 / 2, i11 * 0.65f, this.f1313p);
                int i12 = bounds.right;
                int i13 = bounds.bottom;
                canvas.drawLine(i12 / 2, 0.7f * i13, i12 / 2, i13 * 0.75f, this.f1313p);
            }
        }
        invalidateSelf();
    }

    void fail() {
        this.mState = -1;
    }

    @Override // android.graphics.drawable.Drawable
    public int getOpacity() {
        return 0;
    }

    void loading() {
        reset();
    }

    void reset() {
        this.mState = 0;
        this.f1315sn = 3;
        this.f1314sm = 1;
        this.f1312n = 0;
        this.f1311m = 0;
        this.f1316x = 0;
        this.f1317y = 0;
        invalidateSelf();
    }

    @Override // android.graphics.drawable.Drawable
    public void setAlpha(int i5) {
        this.f1313p.setAlpha(i5);
    }

    public void setColor(int i5) {
        this.f1313p.setColor(i5);
    }

    @Override // android.graphics.drawable.Drawable
    public void setColorFilter(ColorFilter colorFilter) {
        this.f1313p.setColorFilter(colorFilter);
    }

    void setState(int i5) {
        this.mState = i5;
    }

    public void setStrokeWidth(float f5) {
        this.f1313p.setStrokeWidth(f5);
    }

    void succe() {
        this.mState = 1;
    }
}
