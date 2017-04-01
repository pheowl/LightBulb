package com.example.lightbulb;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder.AudioSource;
import android.os.AsyncTask;
import android.util.Log;

public class VisualizerTask  extends AsyncTask<String, Void, String> 
{

	private AudioRecord rec;
	private FFT fourier; 
	private boolean stopped = false;
	private int n = 4096;
	private double [] x,y;
	private short [] buffer;
	private double [] currResult;
	private double mean = 0;
	private int j=0;
	private int k=0;
	
	public VisualizerTask(int nFFT)
	{
		int m = (int) (Math.log(nFFT) / Math.log(2));
		while (nFFT != (1 << m))
		{
			nFFT++;
			m = (int) (Math.log(nFFT) / Math.log(2));
		}
		n = nFFT;
	          
		fourier = new FFT(n);
		buffer = new short[n];
		x = new double[n];
		y = new double[n];
		currResult = new double[n/2];
		
		//44100?
		int N = AudioRecord.getMinBufferSize(10000,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT);
		Log.i("LightBulb","whatever this is?" + String.valueOf(N));
		rec = new AudioRecord(AudioSource.MIC, 10000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, N);
	}
	
	@Override
	protected String doInBackground(String... arg0) {
		if(AudioRecord.STATE_INITIALIZED==rec.getState())
		{
			rec.startRecording();
		}
        stopped = false;
        int i=0;
        long in,fi;
		while(!stopped)
		{
			in = System.currentTimeMillis();
			rec.read(buffer, 0, n/4);
			for(i=0;i<n;i++)
			{
				if(i<n/4) x[i]=buffer[i];
				else x[i] = 0;
				y[i]=0;
			}

			fourier.fft(x, y);
			for(i=0;i<n/2;i++)
			{
				currResult[i] = Math.sqrt((Math.pow(x[i],2) + Math.pow(y[i],2)))/32767./1024.;
			}
			fi = System.currentTimeMillis();
			Log.i("LightBulb",String.valueOf(fi-in));
		}
		rec.stop();
		rec.release();
		return "done";
	}
	
	@Override
	protected void onPostExecute(String result) 
	{
	}
	
	public void stop()
	{
		stopped=true;
	}
	
	public int getMaxBetween(int min,int max)
	{
		if(max>n/2) max = n/2;
		if(min<0) min = 0;
		k = 0;
		mean = currResult[min];
		for(j=min;j<max;j++)
		{
			if(mean<currResult[j]) 
			{
				mean = currResult[j];
				k = j;
			}
		}
		return k;
	}
	
	public double getAmp()
	{
		mean = currResult[0];
		for(j=0;j<n/2;j++)
		{
			if(mean<currResult[j]) mean = currResult[j];
		}
		return mean;
	}
	
	public double getMeanAmpBetween(int min,int max)
	{
		if(max>n/2) max = n/2;
		if(min<0) min = 0;
		mean = 0;
		for(j=min;j<max;j++)
		{
			mean+=currResult[j];
		}
		return mean/(max-min);
	}
}
