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

import java.io.Serializable;

import comingle.android.directory.ui.dialogs.DirectoryDisplayNameDialogBuilder;
import comingle.android.directory.ui.dialogs.DirectoryPeerListDialogBuilder;
import comingle.android.directory.ui.dialogs.DirectoryWifiAdapterDialogBuilder;
import comingle.comms.directory.BaseDirectory;

import android.app.Activity;

public class WifiDirectSetupDialogSequence<D extends Serializable> extends AlertDialogSequence<D> {

	protected final BaseDirectory<D> directory;

	protected int row_res_id;
	protected int name_res_id;
	protected int loc_res_id;
	protected int ip_addr_res_id;
	
	public WifiDirectSetupDialogSequence(Activity activity, BaseDirectory<D> directory, int row_res_id, 
            int name_res_id, int loc_res_id, int ip_addr_res_id) {
		super(activity);
		this.directory = directory;
		dialogs.add(new DirectoryDisplayNameDialogBuilder<D>(activity, directory));
		this.row_res_id  = row_res_id;
		this.name_res_id = name_res_id;
		this.loc_res_id  = loc_res_id;
		this.ip_addr_res_id = ip_addr_res_id;
	}

	@Override
	public void run() {
		if(!directory.isWifiEnabled()) {
			runDialog( new DirectoryWifiAdapterDialogBuilder<D>( activity, directory ) );
		} else if(!directory.isWifiConnected()) {
			runDialog( new DirectoryWifiAdapterDialogBuilder<D>(activity, directory, "No Wifi-direct group", "Please form a Wifi-direct group"
					                                           ,DirectoryWifiAdapterDialogBuilder.DEFAULT_ACT_REQ_CODE) );
		} else {
			runDialog( new DirectoryDisplayNameDialogBuilder<D>( activity, directory ) );
			directory.establishRole();
			runDialog( new DirectoryPeerListDialogBuilder<D>(activity, directory, row_res_id, name_res_id, loc_res_id, 
		      		                                       ip_addr_res_id, null) ); 
		}
	}
	
}
