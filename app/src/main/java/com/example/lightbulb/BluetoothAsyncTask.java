package com.example.lightbulb;

import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

public class BluetoothAsyncTask extends AsyncTask<Void,String,Integer>
{
	
	private static final String TAG = "BluetoothHandle";
    private static final boolean D = true;
    // Well known SPP UUID (will *probably* map to
    // RFCOMM channel 1 (default) if not in use);
    // see comments in onResume().
    private static final UUID MY_UUID = 
                    UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");


    // ==> hardcode your server's MAC address here <==
    private String address;// = "00:12:12:04:06:29";
    
	public final static int REQUEST_ENABLE_BT = 4;
    
	private BluetoothAdapter mBluetoothAdapter = null;
    private BluetoothSocket btSocket = null;
    private OutputStream outStream = null;
    private Context context;
    private ProgressDialog dialog; 
    private boolean established=false; 
    AlertDialog.Builder builder;
    DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            switch (which){
            case DialogInterface.BUTTON_POSITIVE:
            	
                break;

            case DialogInterface.BUTTON_NEGATIVE:

    			Toast.makeText(context, 
    					"Connection lost, no attempt to reconnect was made", 
    					Toast.LENGTH_LONG).show();
    			((Activity) context).finish();
                break;
            }
        }
    };
    
    public BluetoothAsyncTask(Context cxt,String mac)
    {
    	context = cxt;
    	address = mac;
    	dialog = new ProgressDialog(context);
	    
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if (mBluetoothAdapter == null) {
			Toast.makeText(context, 
					"Bluetooth is not available.", 
					Toast.LENGTH_LONG).show();
			((Activity) context).finish();
			return;
		}

		if (!mBluetoothAdapter.isEnabled()) 
		{
		    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
		    ((Activity) context).startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
		}
		
	   
	    builder = new AlertDialog.Builder(context);
	    builder.setMessage("Attempt to reconnect?").setPositiveButton("Yes", dialogClickListener)
	        .setNegativeButton("No", dialogClickListener);
    }
    
    public void pause()
    {
    	if (D)
    		Log.e(TAG, "- ON PAUSE -");
	    	
    	if (outStream != null) 
    	{
    		try 
    		{
    			outStream.flush();
    		} catch (IOException e) 
    		{
    			Log.e(TAG, "ON PAUSE: Couldn't flush output stream.", e);
    		}
    	}
    	
    	try
    	{
    		btSocket.close();
    	} catch (IOException e2) 
    	{
    		Log.e(TAG, "ON PAUSE: Unable to close socket.", e2);
    	}
    	established = false;
   	}
    
    public void send(byte[] msgBuffer)
    {
    	if(established)
    	{
    		try {
    			outStream.write(msgBuffer);
    		} catch (IOException e) {
    			connectionLost();
    			Log.e(TAG, "ON RESUME: Exception during write.", e);
    		}
    	}
    }
    
    private void connectionLost()
    {
	    builder.show(); 
    }

    @Override
    protected void onPostExecute(Integer res)
    {
    	if(-1 == res)
    	{
    		dialog.dismiss();
    		established = false;
    		/*
			Toast.makeText(context, 
					"Connection could not be enabled, please retry", 
					Toast.LENGTH_LONG).show();
			((Activity) context).finish();*/
    	}
    	else
    	{
    		dialog.dismiss();
    		established = true;
    	}
    }
    
    @Override
    protected void onPreExecute()
    {
    	dialog.setTitle("Please wait, connection is being established");
    	dialog.setCanceledOnTouchOutside(false);
    	dialog.setCancelable(false);
    	dialog.show();
		Log.e(TAG,"pre ex now");
    }

	@Override
	protected Integer doInBackground(Void... arg0)
	{
    	if (D) {
            Log.e(TAG, "+ ON RESUME +");
            Log.e(TAG, "+ ABOUT TO ATTEMPT CLIENT CONNECT +");
    	}
    	// When this returns, it will 'know' about the server,
    	// via it's MAC address.
    	BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);

    	// We need two things before we can successfully connect
    	// (authentication issues aside): a MAC address, which we
    	// already have, and an RFCOMM channel.
    	// Because RFCOMM channels (aka ports) are limited in
    	// number, Android doesn't allow you to use them directly;
    	// instead you request a RFCOMM mapping based on a service
    	// ID. In our case, we will use the well-known SPP Service
    	// ID. This ID is in UUID (GUID to you Microsofties)
    	// format. Given the UUID, Android will handle the
    	// mapping for you. Generally, this will return RFCOMM 1,
    	// but not always; it depends what other BlueTooth services
    	// are in use on your Android device.
    	try {
    		Log.e(TAG, "rfcomm establishing");
            btSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
    	} catch (IOException e) {
            Log.e(TAG, "ON RESUME: Socket creation failed.", e);
            return -1;
    	}

    	// Discovery may be going on, e.g., if you're running a
    	// 'scan for devices' search from your handset's Bluetooth
    	// settings, so we call cancelDiscovery(). It doesn't hurt
    	// to call it, but it might hurt not to... discovery is a
    	// heavyweight process; you don't want it in progress when
    	// a connection attempt is made.
    	mBluetoothAdapter.cancelDiscovery();

		Log.e(TAG, "discovery cancelled");
    	// Blocking connect, for a simple client nothing else can
    	// happen until a successful connection is made, so we
    	// don't care if it blocks.
    	try
    	{
    		Log.e(TAG, "connecting");
    		btSocket.connect();
    		Log.e(TAG, "connected");
    		Log.e(TAG, "ON RESUME: BT connection established, data transfer link open.");
    	} catch (IOException e)
    	{
    		//try 
    		//{
        		Log.e(TAG, "ON RESUME: Exception received - stopping connection.");
                //btSocket.close();
        		return -1;
    		/*} catch (IOException e2) 
            {
                    Log.e(TAG, 
                            "ON RESUME: Unable to close socket during connection failure", e2);
            		return -1;
            }*/
    	}

    	// Create a data stream so we can talk to server.
    	if (D)
            Log.e(TAG, "+ ABOUT TO SAY SOMETHING TO SERVER +");

    	if(isCancelled()) return -1;
    	try
    	{
            outStream = btSocket.getOutputStream();
    	} catch (IOException e) 
    	{
            Log.e(TAG, "ON RESUME: Output stream creation failed.", e);
            return -1;
    	}
    	return 0;
	}
	
	public boolean getState()
	{
		return established;
	}

	
}
