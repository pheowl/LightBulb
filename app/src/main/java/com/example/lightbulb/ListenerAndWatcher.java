package com.example.lightbulb;

import android.widget.SeekBar;

public class ListenerAndWatcher extends LightBulbMain implements SeekBar.OnSeekBarChangeListener
{
	private int field;
	private IntValueStore clr;
	private int[] str = new int[3];
	
	public ListenerAndWatcher(int f,IntValueStore color)
	{
		field = f;
		clr = color;
	}

	@Override
	public void onProgressChanged(SeekBar seekBar, int progress,
			boolean fromUser) {
		str = clr.getValue();
		str[field]=progress;
		clr.setValue(str,IntValueStore.SOURCE_GUI);
	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
		
	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
		seekBar.setSecondaryProgress(seekBar.getProgress());
	}
}
