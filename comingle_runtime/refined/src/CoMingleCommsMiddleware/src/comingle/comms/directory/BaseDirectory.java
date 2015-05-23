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

package comingle.comms.directory;

import java.io.Serializable;
import java.net.InetAddress;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import comingle.comms.datapipe.DataPipe;
import comingle.comms.datapipe.DataPipeManager;
import comingle.comms.listeners.ConnectionEstablishedListener;
import comingle.comms.listeners.DirectoryChangedListener;
import comingle.comms.listeners.ListeningPost;
import comingle.comms.listeners.LocalNodeInfoAvailableListener;
import comingle.comms.listeners.NetworkStatusChangedListener;
import comingle.comms.listeners.OwnerTerminationListener;
import comingle.comms.listeners.RoleEstablishedListener;
import comingle.comms.log.Logger;
import comingle.comms.neighborhood.Neighborhood;


/**
 * Basic Directory class. Maintains a routing table that contain mappings from unique location identifiers to IP addresses
 * and other relevant device information (Name, address, ID, etc..). Each participating instance of this directory communicates
 * with one instance, acting as the owner of the group. Events that changes the directory (new member connected, a member
 * leaving, etc..) are broadcasted by the owner. Applications running this directory can subscribe to such events through
 * the various listener types found in comingle.comms.listener . This listener functionalities are inherited from the 
 * ListeningPost class.
 * 
 * @author Edmund S.L. Lam
 * 
 * @param <D> the class of the administrative messages sent between directory instances. Must be serializable.
 */
public abstract class BaseDirectory<D extends Serializable> extends ListeningPost<D,String> {

	private final static String TAG = "BaseDirectory";
	
	public final static int OWNER_ROLE = 0;
	public final static int MEMBER_ROLE = 1;
	public final static int UNKNOWN_ROLE = -1;
	
	public final static int OWNER_LOC = 0;
	
	protected int locCount = 0;
	
	protected final List<NodeInfo> peers;

	protected String localDisplayName;
	protected NodeInfo local;
	protected NodeInfo owner;
	protected String ownerIP;
	protected int role;
	protected String reqCode;
	protected int networkID;
	protected String localDeviceID;
	
	protected boolean wifiEnabled   = false;
	protected boolean wifiConnected = false;
	protected boolean dirConnected = false;
	protected boolean localDisplayNameAvailable = false;
	
	protected boolean useDeviceIDAsLocationID = false;
	
	/**
	 * Basic Constructor of the directory
	 * @param pipe the data pipe that exchanges administrative data, to be embedded in this base directory.
	 * @param reqCode the request code that is required to join this directory.
	 * @param networkID the unique identifier of the network used by this directory.
	 * @param localDisplayName the local display name of the directory.
	 * @param localDeviceID the local device ID, used to uniquely identify the application which this directory is part of.
	 * @param useDeviceIDAsLocationID true if device ID should be used as location ID. Otherwise, owner location will be assign '0' 
	 * with members uniquely assigned increments of 1.
	 */
	public BaseDirectory(DataPipe<D,String> pipe, String reqCode, int networkID, String localDisplayName, String localDeviceID, 
			             boolean useDeviceIDAsLocationID) {
		super( pipe );
		this.peers = Collections.synchronizedList( new LinkedList<NodeInfo>() );
		this.reqCode = reqCode;
		this.role = UNKNOWN_ROLE;
		this.local = null;
		this.owner = null;
		this.ownerIP = null;
		this.networkID = networkID;
		this.localDisplayName = localDisplayName;
		this.localDeviceID = localDeviceID;
		this.useDeviceIDAsLocationID = useDeviceIDAsLocationID;
	}
	
	/**
	 * Alternative constructor, that initializes with local display name 'null'.
	 * @param pipe the data pipe that exchanges administrative data, to be embedded in this base directory.
	 * @param reqCode the request code that is required to join this directory.
	 * @param networkID the unique identifier of the network used by this directory.
	 * @param localDeviceID the local device ID, used to uniquely identify the application which this directory is part of.
	 */
	public BaseDirectory(DataPipe<D,String> pipe, String reqCode, int networkID, String localDeviceID) {	
		this(pipe, reqCode, networkID, null, localDeviceID, false);
	}
	
