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

package comingle.android.directory.ui.dialogs;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import comingle.android.directory.lan.AndroidLanDirectory;
import comingle.android.directory.wifidirect.WifiDirectDirectory;
import comingle.comms.directory.BaseDirectory;
import comingle.comms.message.Message;
import comingle.comms.misc.Barrier;

public class DirectoryChoiceDialogBuilder extends DirectoryDialogBuilder<Message> {

	public static final int LAN_CHOSEN = 2;
	public static final int WIFI_DIRECT_CHOSEN = 1;	
	public static final int DEBUG_LAN_CHOSEN = 0;
	
	protected int adminPort;
	protected String defaultReqCode;
	protected String localDeviceId;
	protected int chosen;
	
	public DirectoryChoiceDialogBuilder(Activity activity, int adminPort, String defaultReqCode, String localDeviceId) {
		super(activity, null);
		this.adminPort = adminPort;
		this.defaultReqCode = defaultReqCode;
		this.localDeviceId  = localDeviceId;
		this.chosen = -1;
	}

	@Override
	public Builder getDialogBuilder(final Barrier barrier) {
		final CharSequence[] items = {"LAN (Debug)","Wifi-Direct","Local Area Network"};
		final SingleChoice choice = new SingleChoice(DEBUG_LAN_CHOSEN);
		
		AlertDialog.Builder alert = new AlertDialog.Builder(activity);

		alert.setTitle("Choose Connection Type");
		
		alert.setSingleChoiceItems(items, DEBUG_LAN_CHOSEN, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int item) {
				choice.setChoice(item);
			}
		});
		
		alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			  public void onClick(DialogInterface dialog, int whichButton) {
				  switch(choice.getChoice()) {
				  	case DEBUG_LAN_CHOSEN:
				  		directory = new AndroidLanDirectory(activity, adminPort, defaultReqCode, localDeviceId);
				  		break;
				    case LAN_CHOSEN: 
				    	directory = new AndroidLanDirectory(activity, adminPort, localDeviceId);
				        break;
				    case WIFI_DIRECT_CHOSEN: 
				    	directory = new WifiDirectDirectory(activity, adminPort, defaultReqCode, localDeviceId);
				        break;
				  }
				  chosen = choice.getChoice();
				  releaseBarrier(barrier);
			  }
			});
		
		return alert;
	}
	
	public int getChoice() {
		return chosen;
	}

	public BaseDirectory<Message> getDirectory() {
		return directory;
	}
	
}
