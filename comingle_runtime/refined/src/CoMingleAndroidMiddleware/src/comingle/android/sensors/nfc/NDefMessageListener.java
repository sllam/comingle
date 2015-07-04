package comingle.android.sensors.nfc;

import android.nfc.NdefMessage;

abstract public class NDefMessageListener {
	abstract public void onReceiveMessage(NdefMessage msg);
}
