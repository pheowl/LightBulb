package com.example.lightbulb;

import org.apache.http.util.EncodingUtils;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import java.lang.System;

import com.example.lightbulb.IntValueStore.IntValueStoreListener;


public class LightBulbMain extends Activity implements IntValueStoreListener
{

	public final static String TAG = "LightBulb";
	public final static boolean DEBUG = true;
	public final static boolean LOCAL_ECHO_ENABLED = true;
	
	public final static int REGULTYPE_NOREGULATION = 0;
	public final static int REGULTYPE_FOURIER = 1;
	public final static int REGULTYPE_CANDLE = 2;
	public final static int REGULTYPE_ACC = 3;
	public final static int REGULTYPE_PATTERN = 4;
	public final static int AMPSMOOTH = 2;
	public final static int FREQSMOOTH = 8;
	public final static int NFFT = 1024;
	public final static int REQUEST_ENABLE_BT = 4;
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;	
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";
	
	//ColorTrackers
	public IntValueStore color;
	public IntValueStore colorSelect;

	//Interface Objects
	private SeekBar red_bar;
	private SeekBar green_bar;
	private SeekBar blue_bar;
	private View color_disp;
	private ColorPicker cp;
	private TextView status_text;
    private ProgressDialog dialog; 
	
	//Listeners for SeekBars
	private ListenerAndWatcher red_listener;
	private ListenerAndWatcher green_listener;
	private ListenerAndWatcher blue_listener;
	
	//Settings
	private SharedPreferences sharedPref;
	private int curRegMode;
	private String curMacAdd = "00:12:12:04:06:29";
	private int curBright;
	private int curSlpDelay;
	private boolean curSlpEn;
	private int curMin = 0;
	private int curMax = 4096;
	private boolean curRegTp;
	private int curCandleOff;
	private int curCandleFl;
	
	//Handles
	private Handler handle;
	private Context context;
	
	DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            switch (which){
            case DialogInterface.BUTTON_POSITIVE:
            	BTService.connect(BluetoothAdapter.getDefaultAdapter().getRemoteDevice(curMacAdd));
                break;

            case DialogInterface.BUTTON_NEGATIVE:

    			Toast.makeText(context, 
    					"Connection lost, no attempt to reconnect was made", 
    					Toast.LENGTH_LONG).show();
    			finish();
                break;
            }
        }
    };
 
    // The Handler that gets information back from the BluetoothService
	private final Handler BTHandle = new Handler() {
    	
    	byte[] rwBuf;
    	
        @Override
        public void handleMessage(Message msg) {        	
            switch (msg.what) {
            case MESSAGE_STATE_CHANGE:
                if(DEBUG) Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
                switch (msg.arg1) {
                
                case BlueToothService.STATE_CONNECTED:
                	connectedBt = true;
                	connectingBt = false;
            		showProgressDialog(false);
                	status_text.append("\nstateConnected");
                    break;
                    
                case BlueToothService.STATE_CONNECTING:
                	connectingBt = true;
                	connectedBt = false;
            		showProgressDialog(true);
                	status_text.append("\nstateConnecting");
                	break;
                    
                case BlueToothService.STATE_LISTEN:

                	status_text.append("\nstateListen");
                	break;
                case BlueToothService.STATE_NONE:
                	if(connectingBt)
                	{
                		connectingBt = false;
                		showProgressDialog(false);
                		showDialog("Connection could not be established\nAttempt to reconnect?");
                	}
                	if(connectedBt)
                	{
                		connectedBt = false;
                		//showDialog("Connection lost\nAttempt to reconnect?");
                	}
                	status_text.append("\nstateNone");
                
                    break;
                }
                break;
                
            case MESSAGE_WRITE:
            	if (LOCAL_ECHO_ENABLED) {
            		//rwBuf = (byte[]) msg.obj;
            		//status_text.setText("\n" + rwBuf.toString());
            	}
                
                break;
                
            case MESSAGE_READ:
                rwBuf = (byte[]) msg.obj;
                if(status_text.getLineCount()>5)
                	status_text.setText("");
                //status_text.append("\n" + rwBuf.toString());
                
                break;

            case MESSAGE_DEVICE_NAME:
                // save the connected device's name
                break;
                
            case MESSAGE_TOAST:
                Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                               Toast.LENGTH_LONG).show();
                break;
            }
        }
    };    
	private BlueToothService BTService = null;
	private VisualizerTask fft_handle;
	private CandleTask candle_handle;
	private SensorHandler sHandler = null;
	
	//Common Variables and ColorTable
	private boolean connectingBt = false;
	private boolean connectedBt = false;
	private boolean enablingBt = false;
	private byte[] sendBT = new byte[4];
	private int[] colorstore = new int[3];
	private int freq;
	private double amplitude;
	private double[] candlecol = {0,0,0};
	private int[][] colorTable = { { 0, 0, 254 }, { 0, 2, 254 }, { 0, 4, 254 },
			{ 0, 6, 254 }, { 0, 8, 254 }, { 0, 10, 254 }, { 0, 12, 254 },
			{ 0, 14, 254 }, { 0, 16, 254 }, { 0, 18, 254 }, { 0, 20, 254 },
			{ 0, 22, 254 }, { 0, 24, 254 }, { 0, 26, 254 }, { 0, 28, 254 },
			{ 0, 30, 254 }, { 0, 32, 254 }, { 0, 34, 254 }, { 0, 36, 254 },
			{ 0, 38, 254 }, { 0, 40, 254 }, { 0, 42, 254 }, { 0, 44, 254 },
			{ 0, 46, 254 }, { 0, 48, 254 }, { 0, 50, 254 }, { 0, 52, 254 },
			{ 0, 54, 254 }, { 0, 56, 254 }, { 0, 58, 254 }, { 0, 60, 254 },
			{ 0, 62, 254 }, { 0, 64, 254 }, { 0, 66, 254 }, { 0, 68, 254 },
			{ 0, 70, 254 }, { 0, 72, 254 }, { 0, 74, 254 }, { 0, 76, 254 },
			{ 0, 78, 254 }, { 0, 80, 254 }, { 0, 82, 254 }, { 0, 84, 254 },
			{ 0, 86, 254 }, { 0, 88, 254 }, { 0, 90, 254 }, { 0, 92, 254 },
			{ 0, 94, 254 }, { 0, 96, 254 }, { 0, 98, 254 }, { 0, 100, 254 },
			{ 0, 102, 254 }, { 0, 104, 254 }, { 0, 106, 254 }, { 0, 108, 254 },
			{ 0, 110, 254 }, { 0, 112, 254 }, { 0, 114, 254 }, { 0, 116, 254 },
			{ 0, 118, 254 }, { 0, 120, 254 }, { 0, 122, 254 }, { 0, 124, 254 },
			{ 0, 126, 254 }, { 0, 128, 254 }, { 0, 130, 254 }, { 0, 132, 254 },
			{ 0, 134, 254 }, { 0, 136, 254 }, { 0, 138, 254 }, { 0, 140, 254 },
			{ 0, 142, 254 }, { 0, 144, 254 }, { 0, 146, 254 }, { 0, 148, 254 },
			{ 0, 150, 254 }, { 0, 152, 254 }, { 0, 154, 254 }, { 0, 156, 254 },
			{ 0, 158, 254 }, { 0, 160, 254 }, { 0, 162, 254 }, { 0, 164, 254 },
			{ 0, 166, 254 }, { 0, 168, 254 }, { 0, 170, 254 }, { 0, 172, 254 },
			{ 0, 174, 254 }, { 0, 176, 254 }, { 0, 178, 254 }, { 0, 180, 254 },
			{ 0, 182, 254 }, { 0, 184, 254 }, { 0, 186, 254 }, { 0, 188, 254 },
			{ 0, 190, 254 }, { 0, 192, 254 }, { 0, 194, 254 }, { 0, 196, 254 },
			{ 0, 198, 254 }, { 0, 200, 254 }, { 0, 202, 254 }, { 0, 204, 254 },
			{ 0, 206, 254 }, { 0, 208, 254 }, { 0, 210, 254 }, { 0, 212, 254 },
			{ 0, 214, 254 }, { 0, 216, 254 }, { 0, 218, 254 }, { 0, 220, 254 },
			{ 0, 222, 254 }, { 0, 224, 254 }, { 0, 226, 254 }, { 0, 228, 254 },
			{ 0, 230, 254 }, { 0, 232, 254 }, { 0, 234, 254 }, { 0, 236, 254 },
			{ 0, 238, 254 }, { 0, 240, 254 }, { 0, 242, 254 }, { 0, 244, 254 },
			{ 0, 246, 254 }, { 0, 248, 254 }, { 0, 250, 254 }, { 0, 252, 254 },
			{ 0, 254, 254 }, { 0, 254, 254 }, { 0, 254, 252 }, { 0, 254, 250 },
			{ 0, 254, 248 }, { 0, 254, 246 }, { 0, 254, 244 }, { 0, 254, 242 },
			{ 0, 254, 240 }, { 0, 254, 238 }, { 0, 254, 236 }, { 0, 254, 234 },
			{ 0, 254, 232 }, { 0, 254, 230 }, { 0, 254, 228 }, { 0, 254, 226 },
			{ 0, 254, 224 }, { 0, 254, 222 }, { 0, 254, 220 }, { 0, 254, 218 },
			{ 0, 254, 216 }, { 0, 254, 214 }, { 0, 254, 212 }, { 0, 254, 210 },
			{ 0, 254, 208 }, { 0, 254, 206 }, { 0, 254, 204 }, { 0, 254, 202 },
			{ 0, 254, 200 }, { 0, 254, 198 }, { 0, 254, 196 }, { 0, 254, 194 },
			{ 0, 254, 192 }, { 0, 254, 190 }, { 0, 254, 188 }, { 0, 254, 186 },
			{ 0, 254, 184 }, { 0, 254, 182 }, { 0, 254, 180 }, { 0, 254, 178 },
			{ 0, 254, 176 }, { 0, 254, 174 }, { 0, 254, 172 }, { 0, 254, 170 },
			{ 0, 254, 168 }, { 0, 254, 166 }, { 0, 254, 164 }, { 0, 254, 162 },
			{ 0, 254, 160 }, { 0, 254, 158 }, { 0, 254, 156 }, { 0, 254, 154 },
			{ 0, 254, 152 }, { 0, 254, 150 }, { 0, 254, 148 }, { 0, 254, 146 },
			{ 0, 254, 144 }, { 0, 254, 142 }, { 0, 254, 140 }, { 0, 254, 138 },
			{ 0, 254, 136 }, { 0, 254, 134 }, { 0, 254, 132 }, { 0, 254, 130 },
			{ 0, 254, 128 }, { 0, 254, 126 }, { 0, 254, 124 }, { 0, 254, 122 },
			{ 0, 254, 120 }, { 0, 254, 118 }, { 0, 254, 116 }, { 0, 254, 114 },
			{ 0, 254, 112 }, { 0, 254, 110 }, { 0, 254, 108 }, { 0, 254, 106 },
			{ 0, 254, 104 }, { 0, 254, 102 }, { 0, 254, 100 }, { 0, 254, 98 },
			{ 0, 254, 96 }, { 0, 254, 94 }, { 0, 254, 92 }, { 0, 254, 90 },
			{ 0, 254, 88 }, { 0, 254, 86 }, { 0, 254, 84 }, { 0, 254, 82 },
			{ 0, 254, 80 }, { 0, 254, 78 }, { 0, 254, 76 }, { 0, 254, 74 },
			{ 0, 254, 72 }, { 0, 254, 70 }, { 0, 254, 68 }, { 0, 254, 66 },
			{ 0, 254, 64 }, { 0, 254, 62 }, { 0, 254, 60 }, { 0, 254, 58 },
			{ 0, 254, 56 }, { 0, 254, 54 }, { 0, 254, 52 }, { 0, 254, 50 },
			{ 0, 254, 48 }, { 0, 254, 46 }, { 0, 254, 44 }, { 0, 254, 42 },
			{ 0, 254, 40 }, { 0, 254, 38 }, { 0, 254, 36 }, { 0, 254, 34 },
			{ 0, 254, 32 }, { 0, 254, 30 }, { 0, 254, 28 }, { 0, 254, 26 },
			{ 0, 254, 24 }, { 0, 254, 22 }, { 0, 254, 20 }, { 0, 254, 18 },
			{ 0, 254, 16 }, { 0, 254, 14 }, { 0, 254, 12 }, { 0, 254, 10 },
			{ 0, 254, 8 }, { 0, 254, 6 }, { 0, 254, 4 }, { 0, 254, 2 },
			{ 0, 254, 0 }, { 0, 254, 0 }, { 2, 254, 0 }, { 4, 254, 0 },
			{ 6, 254, 0 }, { 8, 254, 0 }, { 10, 254, 0 }, { 12, 254, 0 },
			{ 14, 254, 0 }, { 16, 254, 0 }, { 18, 254, 0 }, { 20, 254, 0 },
			{ 22, 254, 0 }, { 24, 254, 0 }, { 26, 254, 0 }, { 28, 254, 0 },
			{ 30, 254, 0 }, { 32, 254, 0 }, { 34, 254, 0 }, { 36, 254, 0 },
			{ 38, 254, 0 }, { 40, 254, 0 }, { 42, 254, 0 }, { 44, 254, 0 },
			{ 46, 254, 0 }, { 48, 254, 0 }, { 50, 254, 0 }, { 52, 254, 0 },
			{ 54, 254, 0 }, { 56, 254, 0 }, { 58, 254, 0 }, { 60, 254, 0 },
			{ 62, 254, 0 }, { 64, 254, 0 }, { 66, 254, 0 }, { 68, 254, 0 },
			{ 70, 254, 0 }, { 72, 254, 0 }, { 74, 254, 0 }, { 76, 254, 0 },
			{ 78, 254, 0 }, { 80, 254, 0 }, { 82, 254, 0 }, { 84, 254, 0 },
			{ 86, 254, 0 }, { 88, 254, 0 }, { 90, 254, 0 }, { 92, 254, 0 },
			{ 94, 254, 0 }, { 96, 254, 0 }, { 98, 254, 0 }, { 100, 254, 0 },
			{ 102, 254, 0 }, { 104, 254, 0 }, { 106, 254, 0 }, { 108, 254, 0 },
			{ 110, 254, 0 }, { 112, 254, 0 }, { 114, 254, 0 }, { 116, 254, 0 },
			{ 118, 254, 0 }, { 120, 254, 0 }, { 122, 254, 0 }, { 124, 254, 0 },
			{ 126, 254, 0 }, { 128, 254, 0 }, { 130, 254, 0 }, { 132, 254, 0 },
			{ 134, 254, 0 }, { 136, 254, 0 }, { 138, 254, 0 }, { 140, 254, 0 },
			{ 142, 254, 0 }, { 144, 254, 0 }, { 146, 254, 0 }, { 148, 254, 0 },
			{ 150, 254, 0 }, { 152, 254, 0 }, { 154, 254, 0 }, { 156, 254, 0 },
			{ 158, 254, 0 }, { 160, 254, 0 }, { 162, 254, 0 }, { 164, 254, 0 },
			{ 166, 254, 0 }, { 168, 254, 0 }, { 170, 254, 0 }, { 172, 254, 0 },
			{ 174, 254, 0 }, { 176, 254, 0 }, { 178, 254, 0 }, { 180, 254, 0 },
			{ 182, 254, 0 }, { 184, 254, 0 }, { 186, 254, 0 }, { 188, 254, 0 },
			{ 190, 254, 0 }, { 192, 254, 0 }, { 194, 254, 0 }, { 196, 254, 0 },
			{ 198, 254, 0 }, { 200, 254, 0 }, { 202, 254, 0 }, { 204, 254, 0 },
			{ 206, 254, 0 }, { 208, 254, 0 }, { 210, 254, 0 }, { 212, 254, 0 },
			{ 214, 254, 0 }, { 216, 254, 0 }, { 218, 254, 0 }, { 220, 254, 0 },
			{ 222, 254, 0 }, { 224, 254, 0 }, { 226, 254, 0 }, { 228, 254, 0 },
			{ 230, 254, 0 }, { 232, 254, 0 }, { 234, 254, 0 }, { 236, 254, 0 },
			{ 238, 254, 0 }, { 240, 254, 0 }, { 242, 254, 0 }, { 244, 254, 0 },
			{ 246, 254, 0 }, { 248, 254, 0 }, { 250, 254, 0 }, { 252, 254, 0 },
			{ 254, 254, 0 }, { 254, 254, 0 }, { 254, 252, 0 }, { 254, 250, 0 },
			{ 254, 248, 0 }, { 254, 246, 0 }, { 254, 244, 0 }, { 254, 242, 0 },
			{ 254, 240, 0 }, { 254, 238, 0 }, { 254, 236, 0 }, { 254, 234, 0 },
			{ 254, 232, 0 }, { 254, 230, 0 }, { 254, 228, 0 }, { 254, 226, 0 },
			{ 254, 224, 0 }, { 254, 222, 0 }, { 254, 220, 0 }, { 254, 218, 0 },
			{ 254, 216, 0 }, { 254, 214, 0 }, { 254, 212, 0 }, { 254, 210, 0 },
			{ 254, 208, 0 }, { 254, 206, 0 }, { 254, 204, 0 }, { 254, 202, 0 },
			{ 254, 200, 0 }, { 254, 198, 0 }, { 254, 196, 0 }, { 254, 194, 0 },
			{ 254, 192, 0 }, { 254, 190, 0 }, { 254, 188, 0 }, { 254, 186, 0 },
			{ 254, 184, 0 }, { 254, 182, 0 }, { 254, 180, 0 }, { 254, 178, 0 },
			{ 254, 176, 0 }, { 254, 174, 0 }, { 254, 172, 0 }, { 254, 170, 0 },
			{ 254, 168, 0 }, { 254, 166, 0 }, { 254, 164, 0 }, { 254, 162, 0 },
			{ 254, 160, 0 }, { 254, 158, 0 }, { 254, 156, 0 }, { 254, 154, 0 },
			{ 254, 152, 0 }, { 254, 150, 0 }, { 254, 148, 0 }, { 254, 146, 0 },
			{ 254, 144, 0 }, { 254, 142, 0 }, { 254, 140, 0 }, { 254, 138, 0 },
			{ 254, 136, 0 }, { 254, 134, 0 }, { 254, 132, 0 }, { 254, 130, 0 },
			{ 254, 128, 0 }, { 254, 126, 0 }, { 254, 124, 0 }, { 254, 122, 0 },
			{ 254, 120, 0 }, { 254, 118, 0 }, { 254, 116, 0 }, { 254, 114, 0 },
			{ 254, 112, 0 }, { 254, 110, 0 }, { 254, 108, 0 }, { 254, 106, 0 },
			{ 254, 104, 0 }, { 254, 102, 0 }, { 254, 100, 0 }, { 254, 98, 0 },
			{ 254, 96, 0 }, { 254, 94, 0 }, { 254, 92, 0 }, { 254, 90, 0 },
			{ 254, 88, 0 }, { 254, 86, 0 }, { 254, 84, 0 }, { 254, 82, 0 },
			{ 254, 80, 0 }, { 254, 78, 0 }, { 254, 76, 0 }, { 254, 74, 0 },
			{ 254, 72, 0 }, { 254, 70, 0 }, { 254, 68, 0 }, { 254, 66, 0 },
			{ 254, 64, 0 }, { 254, 62, 0 }, { 254, 60, 0 }, { 254, 58, 0 },
			{ 254, 56, 0 }, { 254, 54, 0 }, { 254, 52, 0 }, { 254, 50, 0 },
			{ 254, 48, 0 }, { 254, 46, 0 }, { 254, 44, 0 }, { 254, 42, 0 },
			{ 254, 40, 0 }, { 254, 38, 0 }, { 254, 36, 0 }, { 254, 34, 0 },
			{ 254, 32, 0 }, { 254, 30, 0 }, { 254, 28, 0 }, { 254, 26, 0 },
			{ 254, 24, 0 }, { 254, 22, 0 }, { 254, 20, 0 }, { 254, 18, 0 },
			{ 254, 16, 0 }, { 254, 14, 0 }, { 254, 12, 0 }, { 254, 10, 0 },
			{ 254, 8, 0 }, { 254, 6, 0 }, { 254, 4, 0 }, { 254, 2, 0 },
			{ 254, 0, 0 } };

	//Runnables
	
	private Runnable Timer_Tick = new Runnable() 
	{
		public void run() 
		{
			sendMessage((byte) (99 & 0xFF) );
		}	
	};

	private Runnable updateAcc = new Runnable() 
	{
		private double ms;
		
		public void run() 
		{
			//acc = sHandler.getGrav();
			//Log.e(TAG,String.valueOf(acc[0]) + " " + String.valueOf(acc[1]) + " " + String.valueOf(acc[2]));
			ms = Math.pow(sHandler.getGrav()[0],2)+Math.pow(sHandler.getGrav()[1],2)+Math.pow(sHandler.getGrav()[2],2);
			colorstore[0] = (int) (curBright*Math.pow(sHandler.getGrav()[0],2)/ms);
			colorstore[1] = (int) (curBright*Math.pow(sHandler.getGrav()[1],2)/ms);
			colorstore[2] = (int) (curBright*Math.pow(sHandler.getGrav()[2],2)/ms);
			color.setValue(colorstore,IntValueStore.SOURCE_LOOP);
		}	
	};
	
	private Runnable updateCandle = new Runnable() 
	{
		public void run() 
		{
			colorstore[0] = (int) Math.ceil(candlecol[0]*candle_handle.getBright());
			colorstore[1] = (int) Math.ceil(candlecol[1]*candle_handle.getBright());
			colorstore[2] = (int) Math.ceil(candlecol[2]*candle_handle.getBright());
			color.setValue(colorstore,IntValueStore.SOURCE_LOOP);
		}
		
	};

	private Runnable applyFFT = new Runnable() 
	{
		double[][] amp_window = new double[AMPSMOOTH][3];
		int[] freq_window = new int[FREQSMOOTH];
		int posf = 0;
		int posa = 0;
		double mean = 0;
		int j = 0;
		//long in, fi;
		
		public void run() {
			if(posf==FREQSMOOTH) posf=0;
			if(posa==AMPSMOOTH) posa=0;
			//in = System.currentTimeMillis();
			freq_window[posf]=fft_handle.getMaxBetween(curMin,curMax);
			freq = freq_mean();
			if (curRegTp)
			{
				amp_window[posa][0]=fft_handle.getAmp()*50;
				amplitude = amp_mean(0);
				if(amplitude>1) amplitude = 1;
				colorstore[0] = (int)(amplitude*colorTable[freq][0]);
				colorstore[1] = (int)(amplitude*colorTable[freq][1]);
				colorstore[2] = (int)(amplitude*colorTable[freq][2]);
				color.setValue(colorstore,IntValueStore.SOURCE_LOOP);
			}
			else
			{
				amp_window[posa][0]=fft_handle.getMeanAmpBetween(100, 512)*25000;
				amp_window[posa][1]=fft_handle.getMeanAmpBetween(100, 150)*20000;
				amp_window[posa][2]=fft_handle.getMeanAmpBetween(0, 50)*15000;
				colorstore[0] = (int)amp_mean(0)>255 ? 255 : (int)amp_mean(0);
				colorstore[1] = (int)amp_mean(1)>255 ? 255 : (int)amp_mean(1);
				colorstore[2] = (int)amp_mean(2)>255 ? 255 : (int)amp_mean(2);
				color.setValue(colorstore,IntValueStore.SOURCE_LOOP);
			}
			posa++;
			posf++;
			/*try {
				Thread.sleep(40);
			} catch (InterruptedException e) {
				Log.e(TAG, "sleep", e);
			}*/
			//fi = System.currentTimeMillis();
			//Log.i(TAG, "Time for update" + String.valueOf(fi-in));
		}

		private int freq_mean()
		{
			mean = 0;
			for(j=0;j<FREQSMOOTH;j++)
			{
				mean+=freq_window[j];
			}
			return (int) Math.ceil(mean/FREQSMOOTH);
		}

		private double amp_mean(int pos)
		{
			mean = 0;
			for(j=0;j<AMPSMOOTH;j++)
			{
				mean+=amp_window[j][pos];
			}
			return mean/AMPSMOOTH;
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);


		setContentView(R.layout.activity_main);
		
		context = (Context) this;

		cp = (ColorPicker) findViewById(R.id.Colorpicker);

		status_text = (TextView)findViewById(R.id.Status);
		
		red_bar = (SeekBar)findViewById(R.id.seekBar_red); // make seekbar object
		green_bar = (SeekBar)findViewById(R.id.seekBar_green); // make seekbar object
		blue_bar = (SeekBar)findViewById(R.id.seekBar_blue); // make seekbar object

		color_disp = findViewById(R.id.colordisp);

		handle = new Handler();		
		
		color = new IntValueStore(new int [3]);

		color.setListener(this);
		
		
		red_listener = new ListenerAndWatcher(0,color);
		green_listener = new ListenerAndWatcher(1,color);
		blue_listener = new ListenerAndWatcher(2,color);
				
		sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
		//startSettings();

		dialog = new ProgressDialog((Context) this);
		dialog.setCancelable(false);
		dialog.setCanceledOnTouchOutside(false);
		
		sHandler = new SensorHandler((Context) this);
		
		BTService = new BlueToothService(this,BTHandle);

		BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if (mBluetoothAdapter == null) {
			Toast.makeText(this, 
					"Bluetooth is not available.", 
					Toast.LENGTH_LONG).show();
			finish();
			return;
		}

		if (!mBluetoothAdapter.isEnabled()) 
		{
		    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
		    this.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
		    enablingBt = true;
		}


	}

	@Override
	public void onStart() {
		super.onStart();
	}

	@Override
	public void onResume() {
		super.onResume();
		Log.e(TAG,"+++ ON RESUME +++");

		if (BTService != null && enablingBt == false) {
	    	// Only if the state is STATE_NONE, do we know that we haven't started already
	    	if (BTService.getState() == BlueToothService.STATE_NONE && connectingBt == false) {
	    		// Start the Bluetooth chat services
	    		BTService.start();
				BluetoothDevice device = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(curMacAdd);
				if(device!=null) BTService.connect(device);
	    		
	    	}
	    }
		
		handle.removeCallbacks(Timer_Tick);
		handle.removeCallbacks(updateAcc);
		handle.removeCallbacks(applyFFT);
		handle.removeCallbacks(updateCandle);
		sHandler.unregisterSensors();
		if(fft_handle != null) fft_handle.stop();
		fft_handle = null;
		if(candle_handle != null) candle_handle.stop();
		candle_handle = null;
		
		String dmy = curMacAdd;
		curMacAdd = sharedPref.getString("mac_add", "00:12:12:04:06:29");
		if(dmy!=curMacAdd && BlueToothService.STATE_CONNECTED == BTService.getState())
		{
			Log.e(TAG,"address changed");
			BTService.stop();
			BTService.start();
			BluetoothDevice device = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(curMacAdd);
			if(device!=null) BTService.connect(device);
			else Toast.makeText(this, "Invalid device address, please correct",Toast.LENGTH_LONG).show();
		}
		
		//Get Settings from Settings Activity
		curRegMode = Integer.valueOf(sharedPref.getString("reg_list", "0"));
		curBright = Integer.valueOf(sharedPref.getString("acc_bright","100"));
		curSlpEn = sharedPref.getBoolean("sleep_checkbox",false);
		curSlpDelay = Integer.valueOf(sharedPref.getString("sleep_delay","100"));
		curMin = Integer.valueOf(sharedPref.getString("min_sel","0"));
		curMax = Integer.valueOf(sharedPref.getString("max_sel","512"));
		curRegTp = sharedPref.getBoolean("sw_reg_tp",false);
		curCandleOff = Integer.valueOf(sharedPref.getString("candle_offset", "32"));
		curCandleFl = Integer.valueOf(sharedPref.getString("candle_flicker", "48"));
		
		//check values for validity!
		if(curBright<0) 
		{
			curBright=0;
			sharedPref.edit().putString("acc_bright", "0");
		}
		else if(curBright>255)
		{
			curBright=255;
			sharedPref.edit().putString("acc_bright", "255");
		}
		
		if(curMin<0 || curMin>511) 
		{
			curMin = 0;
			sharedPref.edit().putString("min_sel", "0").commit();
		}
		
		if(curMax<0 || curMax>512)
		{
			curMax = 512;
			sharedPref.edit().putString("max_sel", "512").commit();
		}
		
		if(curCandleOff > 255 || curCandleOff < 0)
		{
			curCandleOff = 255;
			sharedPref.edit().putString("candle_offset", "255").commit();
		}
		
		if(curCandleFl > 255-curCandleOff || curCandleFl < 0)
		{
			curCandleFl = 255-curCandleOff;
			sharedPref.edit().putString("candle_flicker", String.valueOf(curCandleFl)).commit();
		}
		
		//if bluetooth is not running stop execution
		if(enablingBt) return;
		
		//Start regulation according to regmode
		if(REGULTYPE_NOREGULATION==curRegMode)
		{
			enableInput(true);
		}
		else if(REGULTYPE_ACC==curRegMode)
		{
			enableInput(false);
			sHandler.registerSensors();
			Log.e(TAG,"regultype acc");
			handle.postDelayed(updateAcc, 100);
		}
		else if(REGULTYPE_FOURIER==curRegMode)
		{
			fft_handle = new VisualizerTask(NFFT);
			enableInput(false);
			fft_handle.execute();
			handle.postDelayed(applyFFT, 100);
		}
		else if(REGULTYPE_CANDLE==curRegMode)
		{
			updateCandleCol();
			candle_handle = new CandleTask();
			enableInput(false);
			cp.setOnTouchListener(new ColorPickerListener(color));
			candle_handle.execute();
			candle_handle.setOffset(curCandleOff);
			candle_handle.setFlicker(curCandleFl);
			handle.postDelayed(updateCandle,100);
		}
		else
		{
			enableInput(false);
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		Log.e(TAG,"+++ ON PAUSE +++");
		
		if(enablingBt) return;
	}

	@Override
	public void onStop() {
		super.onStop();

	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		color.setValue(new int[3],IntValueStore.SOURCE_LOOP);
		sendMessage((byte) (99 & 0xFF) );
		connectingBt = false;
		BTService.stop();
		handle.removeCallbacks(Timer_Tick);
		if(REGULTYPE_ACC==curRegMode)
		{
			sHandler.unregisterSensors();
			handle.removeCallbacks(updateAcc);
		}
		else if(REGULTYPE_FOURIER==curRegMode)
		{
			fft_handle.stop();
			handle.removeCallbacks(applyFFT);
			fft_handle = null;
		}
		else if(REGULTYPE_CANDLE==curRegMode)
		{
			candle_handle.stop();
			handle.removeCallbacks(updateCandle);
			candle_handle = null;
		}
	}

	private void enableInput(boolean flag)
	{
		if(!flag)
		{
			red_bar.setOnSeekBarChangeListener(null); 
			green_bar.setOnSeekBarChangeListener(null); 
			blue_bar.setOnSeekBarChangeListener(null); 

			cp.setOnTouchListener(null);

			red_bar.setEnabled(false);
			green_bar.setEnabled(false);
			blue_bar.setEnabled(false);

		}
		else
		{
			red_bar.setOnSeekBarChangeListener(red_listener); 
			green_bar.setOnSeekBarChangeListener(green_listener); 
			blue_bar.setOnSeekBarChangeListener(blue_listener); 

			cp.setOnTouchListener(new ColorPickerListener(color));

			red_bar.setEnabled(true);
			green_bar.setEnabled(true);
			blue_bar.setEnabled(true);

		}
	}

	public void sendMessage(byte cmd)
	{
		sendBT[0] = cmd;
		colorstore = color.getValue();
		//Log.e(TAG,String.valueOf(colorstore[0]) + String.valueOf(colorstore[1]) + String.valueOf(colorstore[2]));
		sendBT[1] = (byte) colorstore[0];
		sendBT[2] = (byte) colorstore[1];
		sendBT[3] = (byte) colorstore[2];
		if (BTService.getState() == BlueToothService.STATE_CONNECTED) 
		{
			BTService.write(sendBT);
		}
	}

	@Override
	public void onValueChanged(int[] newValue, int src)
	{
		if(REGULTYPE_CANDLE==curRegMode && IntValueStore.SOURCE_GUI == src)
		{
			updateCandleCol();
			handle.removeCallbacks(Timer_Tick);		
			Log.e(TAG,"if");
		}
		else
		{
			red_bar.setProgress(newValue[0]);
			green_bar.setProgress(newValue[1]);
			blue_bar.setProgress(newValue[2]);
			handle.removeCallbacks(Timer_Tick);
			handle.postDelayed(Timer_Tick,1);
		}
		color_disp.setBackgroundColor((255<<24) + (newValue[0]<<16) + (newValue[1]<<8) + (newValue[2]));
		if(REGULTYPE_ACC==curRegMode) 
		{
			handle.removeCallbacks(updateAcc);
			handle.postDelayed(updateAcc, 10);
		}
		else if(REGULTYPE_FOURIER==curRegMode) 
		{
			handle.removeCallbacks(applyFFT);
			handle.postDelayed(applyFFT, 20);
		}
		else if(REGULTYPE_CANDLE==curRegMode) 
		{
			handle.removeCallbacks(updateCandle);
			handle.postDelayed(updateCandle, 10);
		}
	}
	
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_main, menu);
        return true;
    }
    
    public void updateCandleCol()
    {
    	colorstore = color.getValue();
		candlecol[0] = colorstore[0]/color.getMax();
		candlecol[1] = colorstore[1]/color.getMax();
		candlecol[2] = colorstore[2]/color.getMax();
    }
    
	public void startSettings(View v)
	{
		Intent prefsIntent = new Intent(this,SettingsActivity.class);
		startActivity(prefsIntent);
	}
	
	public void showProgressDialog(boolean flag)
	{
		if(flag) dialog.show();
		else dialog.dismiss();
	}

	public void showDialog(String txt)
	{
		if (!this.isFinishing())
		{
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setCancelable(false);
			builder.setMessage(txt).setPositiveButton("Yes", dialogClickListener).setNegativeButton("No", dialogClickListener).show();
		}
	}
	
	public void onActivityResult (int requestCode, int resultCode, Intent data)
	{
		if(REQUEST_ENABLE_BT == requestCode)
		{
			if(Activity.RESULT_OK == resultCode)
			{
				BluetoothDevice device = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(curMacAdd);
				BTService.start();
				if(device!=null) BTService.connect(device);
				enablingBt = false;
			}
			else
			{
				Toast.makeText(this, 
						"Bluetooth could not be enabled!", 
						Toast.LENGTH_LONG).show();
				finish();
				return;
			}
		}
	}
}
