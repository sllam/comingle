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

package comingle.android.directory.ui.dialogsequences;


import java.util.LinkedList;
import java.util.List;

import comingle.android.directory.ui.dialogs.DirectoryChoiceDialogBuilder;
import comingle.android.directory.wifidirect.WifiDirectDirectory;
import comingle.comms.directory.BaseDirectory;
import comingle.comms.directory.lan.LanDirectory;
import comingle.comms.message.Message;

import android.app.Activity;

public class DirectoryChoiceDialogSequence extends AlertDialogSequence<Message> {

	protected int adminPort;
	protected String defaultReqCode;
	protected String localDeviceId;
	
	protected int row_res_id;
	protected int name_res_id;
	protected int loc_res_id;
	protected int ip_addr_res_id;
	
	protected AlertDialogSequence<Message> directorySetupSeq = null;
	
	protected List<DirectoryChosenListener<Message>> listeners = new LinkedList<DirectoryChosenListener<Message>>();
	protected boolean directoryChosen = false;
	BaseDirectory<Message> directory;
	protected int directoryId = -1;
	
	public DirectoryChoiceDialogSequence(Activity activity, int adminPort, String defaultReqCode, String localDeviceId,
			int row_res_id, int name_res_id, int loc_res_id, int ip_addr_res_id) {
		super(activity);
		this.adminPort = adminPort;
		this.defaultReqCode = defaultReqCode;
		this.localDeviceId  = localDeviceId;

		this.row_res_id  = row_res_id;
		this.name_res_id = name_res_id;
		this.loc_res_id  = loc_res_id;
		this.ip_addr_res_id = ip_addr_res_id;
	}
	
	public boolean directoryChosen() {
		return directoryChosen;
	}
	
	public AlertDialogSequence<Message> getDirectorySetupSequence() {
		switch(directoryId) {
			case LanDirectory.LAN_NETWORK_ID:
				return new LanSetupDialogSequence<Message>(activity, directory, row_res_id, 
                        name_res_id, loc_res_id, ip_addr_res_id);
			case WifiDirectDirectory.WIFI_DIRECT_NETWORK_ID:
				return new WifiDirectSetupDialogSequence<Message>(activity, directory, row_res_id, 
                        name_res_id, loc_res_id, ip_addr_res_id);
		}
		return null;
	}

	public void addDirectoryChosenListener(DirectoryChosenListener<Message> l) {
		listeners.add(l);
	}

	public void removeDirectoryChosenListener(DirectoryChosenListener<Message> l) {
		listeners.remove(l);
	}
	
	protected void doDirectoryChosenActions(BaseDirectory<Message> directory) {
		for(DirectoryChosenListener<Message> l: listeners) {
			l.doDirectoryChosenAction(directory);
		}
	}
	
	@Override
	public void run() {
		DirectoryChoiceDialogBuilder builder = new DirectoryChoiceDialogBuilder(
				                                      activity, adminPort, defaultReqCode, localDeviceId);
		runDialog(builder);
		directory = builder.getDirectory();
		directoryChosen = true;
		doDirectoryChosenActions(directory);
		switch(builder.getChoice()) {
			case DirectoryChoiceDialogBuilder.LAN_CHOSEN:
				directoryId = LanDirectory.LAN_NETWORK_ID;
				directorySetupSeq = new LanSetupDialogSequence<Message>(activity, directory, row_res_id, 
			                                     name_res_id, loc_res_id, ip_addr_res_id);
				break;
			case DirectoryChoiceDialogBuilder.WIFI_DIRECT_CHOSEN:
				directoryId = WifiDirectDirectory.WIFI_DIRECT_NETWORK_ID;
				directorySetupSeq = new WifiDirectSetupDialogSequence<Message>(activity, directory, row_res_id, 
			                                     name_res_id, loc_res_id, ip_addr_res_id);
				break;
		}
		directorySetupSeq.run();
	}
	
}