	/**
	 * Alternative constructor, that initializes with request code and local display name 'null'.
	 * @param pipe the data pipe that exchanges administrative data, to be embedded in this base directory.
	 * @param networkID the unique identifier of the network used by this directory.
	 * @param localDeviceID the local device ID, used to uniquely identify the application which this directory is part of.
	 */
	public BaseDirectory(DataPipe<D,String> pipe, int networkID, String localDeviceID) {
		this(pipe, null, networkID, null, localDeviceID, false);
	}
		
	////////////////////////////////
	// Identity Operations        //
	////////////////////////////////
	
	/**
	 * Returns the network ID of this directory
	 * @return the network ID of this directory
	 */
	public int getNetworkID() {
		return networkID;
	}
	
	/**
	 * Returns the unique identify of the application that this directory is part of.
	 * @return the unique identify of the application that this directory is part of.
	 */
	public String getDeviceID() {
		return localDeviceID;
	}
	
	/**
	 * Returns the display name of this directory.
	 * @return the display name of this directory.
	 */
	public String getDisplayName() {
		return localDisplayName;
	}
	
	/**
	 * Returns the request code of this directory.
	 * @return the request code of this directory.
	 */
	public String getReqCode() {
		return reqCode;
	}
	
	/**
	 * Sets this directory to use Hash Code Identity.
	 * @param set true if directory is to use hash code identity.
	 */
	public void setUseHashCodeIdentity(boolean set) {
		useDeviceIDAsLocationID = set;
	}
	
	/**
	 * Set request code to the given String argument
	 * @param reqCode the request code
	 */
	public void setReqCode(String reqCode) {
		this.reqCode = reqCode;
		// establishRole();
	}
	
	/**
	 * Set display name to the given String argument
	 * @param name the display name
	 */
	public void setDisplayName(String name) {
		localDisplayName = name;
		localDisplayNameAvailable = true;
		// establishRole();
	}
	
	protected boolean role_not_established = true;
	
	/**
	 * Try to establish the role of this directory. In order to do so, the following criteria must be fullfilled:
	 *    i.   Role not established earlier.
	 *    ii.  Display name is not null.
	 *    iii. Request Code is not null.
	 *    iv.  Directory has determined that it is either an owner or member.
	 * @return true if role has been successfully established.
	 */
	public boolean establishRole() {
		if(role_not_established && localDisplayName != null && reqCode != null && (isOwner() || isMember())) {
			if(isOwner() && ownerIP != null) {
				// local = newNodeInfo(ownerIP, thisDevice.deviceName, thisDevice.deviceAddress, this.showRole(OWNER_ROLE));
				// local = this.localNodeInfo(this.getNextLoc(), ownerIP, this.showRole(OWNER_ROLE));
				local = this.localNodeInfo(getIdentity( localDeviceID ), ownerIP, this.showRole(OWNER_ROLE));
				peers.add(local);
				this.dirConnected = true;
				this.doLocalNodeInfoAvailableActions();
				this.doRoleEstablishedActions();
				role_not_established = false;
				log("Owner Role Established.");
				return true;
			} else if (this.isMember()) {
				this.doRoleEstablishedActions();
				role_not_established = false;
				log("Member Role Established.");
				return true;
			} else if (isOwner() && ownerIP == null) {
				log("Cannot establise role: Owner IP is null.");
				return false;
			}
		} else if(!localDisplayNameAvailable) {
			log("Cannot establish role: Display Name or role is unknown.");
			return false;
		}
		return false;
	}
	
	////////////////////////////////
	// Server / Client Operations //
	////////////////////////////////
	
	/**
	 * Do the owner service routine.
	 */
	protected void doOwnerRoutine() {
		serve();
	}
	
	/**
	 * Do the member connection routine
	 */
	protected void doMemberRoutine() {
		connect();
	}
	
	/**
	 * Run thread operation of a BaseDirectory. 
	 */
	@Override
	public void run() {
		switch(role) {
			case OWNER_ROLE:
				doOwnerRoutine();
				break;
			case MEMBER_ROLE:
				doMemberRoutine();
				break;
		}
	}
	
