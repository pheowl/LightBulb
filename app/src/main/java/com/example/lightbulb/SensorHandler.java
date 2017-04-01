package com.example.lightbulb;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

public class SensorHandler {

    private SensorManager mSensorManager;
    private Sensor mGyroSensor, mAccSensor, mMagSensor, mGravSensor;
    private Context context;
    private float[] grav = new float[3];
    private float[] acc = new float[3];
    private float[] mag = new float[3];
    private float[] rot = new float[3];
    private float scale = 0;
    private int i=0;
    
    private SensorEventListener mMagListener = new SensorEventListener() {
	    @Override
	    public void onAccuracyChanged(Sensor sensor, int accuracy) {
	    }

	    @Override
	    public void onSensorChanged(SensorEvent event) {
	        mag[0] = event.values[0];
	        mag[1] = event.values[1];
	        mag[2] = event.values[2];

	    }
	};
	
	private SensorEventListener mGravListener = new SensorEventListener() {
	    @Override
	    public void onAccuracyChanged(Sensor sensor, int accuracy) {
	    }

	    @Override
	    public void onSensorChanged(SensorEvent event) {
	        grav[0] = event.values[0];
	        grav[1] = event.values[1];
	        grav[2] = event.values[2];

	    }
	};
	
	private SensorEventListener mGyroListener = new SensorEventListener() {

	    private static final float MIN_TIME_STEP = (1f / 40f);
	    private long mLastTime = System.currentTimeMillis();
	    private float mRotationX, mRotationY, mRotationZ;
	    private long now;
	    private float timeDiff;
	    private float x;
	    private float y;
	    private float z;
	    private float angularVelocity;
	    
	    @Override
	    public void onAccuracyChanged(Sensor sensor, int accuracy) {
	    }

	    @Override
	    public void onSensorChanged(SensorEvent event) {
	        x = event.values[0];
	        y = event.values[1];
	        z = event.values[2];

	        angularVelocity = z * 0.96f; // Minor adjustment to avoid drift on Nexus S

	        // Calculate time diff
	        now = System.currentTimeMillis();
	        timeDiff = (now - mLastTime) / 1000f;
	        mLastTime = now;
	        if (timeDiff > 1) {
	            // Make sure we don't go bananas after pause/resume
	            timeDiff = MIN_TIME_STEP;
	        }

	        mRotationX += x * timeDiff;
	        if (mRotationX > 0.5f)
	            mRotationX = 0.5f;
	        else if (mRotationX < -0.5f)
	            mRotationX = -0.5f;

	        mRotationY += y * timeDiff;
	        if (mRotationY > 0.5f)
	            mRotationY = 0.5f;
	        else if (mRotationY < -0.5f)
	            mRotationY = -0.5f;

	        mRotationZ += angularVelocity * timeDiff;

	        rot[0] = mRotationX;
	        rot[1] = mRotationY;
	        rot[2] = mRotationZ;
	    }
	};

	private SensorEventListener mAccListener = new SensorEventListener() {
	    @Override
	    public void onAccuracyChanged(Sensor sensor, int accuracy) {
	    }

	    @Override
	    public void onSensorChanged(SensorEvent event) {
	        acc[0] = event.values[0];
	        acc[1] = event.values[1];
	        acc[2] = event.values[2];

	        // Ignoring orientation since the activity is using screenOrientation "nosensor"

	    }
	};
	
	public SensorHandler(Context ctx)
	{
		context = ctx;
	    mSensorManager = (SensorManager) context.getSystemService(Activity.SENSOR_SERVICE);

	    mGravSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
	    mGyroSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
	    mAccSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
	    mMagSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);	
	}
	
	public float[] getAcc()
	{
		return acc;
	}
	
	public float[] getGrav()
	{
		if(null == mGravSensor) 
		{
			scale = (float) Math.sqrt(96.17038122091692999991/(Math.pow(acc[0],2) + Math.pow(acc[1],2) + Math.pow(acc[2],2)));
			for(i=0;i<3;i++)
			{
				acc[i]=acc[i]*scale;
			}
			Log.e("BlaSens","no grav sense");
			return acc;
		}
		else return grav;
	}
	
	public void registerSensors()
	{
	    mSensorManager.registerListener(mGyroListener, mGyroSensor, SensorManager.SENSOR_DELAY_UI);
	    mSensorManager.registerListener(mAccListener, mAccSensor, SensorManager.SENSOR_DELAY_UI);
	    mSensorManager.registerListener(mMagListener, mMagSensor, SensorManager.SENSOR_DELAY_UI);	
	    mSensorManager.registerListener(mGravListener, mGravSensor, SensorManager.SENSOR_DELAY_FASTEST);
	}
	
	public void unregisterSensors()
	{
	    mSensorManager.unregisterListener(mGyroListener, mGyroSensor);
	    mSensorManager.unregisterListener(mAccListener, mAccSensor);
	    mSensorManager.unregisterListener(mMagListener, mMagSensor);	
	    mSensorManager.unregisterListener(mGravListener, mGravSensor);
	}
}
