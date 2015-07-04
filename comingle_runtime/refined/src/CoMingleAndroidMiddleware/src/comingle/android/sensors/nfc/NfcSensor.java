package comingle.android.sensors.nfc;

import java.nio.charset.Charset;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentFilter.MalformedMimeTypeException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcAdapter.CreateNdefMessageCallback;
import android.nfc.NfcEvent;
import android.os.Parcelable;
import android.util.Log;

public class NfcSensor implements CreateNdefMessageCallback {

	public final static String TAG = "NFC-SENSOR";
	
	protected String mimeType = "application/com.example.android.beam";
	
	protected Activity activity;
	protected NfcAdapter mNfcAdapter;
    protected PendingIntent mPendingIntent;
    protected IntentFilter[] mFilters;
	
    protected NDefMessageCreator  creator  = null;
    protected NDefMessageListener listener = null;
    
    // protected int count = 0;
    
	public NfcSensor(Activity activity) {
		this.activity = activity;
	}

	public NfcSensor(Activity activity, String mimeType) {
		this.activity = activity;
		this.mimeType = mimeType;
	}
	
	public NfcSensor(Activity activity, NDefMessageListener listener) {
		this(activity);
		this.setNDefMessageListener(listener);
	}
	
	public void setNDefMessageListener(NDefMessageListener listener) {
		this.listener = listener;
	}
	
	public void setNDefMessageCreator(NDefMessageCreator creator) {
		this.creator = creator;
	}
	
	public void setPendingIntent(PendingIntent pt) {
		mPendingIntent = pt;
	}
	
	public boolean init() {
        mNfcAdapter = NfcAdapter.getDefaultAdapter(activity);
		if(mNfcAdapter != null) {
			
	        mPendingIntent = PendingIntent.getActivity(activity, 0,
	                new Intent(activity, activity.getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
	        
	        IntentFilter ndef = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);
	        try {
	            ndef.addDataType(mimeType); //"*/*");
	        } catch (MalformedMimeTypeException e) {
	            throw new RuntimeException("fail", e);
	        }
	        mFilters = new IntentFilter[] {
	                ndef,
	        };
			
	        mNfcAdapter.setNdefPushMessageCallback(this, activity);
	        
	        Log.i(TAG, "NFC Adapter linked and initialized.");
			return true;
		} else {
	        Log.i(TAG, "NFC not available.");
			return false;
		}
	}

	@Override
	public NdefMessage createNdefMessage(NfcEvent arg0) {
		/*
		String str = String.format("%s", count);
    	count++; */
		String str = "Ping";
		if(creator != null) {
			str = creator.onCreateNDefMessage();
		}
        NdefMessage msg = new NdefMessage(
                new NdefRecord[] { createMimeRecord(
                        mimeType, str.getBytes())
          //,NdefRecord.createApplicationRecord("com.example.android.beam")
        });
        Log.i(TAG, "NDef Message created.");
        return msg;
	}
	
    public NdefRecord createMimeRecord(String mimeType, byte[] payload) {
        byte[] mimeBytes = mimeType.getBytes(Charset.forName("US-ASCII"));
        NdefRecord mimeRecord = new NdefRecord(
                NdefRecord.TNF_MIME_MEDIA, mimeBytes, new byte[0], payload);
        return mimeRecord;
    }
    
    public void resumeSensorNotifications() {
    	mNfcAdapter.enableForegroundDispatch(activity, mPendingIntent, mFilters, null);
    	Log.i(TAG, "NFC foreground stuff enabled.");
    	
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(activity.getIntent().getAction())) {
            processIntent(activity.getIntent());
        }
    	
    }
    
    public void processIntent(Intent intent) {
        if (!NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction())) {
        	Log.i(TAG, "Non-compatible intent detected.");
        	return;
        }
        Parcelable[] rawMsgs = intent.getParcelableArrayExtra(
                NfcAdapter.EXTRA_NDEF_MESSAGES);
        // only one message sent during the beam
        NdefMessage msg = (NdefMessage) rawMsgs[0];

        Log.i(TAG, String.format("Received NFC Data: %s", msg.toString()));
        
        if(listener != null) {
        	listener.onReceiveMessage(msg);
        }
        
    }
	
}
