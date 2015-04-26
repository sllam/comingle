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

package comingle.android.directory.wifidirect;

import java.util.LinkedList;
import java.util.List;

import comingle.android.directory.ui.dialogsequences.WifiDirectSetupDialogSequence;
import comingle.comms.directory.NodeInfo;
import comingle.comms.message.Accept;
import comingle.comms.message.JoinRequest;
import comingle.comms.message.Message;
import comingle.comms.message.Ping;
import comingle.comms.message.Pong;
import comingle.comms.message.Quit;
import comingle.comms.message.Reject;
import comingle.comms.message.Service;
import comingle.comms.message.Update;

import android.app.Activity;
import android.content.Context;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener;
import android.net.wifi.p2p.WifiP2pManager.GroupInfoListener;
import android.net.wifi.p2p.WifiP2pManager.PeerListListener;

/**
 * 
 * An instance of a base directory that establishes and maintains a Wifi-Direct based directory.
 * Connection is established via standard Wifi-Direct protocol and APIs. Once a Wifi-Direct group is
 * established, group owner's directory maintains the directory service and broadcasts IP address of
 * all participants of the directory.
 * 
 * @author Edmund S.L. Lam
 *
 */
public class WifiDirectDirectory extends SocketDataManager implements PeerListListener, GroupInfoListener, ConnectionInfoListener {
	
	private final static String TAG = "WifiDirectDirectory";
	
	public final static String OWNER_IP = "192.168.49.1";
	public final static int WIFI_DIRECT_NETWORK_ID = 122;
	
	protected final IntentFilter intentFilter = new IntentFilter();
	protected Channel mChannel;
	protected WifiP2pManager mManager;
	protected WifiDirectBroadcastReceiver mReceiver;
	
	protected WifiP2pDevice thisDevice;
	
	/**
	 * Basic constructor
	 * @param activity the activity this directory is embedded in
	 * @param admin_port the admin port number
	 * @param req_code the request code for this service
	 * @param owner_ip IP address of the owner
	 */
	public WifiDirectDirectory(Activity activity, int port, String reqCode, String ownerIP, String localDeviceID) {
		super(activity, port, reqCode, WIFI_DIRECT_NETWORK_ID, localDeviceID);
		setOwnerIP(ownerIP);
	}
	
	/**
	 * Constructor with default owner IP: 192.168.49.1
	 * @param activity the activity this directory is embedded in
	 * @param admin_port the admin port number
	 * @param req_code the request code for this service
	 */
	public WifiDirectDirectory(Activity activity, int port, String reqCode, String localDeviceID) {
		this(activity, port, reqCode, OWNER_IP, localDeviceID);
	}
		
	
	/**
	 * Close all operations of the directory. Specifically, an owner will
	 * notify all members of this closure, while a member will notify the
	 * owner. This is followed by the termination of all active threads of
	 * this directory, namely administrative data pipes and connection loops.
	 */
	public void close() {
		if (isOwner()) { 
			this.broadcastMessage(new Quit(local));
		} else {
			this.sendDataThreaded(new Quit(local), this.ownerIP);
		}
		super.close();
	}

	
	/**
	 * Client (Non-group owner) Routine, tries to establish a connection with the
	 * group owner. 
	 * @param tries number of tries.
	 * @param time_out time in milliseconds till next try.
	 * @return true if connection is successfully established
	 */
	public boolean connect(int tries, int time_out) {
		log(String.format("Attempting to connect to %s, for %s times, %s ms apart.", this.ownerIP, tries, time_out));
		int count = 0;
		if(tries < 0) { tries = 10000; }
		JoinRequest msg = new JoinRequest(this.reqCode, getDisplayName(), getDeviceID()); 
		while(count <= tries && !isAccepted() && !isRejected() && !isClosing()) {
			// Log.v(TAG, "calling send message...");
			this.sendData(msg, this.ownerIP);
			this.sleep(time_out);
			count++;
		}
		if(isRejected() || isClosing()) { return false; }
		else { return isAccepted(); }
	}
	
	////////////////////
	// Common Methods //
	////////////////////
	
	/**
	 * Set the device of this directory to this device.
	 * @param device the device to set.
	 */
	public void setDevice(WifiP2pDevice device) {
		thisDevice = device;
	}

	
	//////////////////////////////
	// AdminDataManager Methods //
	//////////////////////////////
	
	/**
	 * Process a join message. Does the following if this directory is an owner (otherwise, nothing) and the join request code
	 * matches this directory:
	 *    i.   Create a new NodeInfo with device info in join message, assigns this node a unique location id.
	 *    ii.  Add new node to existing list of peers.
	 *    iii. Send an accept message to the source of this join message.
	 *    iv.  Broadcast new list of peers to all other member directories subscribed to this directory.
	 *    v.   Invokes the operations of DirectoryChangedListeners, ConnectionEstablishedListeners.
	 * If request code does not match, reply with reject message.
	 */
	@Override
	public void processMessage(JoinRequest join) {
		if( !this.isOwner() ) { return; }
		if( this.reqCode.equals( join.req_code ) ) {
			if(!hasNode(join.ipAddress)) {
				// NodeInfo new_peer = newNodeInfo(join.ipAddress, join.device_name, join.device_mac, this.showRole(MEMBER_ROLE));
				NodeInfo new_peer = this.newNodeInfo(join.ipAddress, join.device_name, join.device_id, this.showRole(MEMBER_ROLE));
				peers.add(new_peer);
				List<NodeInfo> added_nodes = new LinkedList<NodeInfo>();
				added_nodes.add(new_peer);
				this.doDirectoryChangedActions(added_nodes, new LinkedList<NodeInfo>());
				this.doConnectionEstablishedActions(new_peer);
				this.sendData(new Accept(new_peer), join.ipAddress);
				this.broadcastMessage(new Update(this.getNodeArray()));
			} else {
				// Received more join requests from the same ip address,
				// Currently, it will just resend the accept message.
				// TODO: Compare old and new peer data
				NodeInfo peer = getNode(join.ipAddress);
				this.sendData(new Accept(peer), join.ipAddress);
			}
		} else {
			this.sendData(new Reject(Reject.REQ_CODE_MISMATCH), join.ipAddress);
		}
	}

