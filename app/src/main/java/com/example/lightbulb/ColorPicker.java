package com.example.lightbulb;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ComposeShader;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.Shader.TileMode;
import android.graphics.SweepGradient;
import android.util.AttributeSet;
import android.widget.ImageView;

public class ColorPicker extends ImageView {

	public ColorPicker(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub
	}
    Paint paint;
    Shader luar;
    final float[] color = { 1.f, 1.f, 1.f };

    public ColorPicker(Context context, AttributeSet attrs, int defStyle) {
            super(context, attrs, defStyle);
    }


    @SuppressLint("DrawAllocation") @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            int smaller_size = this.getMeasuredWidth()/2;
            if (smaller_size > this.getMeasuredHeight()/2) smaller_size = this.getMeasuredHeight()/2;
            if (paint == null) {
                    paint = new Paint();
                    luar = new SweepGradient(this.getMeasuredWidth()/2,this.getMeasuredHeight()/2, new int[] { Color.RED, Color.YELLOW, Color.GREEN, Color.CYAN, Color.BLUE, Color.MAGENTA, Color.RED }, null);
            }
            Shader dalam = new RadialGradient(this.getMeasuredWidth()/2,this.getMeasuredHeight()/2,smaller_size,new int[] { 0x0000000, 0x00000000, 0xff000000},null, TileMode.CLAMP);
            ComposeShader shader = new ComposeShader(luar, dalam, PorterDuff.Mode.DARKEN);
            dalam = new RadialGradient(this.getMeasuredWidth()/2,this.getMeasuredHeight()/2,smaller_size,new int[] { 0xffffffff, 0x00000000, 0x00000000},null, TileMode.CLAMP);
            shader = new ComposeShader(shader,dalam,PorterDuff.Mode.LIGHTEN);
            paint.setShader(shader);
            canvas.drawCircle(this.getMeasuredWidth()/2,this.getMeasuredHeight()/2,smaller_size, paint);
    }


    void setHue(float hue) {
            color[0] = hue;
            invalidate();
    }
}

