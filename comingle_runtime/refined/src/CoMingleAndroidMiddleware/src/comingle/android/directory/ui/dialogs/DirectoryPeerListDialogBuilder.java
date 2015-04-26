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
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import comingle.android.directory.ui.displays.PeerInfoItemClickListener;
import comingle.comms.directory.BaseDirectory;
import comingle.comms.directory.NodeInfo;
import comingle.comms.listeners.DirectoryChangedListener;
import comingle.comms.misc.Barrier;

public class DirectoryPeerListDialogBuilder<D extends Serializable> extends DirectoryDialogBuilder<D> {

	class PeerInfoArrayAdapter extends ArrayAdapter<NodeInfo> {
		
		private int row_res_id;
		private int name_res_id;
		private int loc_res_id;
		private int ip_addr_res_id;
		private List<NodeInfo> peers;
		
		public PeerInfoArrayAdapter(Context context, int row_res_id, int name_res_id, int loc_res_id
				                   ,int ip_addr_res_id, List<NodeInfo> peers) {
			super(context, row_res_id, peers);
			this.row_res_id     = row_res_id;
			this.name_res_id    = name_res_id;
			this.loc_res_id     = loc_res_id;
			this.ip_addr_res_id = ip_addr_res_id;
			this.peers = peers;
		}	
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View v = convertView;
			if (v == null) {
				LayoutInflater vi = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				v = vi.inflate(row_res_id, null);
			}
			NodeInfo selected_peer = peers.get(position);
			
			if (selected_peer != null) {
				TextView peer_name = (TextView) v.findViewById(name_res_id);
				TextView peer_loc  = (TextView) v.findViewById(loc_res_id);
				TextView peer_ip   = (TextView) v.findViewById(ip_addr_res_id);
				if (peer_name != null) { peer_name.setText(selected_peer.displayName); }
				if (peer_loc  != null) { peer_loc.setText(String.format("%s", selected_peer.location) ); }
				if (peer_ip   != null) { peer_ip.setText(selected_peer.ipAddress); }
			}
			
			return v;
		}
		
	}
	
	protected int row_res_id;
	protected int name_res_id;
	protected int loc_res_id;
	protected int ip_addr_res_id;
	protected PeerInfoItemClickListener listener;
	protected PeerInfoArrayAdapter adapter;
	
	public DirectoryPeerListDialogBuilder(Activity activity, BaseDirectory<D> directory, int row_res_id, 
			                            int name_res_id, int loc_res_id, int ip_addr_res_id, PeerInfoItemClickListener listener) {
		super(activity, directory);
		this.row_res_id     = row_res_id;
		this.name_res_id    = name_res_id;
		this.loc_res_id     = loc_res_id;
		this.ip_addr_res_id = ip_addr_res_id;
		this.listener = listener;
	}

	@Override
	public Builder getDialogBuilder(final Barrier barrier) {
		
		AlertDialog.Builder alert = new AlertDialog.Builder(activity);
		
		adapter = new PeerInfoArrayAdapter(activity, row_res_id, name_res_id
				                          ,loc_res_id, ip_addr_res_id, directory.getNodes());
		
		alert.setTitle("Session Code: " + directory.getReqCode());
		
		alert.setAdapter(adapter, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				if(listener != null) {
					NodeInfo node = directory.getNodes().get(which);
					listener.onPeerInfoItemClick(node);
				}
			}
		});
		
		alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
		public void onClick(DialogInterface dialog, int whichButton) {
			adapter = null;
		    releaseBarrier(barrier);
		  }
		});
		
		final DirectoryChangedListener dir_listener = new DirectoryChangedListener() {
			@Override
			public void doDirectoryChangedAction(List<NodeInfo> peers, List<NodeInfo> added
					                            ,List<NodeInfo> dropped, int role) {
				refreshPeers();	
			}
		};
		directory.addDirectoryChangedListener(dir_listener);
		
		alert.setOnDismissListener(new OnDismissListener() {
			@Override
			public void onDismiss(DialogInterface dialog) {
				directory.removeDirectoryChangedListener(dir_listener);
			}
		});
		
		return alert;
		
	}

	@Override
	public Dialog getDialog(Barrier barrier) {
		return getDialogBuilder(barrier).create();
	}
	
	public void refreshPeers() {
		if (adapter == null) { return; } 
		final PeerInfoArrayAdapter apt = adapter;
		activity.runOnUiThread( new Runnable() {
			@Override
			public void run() {
				if(apt != null) {
					apt.notifyDataSetChanged();
					apt.notifyDataSetInvalidated();	
				}
			}	
		});
	}
	
}
