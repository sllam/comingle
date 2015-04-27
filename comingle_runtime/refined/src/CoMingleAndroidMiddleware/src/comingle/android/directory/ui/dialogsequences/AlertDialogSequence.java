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
import java.util.LinkedList;
import java.util.List;

import android.app.Activity;

import comingle.android.directory.ui.dialogs.DirectoryDialogBuilder;
import comingle.comms.misc.Barrier;

/**
 * 
 * This abstract class defines daemon thread routines that maintain a sequence of dialog
 * boxes (created by DirectoryDialogBuilder) that prompts user for input parameters that
 * customizes an instance of a BaseDirectory.
 * 
 * @author Edmund S.L. Lam
 *
 * @param <D> Type of administrative data handled by the directory.
 */
public class AlertDialogSequence<D extends Serializable> extends Thread {

	protected final Activity activity;
	protected final Barrier barrier;
	protected final List<DirectoryDialogBuilder<D>> dialogs;
	
	/**
	 * Basic Constructor
	 * @param activity the activity that embeds the dialog boxes created by this sequence.
	 */
	public AlertDialogSequence(Activity activity) {
		this.activity = activity;
		this.barrier = new Barrier();
		this.dialogs = new LinkedList<DirectoryDialogBuilder<D>>();
	}
	
	/**
	 * Main thread routine: Displays a list of dialog boxes prompting user input.
	 */
	@Override
	public void run() {
		for(final DirectoryDialogBuilder<D> dialog: dialogs) {
			runDialog(dialog);
		}	
	}
	
	/**
	 * Given a DirectoryDialogBuilder, create and show its dialog in the
	 * current activity, then wait until dialog box releases the synchronization barrier.
	 * @param dialog
	 */
	protected void runDialog(final DirectoryDialogBuilder<D> dialog) {
		activity.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				dialog.getDialog(barrier).show();
			}
		});
		barrier.await();
		barrier.reset();
	}
	
	/**
	 * Add a dialog builder to the default list of builders
	 * @param dialog the dialog builder to add.
	 */
	public void addDialog(DirectoryDialogBuilder<D> dialog) {
		dialogs.add(dialog);
	}

	/**
	 * Add a list of dialog builders to the default list of builders
	 * @param ds the list of dialog builders to add.
	 */
	public void addDialogs(List<DirectoryDialogBuilder<D>> ds) {
		dialogs.addAll(ds);
	}
	
	/**
	 * Add an array of dialog builders to the default list of builders
	 * @param ds the array of dialog builders to add.
	 */
	public void addDialogs(DirectoryDialogBuilder<D>[] ds) {
		for(DirectoryDialogBuilder<D> d: ds) {
			addDialog(d);
		}
	}
	
}
