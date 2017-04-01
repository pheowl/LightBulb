package com.example.lightbulb;

import java.util.Random;

import android.os.AsyncTask;

public class CandleTask extends AsyncTask<String, Void, String> {

	boolean stopped = false;
	static final int MAXLEN = 20;
	Random random;
	int bright=0;
	int randBase = 48;
	int offset = 32;
	int [] brightarr = new int[MAXLEN];
	
	
	public CandleTask()
	{
		random = new Random();
		for(int i=0;i<MAXLEN;i++)
		{
			brightarr[i] = 0;
		}
	}
	
	@Override
	protected String doInBackground(String... params) {
		int counter = 0;
		while(!stopped)
		{
			if(counter == MAXLEN) counter = 0;
			if(randBase!=0)	brightarr[counter] = random.nextInt(randBase) + offset;
			else brightarr[counter] = offset;
			try
			{
				Thread.sleep(15);
			}
			catch (InterruptedException e)
			{
			}
			counter++;
		}
		return null;
	}
	
	public void stop()
	{
		stopped = true;
	}
	
	public int getBright()
	{
		bright = 0;
		for(int i=0;i<MAXLEN;i++) bright+=brightarr[i];
		return bright/MAXLEN;
	}
	
	public void setOffset(int os)
	{
		if(os > 255) offset = 255;
		else if(os < 0) offset = 0;
		else offset = os;
	}
	
	public void setFlicker(int fl)
	{
		if(255-offset < fl) randBase = 255-offset;
		else if(fl < 0) randBase = 0;
		else randBase = fl;
	}

}