	/**
	 * Runs the 'start' method of this Thread class. See 'run' for the actual operations invoked.
	 */
	@Override
	public void init() {
		super.init();
		this.start();
	}

	protected Boolean closing = false;
	protected void initClosing() { closing = true; }
	protected boolean isClosing() { return closing; }
	
	/**
	 * Signals the directory to stop all active subroutines. This includes,
	 * member connection attempts and owner ping routines. Administrative
	 * data pipe will not be affected.
	 */
	public void stopRoutines() { initClosing(); }
	
	/**
	 * Close all operations of the directory. Specifically, an owner will
	 * notify all members of this closure, while a member will notify the
	 * owner. This is followed by the termination of all active threads of
	 * this directory, namely administrative data pipes and connection loops.
	 */
	public void close() {
		this.initClosing();
		super.close();
	}
	

	/**
	 * Run owner service
	 */
	abstract public void serve();
	
	Boolean accepted = false;
	protected boolean isAccepted() { return accepted; }
	protected void setAccepted() { accepted = true; }

	Boolean rejected = false;
	protected boolean isRejected() { return rejected; }
	protected void setRejected() { rejected = true; }
	
	/**
	 * Client (Non-group owner) Routine, tries to establish a connection with the
	 * group owner. 
	 * @param tries number of tries.
	 * @param time_out time in milliseconds till next try.
	 * @return true if connection is successfully established
	 */
	abstract public boolean connect(int tries, int time_out);
	
	/**
	 * Client (Non-group owner) Routine, tries to establish a connection with the
	 * group owner, with default settings: unlimited retries, 3 secs apart.
	 * @return
	 */
	public boolean connect() { return connect(-1, 3000); }
	
	/////////////////////
	// Role Operations //
	/////////////////////
	
	/**
	 * Set the owner's IP address
	 * @param ownerIP IP address of the owner
	 */
	public void setOwnerIP(String ownerIP) {
		this.ownerIP = ownerIP;
		// establishRole();
	}
	
	public String getOwnerIP() {
		for(NodeInfo node: this.getNodes()) {
			if(node.role.equals("Owner")) {
				return node.ipAddress;
			}
		}
		return ownerIP;
	}
	
	/**
	 * @return true if this directory is an owner directory
	 */
	public boolean isOwner() { return role == OWNER_ROLE; }

	/**
	 * @return true if this directory is a member directory
	 */
	public boolean isMember() { return role == MEMBER_ROLE; }

	/**
	 * @return true if it is not known if this is owner or member
	 */
	public boolean isUnknown() { return role == UNKNOWN_ROLE; }
	
	/**
	 * @return true if it is known if this is owner or member
	 */
	public boolean isKnown() { return role == OWNER_ROLE || role == MEMBER_ROLE; }
	
	/**
	 * Set the role of the directory
	 * @param new_role
	 */
	private void setRole(int new_role) { 
		role = new_role; 
		// establishRole();
	}
	
	/**
	 * Set this directory to the role of an owner.
	 */
	public void setOwnerRole() { setRole(OWNER_ROLE); }

	/**
	 * Set this directory to the role of a member.
	 */
	public void setMemberRole() { setRole(MEMBER_ROLE); }
	
	/**
	 * @return the textual representation of the role of this directory.
	 */
	public String showRole() { return showRole(role); }
	
	/**
	protected int loc_count = 1;
	 * @param role the role of a directory.
	 * @return the textual representation of the input role.
	 */
	public String showRole(int role) {
		switch(role) {
		case OWNER_ROLE: return "Owner";
		case MEMBER_ROLE: return "Member";
		default: return "Unknown";
		}
	}
	
	/**
	 * Return the role of the given location identifier
	 * @param loc the location identifier
	 * @return role of the location identifier
	 */
	public int locRole(int loc) {
		if(loc == OWNER_LOC) {
			return OWNER_ROLE;
		} else {
			return MEMBER_ROLE;
		}
	}
	
	/////////////////////
	// Misc Operations //
	/////////////////////
	
