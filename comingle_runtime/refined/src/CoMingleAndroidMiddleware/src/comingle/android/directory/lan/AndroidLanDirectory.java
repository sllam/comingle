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
import android.content.Context;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.util.Log;
import comingle.android.directory.ui.dialogsequences.LanSetupDialogSequence;
import comingle.comms.directory.lan.LanDirectory;
import comingle.comms.message.Message;

/**
 * 
 * A specialized instance of the LanDirectory for the Android SDK.
 * 
 * @author Edmund S.L. Lam
 *
 */
public class AndroidLanDirectory extends LanDirectory {

	public static final String TAG = "AndroidLanDirectory";
	
	protected final Activity act;
	
	protected final IntentFilter intentFilter = new IntentFilter();
	protected Channel mChannel;
	protected WifiManager mManager;
	protected LanWifiBroadcastReceiver mReceiver;
	
	public AndroidLanDirectory(final Activity act, String multicastAddr, int multicastPort, int p2pPort, String reqCode, String localDeviceID) {
		super(multicastAddr, multicastPort, p2pPort, reqCode, localDeviceID);
		this.act = act;
		/*
		final AndroidLanDirectory self = this;
		this.addNetworkStatusChangedListener(new NetworkStatusChangedListener() {
			@Override
			public void doWifiAdapterStatusChangedAction(boolean enabled) {
				if (!enabled) {
					runOnUIThread(new Runnable() {
						@Override
						public void run() {
							(new DirectoryWifiAdapterAlertDialog( act, self )).getDialog().show();
						}
					});
				}
			}

			@Override
			public void doWifiConnectionStatusChangedAction(boolean connected) {
					
			}
		}); */
	}
	
	public AndroidLanDirectory(Activity act, int p2pPort, String reqCode, String localDeviceID) {
		super(p2pPort, reqCode, localDeviceID);
		this.act = act;
	}
	
	public AndroidLanDirectory(Activity act, int p2pPort, String localDeviceID) {
		super(p2pPort, localDeviceID);
		this.act = act;
	}

	protected void runOnUIThread(Runnable runnable) {
		if(act != null) {
			act.runOnUiThread(runnable);
		}
	}
	
	@Override
	public void log(final String msg) {
		runOnUIThread(new Runnable() {
			@Override
			public void run() {
				Log.v(TAG, msg);
			}
		});
	}

	@Override
	public void err(final String msg) {
		runOnUIThread(new Runnable() {
			@Override
			public void run() {
				Log.e(TAG, msg);
			}
		});
	}

	@Override
	public void info(final String msg) {
		runOnUIThread(new Runnable() {
			@Override
			public void run() {
				Log.i(TAG, msg);
			}
		});
	}

	@Override
	public boolean isWifiEnabled() {
		ConnectivityManager connectivityManager = (ConnectivityManager) act.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo mWifi = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		return mWifi.isConnected();
	}
	
	@Override
	public boolean isConnected() {
		ConnectivityManager connectivityManager = (ConnectivityManager) act.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo mWifi = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		return mWifi.isConnected();
	}
	
	@Override
	public void displayDirectorySetupDialogs(int peer_list_row, int peer_name, int peer_loc, int peer_ip) {
		(new LanSetupDialogSequence<Message>(act, this, peer_list_row, peer_name, peer_loc, peer_ip)).start();
	}
	
	/*
	protected boolean broadcastReceiverInited = false;
	
	@Override
	public void initNetworkNotifications() {
		if (!broadcastReceiverInited) {
			intentFilter.addAction( WifiManager.WIFI_STATE_CHANGED_ACTION );
			mManager = (WifiManager) act.getSystemService(Context.WIFI_SERVICE);
			broadcastReceiverInited = true;
		}
	}
	
	@Override
	public void resumeNetworkNotifications() {
		if (mReceiver == null && broadcastReceiverInited) {
			mReceiver = new LanWifiBroadcastReceiver(mManager, act, this);
			act.registerReceiver(mReceiver, intentFilter);			
		}
		
	}
	
	@Override
	public void pauseNetworkNotifications() {
		if (mReceiver != null && broadcastReceiverInited) {
			act.unregisterReceiver(mReceiver);
			mReceiver = null;
		}
	}
	*/
	
}
