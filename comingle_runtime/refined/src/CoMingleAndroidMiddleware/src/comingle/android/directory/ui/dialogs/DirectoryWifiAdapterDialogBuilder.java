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
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.provider.Settings;

import comingle.comms.directory.BaseDirectory;
import comingle.comms.misc.Barrier;

/**
 * 
 * Builder of a Dialog box that request user to turn on wifi adapters.
 * 
 * @author Edmund S.L. Lam
 *
 * @param <D> Type of administrative data handled by the directory.
 */
public class DirectoryWifiAdapterDialogBuilder<D extends Serializable> extends DirectoryDialogBuilder<D> {

	public static final int DEFAULT_ACT_REQ_CODE = 1023;
	
	protected String title;
	protected String msg;
	protected int actRetCode;
	
	public DirectoryWifiAdapterDialogBuilder(Activity activity, BaseDirectory<D> directory, String title, String msg, int actRetCode) {
		super(activity, directory);
		this.title = title;
		this.msg = msg;
		this.actRetCode = actRetCode;
	}
	
	public DirectoryWifiAdapterDialogBuilder(Activity activity, BaseDirectory<D> directory) {
		this(activity, directory, "Check Wifi Settings", "Please turn on Wifi-adapters", DEFAULT_ACT_REQ_CODE);
	}

	@Override
	public Builder getDialogBuilder(final Barrier barrier) {
		AlertDialog.Builder alert = new AlertDialog.Builder(activity);
		alert.setTitle(title);
		alert.setMessage(msg);
		alert.setPositiveButton("Settings", new OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				activity.startActivityForResult(new Intent(Settings.ACTION_WIFI_SETTINGS), actRetCode);
			    releaseBarrier(barrier);
			}
		});
		alert.setNegativeButton("Close", new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				activity.finish();
			}
		});
		return alert;
	}
	
}
