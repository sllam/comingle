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

import java.util.List;

import comingle.comms.directory.BaseDirectory;
import comingle.comms.message.Message;
import comingle.comms.message.MessageHandler;
import comingle.comms.sockets.SocketDataPipe;

import android.app.Activity;
import android.util.Log;
import android.widget.Toast;


public abstract class SocketDataManager extends BaseDirectory<Message> implements MessageHandler {
	
	protected static String TAG = "SocketDataManager";
	
	protected boolean terminate;
	protected Activity activity;
	
	public SocketDataManager(Activity activity, int port, String reqCode, int networkID, String localDeviceID) {
		super(new SocketDataPipe<Message>(port), reqCode, networkID, localDeviceID);
		this.activity = activity;
		log("SocketDataManager constructor called");
		this.terminate  = false;
	}
	
	@Override
	protected void receiveData(List<Message> msg_list, String ipAddr) {
		for(Message msg: msg_list) {
			log(String.format("%s received from %s", msg, ipAddr));
			msg.setIPAddress(ipAddr);
			msg.enter(this);
		}
	}
	
	@Override
	protected void handleReceiveException(final Exception e) {
		activity.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(activity, "Error when receiving data: " + e.toString(), Toast.LENGTH_SHORT).show();	
			}
		});
	}
	
	public synchronized void terminate() {
		this.terminate = true;
	}
	
	protected void runOnUIThread(Runnable action) {
		activity.runOnUiThread(action);
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
	
}