	/**
	 * Returns true if directory has been notified that Wifi adapter is enabled.
	 * @return true if directory has been notified that Wifi adapter is enabled.
	 */
	public boolean isWifiEnabled() { return wifiEnabled; }

	public void setIsWifiEnabled(boolean wifiEnabled) {
		this.wifiEnabled = wifiEnabled;
	}
	
	/**
	 * Returns true if directory has been notified that Wifi adapter is connected.
	 * @return true if directory has been notified that Wifi adapter is connected.
	 */
	public boolean isWifiConnected() { return wifiConnected; }
	
	/**
	 * Returns true if directory has either established a connection with an owner directory,
	 * or is an owner and is ready to receive connection request.
	 * @return true if directory has either established a connection with an owner directory,
	 * or is an owner and is ready to receive connection request.
	 */
	public boolean isConnected() { return dirConnected; }
	
	public void setIsConnected(boolean b) { dirConnected = b; }
	
	/**
	 * Broadcasts a list of messages to all peers (except self).
	 * @param messages list of messages to broadcast.
	 */
	protected void broadcastMessage(List<D> messages) {
		for(NodeInfo peer: peers) {
			if(!local.ipAddress.equals( peer.ipAddress )) {
				this.sendData(messages, peer.ipAddress, true);
			}
		}
	}

	/**
	 * Broadcasts a message to all peers (except self).
	 * @param messages the message to broadcast.
	 */
	protected void broadcastMessage(D message) {
		List<D> messages = new LinkedList<D>();
		messages.add(message);
		broadcastMessage(messages);
	}
	
