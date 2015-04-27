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
import comingle.android.directory.ui.dialogs.DirectoryReqCodeDialogBuilder;
import comingle.android.directory.ui.dialogs.DirectoryRoleDialogBuilder;
import comingle.android.directory.ui.dialogs.DirectoryWifiAdapterDialogBuilder;
import comingle.android.directory.ui.displays.PeerInfoItemClickListener;
import comingle.comms.directory.BaseDirectory;
import comingle.comms.misc.RandGenerator;

import android.app.Activity;

/**
 * 
 * An instance of the AlertDialogSequence, that prompts for user to setup a LANDirectory.
 * 
 * @author Edmund S.L. Lam
 *
 * @param <D> Type of administrative data handled by the directory.
 */
public class LanSetupDialogSequence<D extends Serializable> extends AlertDialogSequence<D> {

	protected final boolean promptReqCode;
	protected final BaseDirectory<D> directory;
	
	protected int row_res_id;
	protected int name_res_id;
	protected int loc_res_id;
	protected int ip_addr_res_id;
	
	/**
	 * Basic Constructor
	 * @param activity the activity that embeds the dialog boxes.
	 * @param directory the directory to setup.
	 */
	public LanSetupDialogSequence(Activity activity, BaseDirectory<D> directory) {
		super(activity);
		this.directory = directory;
		dialogs.add(new DirectoryDisplayNameDialogBuilder<D>( activity, directory ));
		dialogs.add(new DirectoryRoleDialogBuilder<D>( activity, directory )); 
		this.promptReqCode = false;
	}
	
	/**
	 * Standard constructor, that prompts for request codes and displays current directory nodes when
	 * setup is completed.
	 * @param activity activity that embeds the dialog boxes.
	 * @param directory the directory to setup.
	 * @param row_res_id Resource ID of row view for each node entry.
	 * @param name_res_id Resource ID of name field for a node.
	 * @param loc_res_id Resource ID of location field for a node.
	 * @param ip_addr_res_id Resource ID of address field for a node.
	 */
	public LanSetupDialogSequence(Activity activity, BaseDirectory<D> directory, int row_res_id, 
            int name_res_id, int loc_res_id, int ip_addr_res_id) {
		super(activity);
		this.directory = directory;
		this.promptReqCode = true;
		this.row_res_id  = row_res_id;
		this.name_res_id = name_res_id;
		this.loc_res_id  = loc_res_id;
		this.ip_addr_res_id = ip_addr_res_id;
	}

	/**
	 * Run LAN setup dialog sequence. Run customized sequence (see runSeqWithGeneratedReqCode) if request code prompt is omitted.
	 */
	@Override
	public void run() {
		if (!promptReqCode) {
			super.run();
		} else {
			runSeqWithGeneratedReqCode();
		}
	}
	
	/**
	 * Run custom LAN setup sequence:
	 *   i. If wifi-adapters are not enabled, run wifi-adapter dialog prompt. Otherwise run the following
	 *   ii. Run display name prompt dialog box
	 *   iii. Run role choice prompt dialog box
	 *   iv. If member chosen at iii., run request code prompt dialog prompt. Otherwise, randomly generate a request code.
	 *   v. Run peer list display dialog box.
	 */
	protected void runSeqWithGeneratedReqCode() {
		if( !directory.isWifiEnabled() ) {
			runDialog( new DirectoryWifiAdapterDialogBuilder<D>( activity, directory ) );
		} else {
			runDialog( new DirectoryDisplayNameDialogBuilder<D>( activity, directory ) );
			runDialog( new DirectoryRoleDialogBuilder<D>( activity, directory ) );
			if( directory.isMember() ) {
				runDialog( new DirectoryReqCodeDialogBuilder<D>( activity, directory ) );
			} else {
				directory.setReqCode( RandGenerator.randReqCode() );
			}
			directory.establishRole();
			runDialog( new DirectoryPeerListDialogBuilder<D>(activity, directory, row_res_id, name_res_id, loc_res_id, 
		      		                                       ip_addr_res_id, null) ); 
		}
	}
	
}