	@Override
	public void processMessage(Accept accept) {
		if( !this.isMember() ) { return; } 
		this.local = accept.peer;
		this.setAccepted();
		this.dirConnected = true;
		this.doLocalNodeInfoAvailableActions();
		this.doConnectionEstablishedActions(accept.peer);
	}

	@Override
	public void processMessage(Reject reject) {
		if( !this.isMember() ) { return; }
		this.setRejected();
	}

	@Override
	public void processMessage(Update update) {
		if( !this.isMember() ) { return; }
		List<NodeInfo> added_nodes   = NodeInfo.addedDiff(update.entries, peers);
		List<NodeInfo> dropped_nodes = NodeInfo.droppedDiff(update.entries, peers);		
		this.updateNodes(update.entries);
		this.doDirectoryChangedActions(added_nodes, dropped_nodes);		
	}

	@Override
	public void processMessage(Quit quit) {
		this.removeNode( quit.node );
		List<NodeInfo> dropped_nodes = new LinkedList<NodeInfo>();
		dropped_nodes.add( quit.node );
		this.doDirectoryChangedActions(new LinkedList<NodeInfo>(), dropped_nodes);
		if(this.isOwner()) {
			this.broadcastMessage( new Update(this.getNodeArray()) );
		} else if (this.isMember()) {
			this.dirConnected = false;
			doOwnerTerminationActions( quit.node );
		}
	}

	@Override
	public void processMessage(Ping ping) {
		// TODO Auto-generated method stub

	}

	@Override
	public void processMessage(Pong pong) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onPeersAvailable(WifiP2pDeviceList peerlist) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onConnectionInfoAvailable(WifiP2pInfo info) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onGroupInfoAvailable(WifiP2pGroup group) {
		if(group == null) { return; }
		if(group.isGroupOwner()) { 
			setOwnerRole();
		} else {
			setMemberRole();
		}
		/*
		if(role_not_established && thisDevice != null) {
			if(isOwner()) {
				// local = newNodeInfo(ownerIP, thisDevice.deviceName, thisDevice.deviceAddress, this.showRole(OWNER_ROLE));
				local = this.localNodeInfo(this.getNextLoc(), ownerIP, this.showRole(OWNER_ROLE));
				peers.add(local);
				this.dirConnected = true;
				this.doLocalNodeInfoAvailableActions();
			}
			this.doRoleEstablishedActions();
			role_not_established = false;
		} */
		establishRole();
	}
	
	@Override
	public void serve() {
		
	}

	@Override
	public void processMessage(Service service) {
		// TODO Auto-generated method stub
		
	}
	
	////////////////////////////////
	// Broadcast Receiver Methods //
    ////////////////////////////////
	
	
	@Override
	public boolean isWifiEnabled() {
		ConnectivityManager connectivityManager = (ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo mWifi = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		return mWifi.isConnected();
	}
	/*
	@Override
	public boolean isConnected() {
		ConnectivityManager connectivityManager = (ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo mWifi = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		return mWifi.isConnectedOrConnecting();
	} */
	
	protected boolean broadcastReceiverInited = false;
	
	@Override
	public void initNetworkNotifications() {
		if (!broadcastReceiverInited) {
			// Initialization stuff for P2P WiFi
			intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
			// Indicates a change in the list of available peers.
			intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
			// Indicates the state of Wi-Fi P2P connectivity has changed.
			intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
			// Indicates this device's details have changed.
			intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

			mManager = (WifiP2pManager) activity.getSystemService(Context.WIFI_P2P_SERVICE);
			mChannel = mManager.initialize(activity, activity.getMainLooper(), null);
		
			broadcastReceiverInited = true;
		}
	}
	
	@Override
	public void resumeNetworkNotifications() {
		if (mReceiver == null && broadcastReceiverInited) {
			mReceiver = new WifiDirectBroadcastReceiver(mManager, mChannel, activity, this);
			activity.registerReceiver(mReceiver, intentFilter);			
		}
	}
	
	@Override
	public void pauseNetworkNotifications() {
		if (mReceiver != null && broadcastReceiverInited) {
			activity.unregisterReceiver(mReceiver);
			mReceiver = null;
		}
	} 
	
	@Override
	public void displayDirectorySetupDialogs(int peer_list_row, int peer_name, int peer_loc, int peer_ip) {
		(new WifiDirectSetupDialogSequence<Message>(activity, this, peer_list_row, peer_name, peer_loc, peer_ip)).start();
	}
	
	
}
