/*
This file is part of CoMingle.

CoMingle is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

CoMingle is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with CoMingle. If not, see <http://www.gnu.org/licenses/>.

CoMingle Version 1.5, Prototype Alpha

Authors:
Edmund S. L. Lam      sllam@qatar.cmu.edu

* This implementation was made possible by an JSREP grant (JSREP 4-003-2-001, Effective Distributed 
Programming via Join Patterns with Guards, Propagation and More) from the Qatar National Research Fund 
(a member of the Qatar Foundation). The statements made herein are solely the responsibility of the authors.
*/

package comingle.android.directory.lan;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.util.Log;

/**
 * 
 * Broadcast receiver instance for the LAN Directory
 * 
 * @author Edmund S.L. Lam
 *
 */
public class LanWifiBroadcastReceiver extends BroadcastReceiver {

	private static String TAG = "LanWifiBroadcastReceiver";
	
	private WifiManager manager;
	private Activity activity;
	private boolean is_enabled;
	
	private AndroidLanDirectory lanDir;
	
	/**
	 * @param manager WifiP2pManager system service
	 * @param channel Wifi p2p channel
	 * @param activity activity associated with the receiver
	*/
	public LanWifiBroadcastReceiver(WifiManager manager, Activity activity, AndroidLanDirectory wifiDir) {
		super();
		this.manager = manager;
		this.activity = activity;
		this.lanDir = wifiDir;
	}
	
	public void setIsWifiP2pEnabled(boolean is_enabled) {
		this.is_enabled = is_enabled;
		lanDir.setIsWifiEnabled(is_enabled);
	}
	
	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();
		if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(action)) {

			// UI update to indicate wifi p2p status.
			int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
			if (state == WifiManager.WIFI_STATE_ENABLED) {
				setIsWifiP2pEnabled(true);
				Log.v(TAG, "Wifi P2p is Enabled.");
				lanDir.doWifiAdapterStatusChanged(true);
			} else {
				setIsWifiP2pEnabled(false);
				Log.v(TAG, "Wifi P2p is Disabled.");
				lanDir.doWifiAdapterStatusChanged(false);
			}

		} 		
	}

}
