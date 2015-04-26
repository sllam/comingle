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

package comingle.runtime;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import android.util.Log;

import comingle.comms.directory.BaseDirectory;
import comingle.comms.directory.NodeInfo;
import comingle.comms.listeners.ConnectionEstablishedListener;
import comingle.comms.listeners.DirectoryChangedListener;
import comingle.comms.listeners.ListeningPost;
import comingle.comms.listeners.LocalNodeInfoAvailableListener;
import comingle.comms.listeners.NetworkStatusChangedListener;
import comingle.comms.listeners.OwnerTerminationListener;
import comingle.comms.listeners.RoleEstablishedListener;
import comingle.comms.message.Message;

public class CompositeDirectory extends ListeningPost<Message,String> {

	protected static final String TAG = "CompositeDirectory";
	
	protected BaseDirectory<Message> mainDir;
	
	@SuppressWarnings("rawtypes")
	protected final Map<Integer,BaseDirectory> auxDirs;
	
	@SuppressWarnings("rawtypes")
	public CompositeDirectory(BaseDirectory<Message> mainDir) {
		super(null);
		this.mainDir = mainDir;
		auxDirs = new HashMap<Integer,BaseDirectory>();
		auxDirs.put(mainDir.getNetworkID(), mainDir);
	}
	
	@SuppressWarnings("rawtypes")
	public void attachDirectory(BaseDirectory newDir) {
		auxDirs.put(newDir.getNetworkID(), newDir);
	}
	
	public void close() {
		for(BaseDirectory dir: auxDirs.values()) {
			dir.close();
		}
	}
	
	///////////////////////////////////////////////////
	// Android Activity Network Notification Methods //
	///////////////////////////////////////////////////
	
	public void resumeNetworkNotifications() {
		for(BaseDirectory dir: auxDirs.values()) {
			dir.resumeNetworkNotifications();
		}
	}

	public void pauseNetworkNotifications() {
		for(BaseDirectory dir: auxDirs.values()) {
			dir.pauseNetworkNotifications();
		}
	}
	
	public boolean isWifiEnabled() {
		return mainDir.isWifiEnabled();
	}
	
	public boolean isWifiConnected() {
		return mainDir.isWifiConnected();
	}
	
	//////////////////////
	// Identity Methods //
	//////////////////////
	
	public boolean isOwner() {
		return mainDir.isOwner();
	}
	
	public boolean isMember() {
		return mainDir.isMember();
	}
	 
	//////////////////////////
	// Neighborhood Methods //
	//////////////////////////
	
	@Override
	public int getLocation() {
		return mainDir.getLocation();
	}

	@Override
	public Collection<Integer> getLocations(int networkID) {
		if(auxDirs.containsKey(networkID)) {
			return auxDirs.get(networkID).getLocations();
		}
		return null;
	}

	@Override
	public Collection<Integer> getLocations() {
		Collection<Integer> locs = new LinkedList<Integer>();
		for (BaseDirectory dir: auxDirs.values()) {
			locs.addAll(dir.getLocations());
		}
		return locs;
	}

	@Override
	public boolean isConnectedTo(int destLoc, int networkID) {
		if( auxDirs.containsKey(networkID) ) {
			return auxDirs.get(networkID).hasNode(destLoc);
		} else {
			return false;
		}
	}

	@Override
	public boolean isConnectedTo(int destLoc) {
		for(BaseDirectory dir: auxDirs.values()) {
			if(dir.hasNode(destLoc)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public String lookupIPAddress(int loc) {
		for(BaseDirectory dir: auxDirs.values()) {
			NodeInfo node = dir.getNode(loc);
			if(node != null) {
				return node.ipAddress;
			}
		}
		return null;
	}

	@Override
	public Map<Integer, String> lookupIPAddresses(int loc) {
		Map<Integer, String> ipAddrs = new HashMap<Integer,String>();
		for(BaseDirectory dir: auxDirs.values()) {
			NodeInfo node = dir.getNode(loc);
			if(node != null) {
				ipAddrs.put(dir.getNetworkID(), node.ipAddress);
			}
		}
		return ipAddrs;
	}

	@Override
	public void log(String msg) {
		Log.i(TAG, msg);
	}

	@Override
	public void err(String msg) {
		Log.e(TAG, msg);
	}

	@Override
	public void info(String msg) {
		Log.i(TAG, msg);
	}

	// Current implementation only allows Listeners to be attached to the main directory
	
	@Override
	public void addDirectoryChangedListener(DirectoryChangedListener listener) {
		mainDir.addDirectoryChangedListener(listener);
	}
	
	@Override
	public void addConnectionEstablishedListener(ConnectionEstablishedListener listener) {
		mainDir.addConnectionEstablishedListener(listener);
	}
	
	@Override
	public void addRoleEstablishedListener(RoleEstablishedListener listener) {		
		mainDir.addRoleEstablishedListener(listener);
	}	
	
	@Override
	public void addLocalNodeInfoAvailableListener(LocalNodeInfoAvailableListener listener) {
		mainDir.addLocalNodeInfoAvailableListener(listener);
	}
	
	@Override
	public void addOwnerTerminationListener(OwnerTerminationListener listener) {
		mainDir.addOwnerTerminationListener(listener);
	}
	
	@Override
	public void addNetworkStatusChangedListener(NetworkStatusChangedListener listener) {
		mainDir.addNetworkStatusChangedListener(listener);
	}
	
	@Override
	public void removeDirectoryChangedListener(DirectoryChangedListener listener) {
		mainDir.removeDirectoryChangedListener(listener);
	}
	
	@Override
	public void removeConnectionEstablishedListener(ConnectionEstablishedListener listener) {
		mainDir.removeConnectionEstablishedListener(listener);
	}
	
	@Override
	public void removeRoleEstablishedListener(RoleEstablishedListener listener) {		
		mainDir.removeRoleEstablishedListener(listener);
	}	
	
	@Override
	public void removeLocalNodeInfoAvailableListener(LocalNodeInfoAvailableListener listener) {
		mainDir.removeLocalNodeInfoAvailableListener(listener);
	}
	
	@Override
	public void removeOwnerTerminationListener(OwnerTerminationListener listener) {
		mainDir.removeOwnerTerminationListener(listener);
	}
	
	@Override
	public void removeNetworkStatusChangedListener(NetworkStatusChangedListener listener) {
		mainDir.removeNetworkStatusChangedListener(listener);
	}
	
	@Override
	protected void doDirectoryChangedActions(List<NodeInfo> added_nodes,
			List<NodeInfo> dropped_nodes) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void doConnectionEstablishedActions(NodeInfo peer) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void doRoleEstablishedActions() {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void doLocalNodeInfoAvailableActions() {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void doOwnerTerminationActions(NodeInfo owner) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void receiveData(List<Message> data_list, String addr) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void handleReceiveException(Exception e) {
		// TODO Auto-generated method stub
		
	}

}
