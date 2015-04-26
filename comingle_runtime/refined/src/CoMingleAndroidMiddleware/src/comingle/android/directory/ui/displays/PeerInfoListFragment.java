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

package comingle.android.directory.ui.displays;

import java.io.Serializable;
import java.util.List;

import comingle.comms.directory.BaseDirectory;
import comingle.comms.directory.NodeInfo;
import comingle.comms.listeners.DirectoryChangedListener;
import comingle.comms.listeners.LocalNodeInfoAvailableListener;

import android.support.v4.app.ListFragment;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

/**
 * 
 * A simple list fragment instance that displays contains of a directory
 *  
 * @author Edmund S.L. Lam
 *
 * @param <D>
 */
public class PeerInfoListFragment<D extends Serializable> extends ListFragment {

	class PeerInfoArrayAdapter extends ArrayAdapter<NodeInfo> {
		
		private int row_res_id;
		private int name_res_id;
		private int loc_res_id;
		private int ip_addr_res_id;
		private int did_res_id;
		private int role_res_id;
		private List<NodeInfo> peers;
		
		public PeerInfoArrayAdapter(Context context, int row_res_id, int name_res_id, int loc_res_id
				                   ,int ip_addr_res_id, int did_res_id, int role_res_id, List<NodeInfo> peers) {
			super(context, row_res_id, peers);
			this.row_res_id     = row_res_id;
			this.name_res_id    = name_res_id;
			this.loc_res_id     = loc_res_id;
			this.ip_addr_res_id = ip_addr_res_id;
			this.did_res_id     = did_res_id;
			this.role_res_id    = role_res_id;
			this.peers = peers;
		}	
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View v = convertView;
			if (v == null) {
				LayoutInflater vi = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				v = vi.inflate(row_res_id, null);
			}
			NodeInfo selected_peer = peers.get(position);
			
			if (selected_peer != null) {
				TextView peer_name = (TextView) v.findViewById(name_res_id);
				TextView peer_loc  = (TextView) v.findViewById(loc_res_id);
				TextView peer_ip   = (TextView) v.findViewById(ip_addr_res_id);
				TextView peer_did  = (TextView) v.findViewById(did_res_id);
				TextView peer_role = (TextView) v.findViewById(role_res_id);
				if (peer_name != null) { peer_name.setText(selected_peer.displayName); }
				if (peer_loc  != null) { peer_loc.setText(String.format("%s", selected_peer.location) ); }
				if (peer_ip   != null) { peer_ip.setText(selected_peer.ipAddress); }
				if (peer_did  != null) { peer_did.setText(selected_peer.deviceID); }
				if (peer_role != null) { peer_role.setText(selected_peer.role); } 
			}
			
			return v;
		}
		
	}
	
	private int list_res_id;
	private int row_res_id;
	private int name_res_id;
	private int loc_res_id;
	private int ip_addr_res_id;
	private int did_res_id;
	private int role_res_id;
	private int my_name_res_id;
	private int my_loc_res_id;
	private int my_ip_addr_res_id;
	private int my_did_res_id;
	private int my_role_res_id;
	private BaseDirectory<D> wifiDir;
	private View mContentView = null;
	
	private PeerInfoArrayAdapter adapter = null;
	
	private PeerInfoItemClickListener listener = null;
	
	public PeerInfoListFragment(int list_res_id, int row_res_id, int name_res_id
			                   ,int loc_res_id, int ip_addr_res_id, int did_res_id, int role_res_id
			                   ,int my_name_res_id, int my_loc_res_id, int my_ip_addr_res_id
			                   ,int my_did_res_id, int my_role_res_id, BaseDirectory<D> wifiDir) { 
		this.list_res_id    = list_res_id;
		this.row_res_id     = row_res_id;
		this.name_res_id    = name_res_id;
		this.loc_res_id     = loc_res_id;
		this.ip_addr_res_id = ip_addr_res_id;
		this.did_res_id     = did_res_id;
		this.role_res_id    = role_res_id;
		this.my_name_res_id    = my_name_res_id;
		this.my_loc_res_id     = my_loc_res_id;
		this.my_ip_addr_res_id = my_ip_addr_res_id;
		this.my_did_res_id     = my_did_res_id;
		this.my_role_res_id    = my_role_res_id;		
		this.wifiDir          = wifiDir;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		adapter = new PeerInfoArrayAdapter(getActivity(), row_res_id, name_res_id
				                          ,loc_res_id, ip_addr_res_id, did_res_id, role_res_id, wifiDir.getNodes());
		setListAdapter( adapter );
		
		
		LocalNodeInfoAvailableListener node_listener = new LocalNodeInfoAvailableListener() {
			@Override
			public void doLocalNodeInfoAvailableAction(NodeInfo local, int role) {
				refreshMyDevice();	
			}
		};
		wifiDir.addLocalNodeInfoAvailableListener(node_listener);
		
		DirectoryChangedListener dir_listener = new DirectoryChangedListener() {
			@Override
			public void doDirectoryChangedAction(List<NodeInfo> peers, List<NodeInfo> added
					                            ,List<NodeInfo> dropped, int role) {
				refreshPeers();	
			}
		};
		wifiDir.addDirectoryChangedListener(dir_listener);
		
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		mContentView = inflater.inflate(list_res_id, null);
		return mContentView;
	}	
	
	private void refreshMyDeviceInt() {
		final NodeInfo my_device = wifiDir.getLocalNode();
		TextView view = (TextView) mContentView.findViewById(this.my_name_res_id);
		view.setText(my_device.displayName);
		view = (TextView) mContentView.findViewById(this.my_loc_res_id);
		view.setText( String.format("%s", my_device.location) );

		view = (TextView) mContentView.findViewById(this.my_ip_addr_res_id);
		view.setText( my_device.ipAddress );
		
		view = (TextView) mContentView.findViewById(this.my_did_res_id);
		view.setText( my_device.deviceID );	
		
		view = (TextView) mContentView.findViewById(this.my_role_res_id);
		view.setText( my_device.role );	
	}
	
	public void refreshMyDevice() {
		final PeerInfoListFragment<D> self = this;
		getActivity().runOnUiThread( new Runnable() {
			@Override
			public void run() {
				self.refreshMyDeviceInt();
			}
		});
	}
	
	public void setPeerInfoItemClick(PeerInfoItemClickListener listener) {
		this.listener = listener;
	}
	
	@Override
	public void onListItemClick(ListView lv, View v, int position, long id) {
		if (listener != null) {
			NodeInfo nodeInfo = wifiDir.getNodes().get(position);
			listener.onPeerInfoItemClick(nodeInfo);
		}
	}
	
	public void refreshPeers() {
		final PeerInfoArrayAdapter apt = adapter;
		getActivity().runOnUiThread( new Runnable() {
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
