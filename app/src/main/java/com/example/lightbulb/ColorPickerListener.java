package com.example.lightbulb;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

public class ColorPickerListener extends LightBulbMain implements ImageView.OnTouchListener
{
	private int pixel = 0;
	private int x = 0;
	private int y = 0;
	private Bitmap bitmap;
	private ImageView imageView;
	private IntValueStore clr;
	private int[] str = new int[3];
	
	public ColorPickerListener(IntValueStore color)
	{
		clr = color;
	}
	
	@Override
	public boolean onTouch(View v, MotionEvent event) 
	{
		if (event.getAction() == MotionEvent.ACTION_MOVE
			|| event.getAction() == MotionEvent.ACTION_DOWN
			|| event.getAction() == MotionEvent.ACTION_UP) 
		{	
			pixel = 0;
			x = (int) event.getX();
			y = (int) event.getY();
			imageView = ((ImageView)v);
			imageView.buildDrawingCache();
			bitmap = imageView.getDrawingCache();
			try
			{
				pixel = bitmap.getPixel(x, y);
				str[0] = Color.red(pixel);
				str[1] = Color.green(pixel);
				str[2] = Color.blue(pixel);
				clr.setValue(str,IntValueStore.SOURCE_GUI);
            } catch(IllegalArgumentException e)
            {
            	Log.e("LightBulb","Touch exceeding borders");
            }
			return true;
		}
		return false;
	}
}
