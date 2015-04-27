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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.DialogInterface;
import android.text.InputFilter;
import android.widget.EditText;

import comingle.comms.directory.BaseDirectory;
import comingle.comms.misc.Barrier;

/**
 * 
 * Builder of a Dialog box that prompts user for request code input for the directory.
 * 
 * @author Edmund S.L. Lam
 *
 * @param <D> Type of administrative data handled by the directory.
 */
public class DirectoryReqCodeDialogBuilder<D extends Serializable> extends DirectoryDialogBuilder<D> {

	public DirectoryReqCodeDialogBuilder(Activity activity, BaseDirectory<D> directory) {
		super(activity, directory);
	}

	@Override
	public Builder getDialogBuilder(final Barrier barrier) {
		AlertDialog.Builder alert = new AlertDialog.Builder(activity);

		alert.setTitle("Connect to a Game");
		alert.setMessage("Session Code:");

		final EditText input = new EditText(activity);
		input.setFilters(new InputFilter[] {new InputFilter.AllCaps()});
		alert.setView(input);

		alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
		public void onClick(DialogInterface dialog, int whichButton) {
		    String reqCode = input.getText().toString();
		    directory.setReqCode(reqCode);
		    releaseBarrier(barrier);
		  }
		});
		
		return alert;
	}

	@Override
	public Dialog getDialog(Barrier barrier) {
		return getDialogBuilder(barrier).create();
	}

	
	
}
