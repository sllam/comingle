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

import java.io.Serializable;

import comingle.comms.directory.BaseDirectory;
import comingle.comms.misc.Barrier;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;

/**
 * 
 * This abstract class defines the family of dialog box builders for the directory.
 * The intend of these dialog boxes is to prompt for user input for parameters of
 * the Android application's BaseDirectory. Dialog boxes created by the dialog builder
 * contains a simple synchronization mechanism to allow sequencing of such dialogs.
 * 
 * @author Edmund S.L. Lam
 *
 * @param <D> Type of administrative data handled by the directory.
 */
public abstract class DirectoryDialogBuilder<D extends Serializable> {

	protected final Activity activity;
	protected BaseDirectory<D> directory;
	
	/**
	 * Basic Constructor
	 * @param activity the activity that embeds the dialogs
	 * @param directory the directory to be modified
	 */
	public DirectoryDialogBuilder(Activity activity, BaseDirectory<D> directory) {
		this.activity  = activity;
		this.directory = directory;
	}
	
	/**
	 * Retrieve an instance of the dialog box builder. Dialog box created by this builder
	 * to release given synchronization barrier, when exited.
	 * @param barrier the synchronization barrier to release.
	 * @return an instance of the dialog box builder.
	 */
	abstract public AlertDialog.Builder getDialogBuilder(Barrier barrier);
	
	/**
	 * Retrieve an instance of the dialog box builder. 
	 * @return an instance of the dialog box builder.
	 */
	public AlertDialog.Builder getDialogBuilder() {
		return getDialogBuilder(null);
	}
	
	/**
	 * Retrieve an instance of the dialog box. Dialog box is expected
	 * to release given synchronization barrier, when exited.
	 * @param barrier the synchronization barrier to release.
	 * @return an instance of the dialog box.
	 */
	public Dialog getDialog(Barrier barrier) {
		return getDialogBuilder(barrier).create();
	}

	/**
	 * Retrieve an instance of the dialog box. 
	 * @return an instance of the dialog box.
	 */
	public Dialog getDialog() {
		return getDialog(null);
	}
	
	/**
	 * Release the given synchronization barrier.
	 * @param barrier the synchronization barrier to release.
	 */
	protected void releaseBarrier(Barrier barrier) {
		if(barrier != null) {
			barrier.release();
		}
	}
	
}
