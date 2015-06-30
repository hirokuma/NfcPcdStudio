package com.blogpost.hiro99ma.pcd;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbManager;
import android.util.Log;

import com.blogpost.hiro99ma.pcd.NfcPcd.RecvBroadcast;

public class UsbHost {
	private static final String TAG = "NfcPcdUsbHost";
	private UsbListener mListener = null;
	
	public interface UsbListener {
		public void inserted();
		public void removed();
	}
	
    public boolean onCreate(Activity act) {
    	boolean ret = false;
        UsbManager mgr = (UsbManager)act.getSystemService(Context.USB_SERVICE);

        // initialize PCD
        IntentFilter filter = NfcPcd.init(act, mgr);
        if(filter != null) {
        	act.registerReceiver(mUsbReceiver, filter);
        	mListener = (UsbListener)act;
        	ret = true;
        }
        
        return ret;
    }

    public void onDestroy(Context context) {
    	if(NfcPcd.opened()) {
    		context.unregisterReceiver(mUsbReceiver);
    		NfcPcd.destroy();
    	}
    }

    BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
        	Log.d(TAG, "onReceive : " + intent.getAction());
        	NfcPcd.RecvBroadcast ret = RecvBroadcast.UNKNOWN;
        	synchronized (this) {
        		ret = NfcPcd.receiveBroadcast(context, intent);
        		if((ret == RecvBroadcast.PERMIT) || (ret == RecvBroadcast.ATTACHED)) {
                    /* device inserted */
                    if(mListener != null) {
                    	mListener.inserted();
                    }
        		} else {
                    /* device removed */
                    if(mListener != null) {
                    	mListener.removed();
                    }
        		}
        	}
        }
    };
}