	protected void sleep(int msec) {
		try {
			Thread.sleep(msec);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	}
	
	///////////////////////////////
	// Directory Node Operations //
	///////////////////////////////
	
	
	protected int getNextLoc() {
		int next_loc = this.locCount;
		locCount++;
		return next_loc;
	}
	
	protected int getIdentity(String identityStr) {
		if (useDeviceIDAsLocationID) {
			return identityStr.hashCode();
		} else {
			return getNextLoc();
		}
	}
	
	public NodeInfo newNodeInfo(String ipAddress, String name, String deviceID, String role) {
		// int newLoc = getNextLoc();
		return new NodeInfo(getIdentity( deviceID ), networkID, ipAddress, name, deviceID, role);
	}
	
	public NodeInfo localNodeInfo(int loc, String ipAddress, String role) {
		return new NodeInfo(loc, networkID, ipAddress, getDisplayName(), getDeviceID(), role);
	}
	
	/**
	 * Return a hash map that maps locations to IP addresses.
	 * @return a hash map containing the location -> IP address mappings.
	 */	
	public HashMap<Integer,String> getNodeMap() {
		HashMap<Integer,String> map = new HashMap<Integer,String>();
		synchronized(peers) {
			for(NodeInfo peer: peers) {
				map.put(peer.location, peer.ipAddress);
			}
		}
		return map;
	}
	
	/**
	 * @return an array containing all peers in this directory.
	 */
	public NodeInfo[] getNodeArray() {
		NodeInfo[] arr = new NodeInfo[peers.size()];
		synchronized(peers) {
			int x = 0;
			for(NodeInfo peer: peers) {
				arr[x] = peer;
				x++;
			}
		}
		return arr;
	}
	
	/**
	 * @return the list of peers in this directory.
	 */
	public List<NodeInfo> getNodes() {
		return peers;
	}
	
	/**
	 * @return the local node info of the directory.
	 */
	public NodeInfo getLocalNode() { 
		return local;
	}
	
	/**
	 * Set the local node to the given node.
	 * @param node a node info class to be set as the local node.
	 */
	public void setLocalNode(NodeInfo node) {
		local = node;
	}
	
	protected void addNode(NodeInfo new_peer) {
		for(NodeInfo node: peers) {
			if(node.location == new_peer.location) {
				return;
			}
		}
		peers.add(new_peer);
	}
	
	protected void addNode(Collection<NodeInfo> new_peers) {
		peers.addAll(new_peers);
	}
	
	/**
	 * Destructively update the directory's peers.
	 * @param new_peers new updated list of peers.
	 */
	protected void updateNodes(NodeInfo[] new_peers) {
		synchronized(peers) {
			peers.clear();
			for(NodeInfo peer: new_peers) {
				peers.add(peer);
			}
		}
	}
	
	/**
	 * Remove a given node from the list of peers.
	 */
	protected boolean removeNode(NodeInfo node) {
		return peers.remove(node);
	}
	
	protected NodeInfo removeNode(int loc) {
		synchronized(peers) {
			for(int x=0; x<peers.size(); x++) {
				if(peers.get(x).location == loc) {
					NodeInfo node = peers.get(x);
					peers.remove(x);
					return node;
				}
			}
		}
		return null;
	}
	
	
	/**
	 * Returns true if a peer with this location ID exists in this directory.
	 * @param loc
	 * @return true if a peer with this location ID exists in this directory.
	 */
	public boolean hasNode(int loc) {
		for(NodeInfo peer: peers) {
			if(peer.location == loc) {
				return true;
			}
		} 
		return false;
	}
	
	/**
	 * Returns true if a peer with this InetAddress exists in this directory.
	 * @param inetaddr
	 * @return Returns true if a peer with this InetAddress exists in this directory.
	 */
	public boolean hasNode(InetAddress inetaddr) {
		return hasNode(inetaddr.getHostAddress());
	}
	
	/**
	 * Returns true if a peer with this IP address exists in this directory.
	 * @param ipAddr
	 * @return Returns true if a peer with this IP address exists in this directory.
	 */
	public boolean hasNode(String ipAddr) {
		for(NodeInfo peer: peers) {
			if(peer.ipAddress == ipAddr) {
				return true;
			}
		}
		return false;			
	}
	
	/**
	 * Returns true if a node with this InetAddress exists in this directory. This
	 * Method uses the NodeInfo's 'equals' method.
	 * @param inetaddr
	 * @return Returns true if a node with this InetAddress exists in this directory
	 */
	public boolean hasNode(NodeInfo peer) {
		return peers.contains(peer);
	}
	
	/**
	 * Get the node info corresponding to the input inet address.
	 * @param inetaddr the inet address of the desired node.
	 * @return the node info of inetaddr, null if it does not exist.
	 */
	public NodeInfo getNode(InetAddress inetaddr) {
		return getNode(inetaddr.getHostAddress());
	}
	
	/**
	 * Get the node info corresponding to the input ip address.
	 * @param ipAddr the ip address of the desired node.
	 * @return the node info of the given ip address, null if it does not exist.
	 */
	public NodeInfo getNode(String ipAddr) {
		for(NodeInfo peer: peers) {
			if(peer.ipAddress == ipAddr) {
				return peer;
			}
		}
		return null;		
	}
	
	/**
	 * Get a node given a unique location identifier.
	 * @param loc the location of the node
	 * @return the NodeInfo object of this location. null if it does not exist.
	 */
	public NodeInfo getNode(int loc) {
		for(NodeInfo peer: peers) {
			if(peer.location == loc) {
				return peer;
			}
		}
		return null;
	}
	
	/**
	 * Returns the device name of this location.
	 * @return the device name of this location.
	 */
	public String getLocalName() {
		NodeInfo node = getLocalNode();
		if(node != null) {
			return node.displayName;
		} else {
			return "";
		}
	}
	
	/**
	 * Returns the device name of the given location identifier
	 * @param loc the location
	 * @return the device name of loc.
	 */
	public String getName(int loc) {
		NodeInfo node = getNode(loc);
		if(node != null) {
			return node.displayName;
		} else {
			return null;
		}
	}
	
	/**
	 * Returns a map containing location to display name mappings
	 * @return a map containing location to display name mappings
	 */
	public Map<Integer,String> getNames() {
		Map<Integer,String> names = new HashMap<Integer,String>();
		synchronized(peers) {
			for(NodeInfo n: peers) {
				names.put(n.location, n.displayName);
			}
		}
		return names;
	}
	
	/////////////////////////
	// Listener Operations //
	/////////////////////////

	/**
	 * Invoke directory change listeners. 
	 */
	@Override
	protected void doDirectoryChangedActions(List<NodeInfo> added_nodes, List<NodeInfo> dropped_nodes) {
		synchronized(dclisteners) {
			for(DirectoryChangedListener listener: dclisteners) {
				listener.doDirectoryChangedAction(peers, added_nodes, dropped_nodes, role);
			}	
		}
	}
	
	/**
	 * Invoke connection established listeners.
	 */
	@Override
	protected void doConnectionEstablishedActions(NodeInfo peer) {
			
		log( String.format("Connection established with %s", peer) );
		
		for (ConnectionEstablishedListener listener: celisteners) {
			listener.doConnectionEstablishedAction(peer, role);
		}
	}
	
	/**
	 * Invoke role established listeners
	 */
	@Override
	protected void doRoleEstablishedActions() {
		log( String.format("Role established: %s", showRole(role)) );
		init();		
		for (RoleEstablishedListener listener: relisteners) {
			listener.doRoleEstablishedAction(role);
		}
	}
	
	/**
	 * Invoke local node info available listeners.
	 */
	@Override
	protected void doLocalNodeInfoAvailableActions() {
		for (LocalNodeInfoAvailableListener listener: nilisteners) {
			listener.doLocalNodeInfoAvailableAction(local, role);
		}
	}
	
	/**
	 * Invoke owner termination listeners
	 * @param owner the owner's node info
	 */
	@Override
	protected void doOwnerTerminationActions(NodeInfo owner) {
		for (OwnerTerminationListener listener: otlisteners) {
			listener.doOwnerTerminationAction(owner, role);
		}
	}
	
	/**
	 * Invoke network status changed listeners. The doWifiAdapterStatusChangedAction
	 * of each listener will be called. This method is intended to be called by a
	 * network monitoring service (e.g., a BroadcastReceiver that registers on network events.).
	 * @param enabled true if the wifi adapter is enabled.
	 */
	@Override
	public void doWifiAdapterStatusChanged(boolean enabled) {
		this.wifiEnabled = enabled;
		for(NetworkStatusChangedListener listener: nsclisteners) {
			listener.doWifiAdapterStatusChangedAction(enabled);
		}
	}
	
	/**
	 * Invoke network status changed listeners. The doWifiConnectionStatusChangedAction
	 * of each listener will be called. This method is intended to be called by a
	 * network monitoring service (e.g., a BroadcastReceiver that registers on network events.).
	 * @param connected true if the wifi adapter is connected to some network.
	 */
	@Override
	public void doWifiConnectionStatusChanged(boolean connected) {
		this.wifiConnected = connected;
		for(NetworkStatusChangedListener listener: nsclisteners) {
			listener.doWifiConnectionStatusChangedAction(connected);
		}		
	}
	
	/////////////////////////////
	// Neighborhood Operations //
	/////////////////////////////
	
	public int getLocation() {
		return this.local.location;
	}
	
	public Collection<Integer> getLocations(int networkID) {
		if(this.networkID == networkID) {
			return this.getLocations();
		} else {
			return new LinkedList<Integer>();
		}
	}
	
	/**
	 * Return a list containing all location ids in this directory.
	 * @return list of all location ids.
	 */	
	public Collection<Integer> getLocations() {
		LinkedList<Integer> locs = new LinkedList<Integer>();
		synchronized(peers) {
			for(NodeInfo peer: peers) {
				locs.add(peer.location);
			}
		}
		return locs;
	}
	
	public boolean isConnectedTo(int destLoc, int networkID) {
		if(this.networkID == networkID) {
			return this.getNode(destLoc) != null;
		} else {
			return false;
		}
	}
	
	public boolean isConnectedTo(int destLoc) {
		return this.getNode(destLoc) != null;
	}
	
	public String lookupIPAddress(int loc) {
		NodeInfo node = getNode(loc);
		if (node != null) {
			return node.ipAddress;
		} else {
			return null;
		}
	}
	
	public Map<Integer,String> lookupIPAddresses(int loc) {
		String ipAddr = lookupIPAddress(loc);
		Map<Integer,String> map = new HashMap<Integer,String>();
		if (ipAddr != null) {
			map.put(loc, ipAddr);
		}
		return map;
	}
	
	
}
