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

package comingle.android.directory.wifidirect;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.util.Log;

public class WifiDirectBroadcastReceiver extends BroadcastReceiver {

	private static String TAG = "WifiDirectBroadcastReceiver";
	
	private WifiP2pManager manager;
	private Channel channel;
	private Activity activity;
	private boolean is_enabled;
	
	private WifiDirectDirectory wifiDir;
	
	/**
	 * @param manager WifiP2pManager system service
	 * @param channel Wifi p2p channel
	 * @param activity activity associated with the receiver
	*/
	public WifiDirectBroadcastReceiver(WifiP2pManager manager, Channel channel, Activity activity, WifiDirectDirectory wifiDir) {
		super();
		this.manager = manager;
		this.channel = channel;
		this.activity = activity;
		this.wifiDir = wifiDir;
	}
	
	public void setIsWifiP2pEnabled(boolean is_enabled) {
		this.is_enabled = is_enabled;
		wifiDir.setIsWifiEnabled(is_enabled);
	}
	
	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();
		if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {

			// UI update to indicate wifi p2p status.
			int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
			if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
				setIsWifiP2pEnabled(true);
				Log.v(TAG, "Wifi P2p is Enabled.");
				wifiDir.doWifiAdapterStatusChanged(true);
			} else {
				setIsWifiP2pEnabled(false);
				Log.v(TAG, "Wifi P2p is Disabled.");
				wifiDir.doWifiAdapterStatusChanged(false);
			}

		} else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {

			// request available peers from the wifi p2p manager. This is an
			// asynchronous call and the calling activity is notified with a
			// callback on PeerListListener.onPeersAvailable()
			if (manager != null) {
				// manager.requestGroupInfo(channel, p2p_dir);
				manager.requestPeers(channel, wifiDir);
			}

		} else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {

			if (manager == null) { return; }

			NetworkInfo networkInfo = (NetworkInfo) intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);

			if (networkInfo.isConnected()) {
				Log.v(TAG, "Connection occurred.");
				// we are connected with the other device, request connection
				// info to find group owner IP
				manager.requestConnectionInfo(channel, wifiDir);
				manager.requestGroupInfo(channel, wifiDir);
				wifiDir.doWifiConnectionStatusChanged(true);
			} else {
				Log.v(TAG, "Disconnection occurred.");
				wifiDir.doWifiConnectionStatusChanged(false);
			}

		} else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {

			if (manager == null) { return; }
			WifiP2pDevice device = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
			if (device != null) {
				wifiDir.setDevice(device);
				manager.requestGroupInfo(channel, wifiDir);
				Log.v(TAG, "Device changed.");
			}
		}
		
	}

}
