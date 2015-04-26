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

package comingle.comms.listeners;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

import comingle.comms.datapipe.DataPipe;
import comingle.comms.datapipe.DataPipeManager;
import comingle.comms.directory.NodeInfo;
import comingle.comms.log.Logger;
import comingle.comms.neighborhood.Neighborhood;

/**
 * 
 * Listening Post abstract class that manages a data pipe and introduces NodeInfo listening functionality.
 * 
 * @author Edmund S.L. Lam
 *
 * @param <D> Type of administrative data that data pipe handles
 * @param <A> Type of address that nodes uses
 */
abstract public class ListeningPost<D extends Serializable,A> extends DataPipeManager<D,A> implements Logger, Neighborhood {

	protected List<RoleEstablishedListener> relisteners; 
	protected List<ConnectionEstablishedListener> celisteners; 
	protected List<DirectoryChangedListener> dclisteners;
	protected List<LocalNodeInfoAvailableListener> nilisteners;
	protected List<OwnerTerminationListener> otlisteners;
	protected List<NetworkStatusChangedListener> nsclisteners;
	
	public ListeningPost(DataPipe<D, A> pipe) {
		super(pipe);
		this.relisteners = new LinkedList<RoleEstablishedListener>();
		this.celisteners = new LinkedList<ConnectionEstablishedListener>();
		this.dclisteners = new LinkedList<DirectoryChangedListener>();
		this.nilisteners = new LinkedList<LocalNodeInfoAvailableListener>(); 
		this.otlisteners = new LinkedList<OwnerTerminationListener>();
		this.nsclisteners = new LinkedList<NetworkStatusChangedListener>();
	}
	
	/////////////////////////
	// Listener Operations //
	/////////////////////////
	
	/**
	 * Add a directory changed listener to this directory. Listener operations are
	 * invoked every time this directory is updates.
	 * @param listener the listener to add.
	 */
	public void addDirectoryChangedListener(DirectoryChangedListener listener) {
		synchronized(this.dclisteners) {
			this.dclisteners.add(listener);
		}
	}

	/**
	 * Invoke directory change listeners. 
	 */
	abstract protected void doDirectoryChangedActions(List<NodeInfo> added_nodes, List<NodeInfo> dropped_nodes);
	
	public void removeDirectoryChangedListener(DirectoryChangedListener listener) {
		synchronized(this.dclisteners) {
			this.dclisteners.remove(listener);
		}
	}
	
	/**
	 * Add a connection established listener to this directory. Listener operations are
	 * invoked every time this directory has established a connection.
	 * @param listener the listener to add.
	 */
	public void addConnectionEstablishedListener(ConnectionEstablishedListener listener) {
		this.celisteners.add(listener);
	}
	
	/**
	 * Invoke connection established listeners.
	 */
	abstract protected void doConnectionEstablishedActions(NodeInfo peer);
	
	public void removeConnectionEstablishedListener(ConnectionEstablishedListener listener) {
		this.celisteners.remove(listener);
	}
	
	/**
	 * Add a role established listener to this directory. Listener operations are
	 * invoked each time this directory transit from an 'unknown' role state to
	 * a known role state.
	 * @param listener the listener to add.
	 */
	public void addRoleEstablishedListener(RoleEstablishedListener listener) {		
		this.relisteners.add(listener);
	}
	
	/**
	 * Invoke role established listeners
	 */
	abstract protected void doRoleEstablishedActions();
	
	public void removeRoleEstablishedListener(RoleEstablishedListener listener) {
		this.relisteners.remove(listener);
	}
	
	/**
	 * Add a local node info available listener to this directory. Listener operations
	 * are invoked when the node info of the local node is available.
	 * @param listener the listener to add.
	 */
	public void addLocalNodeInfoAvailableListener(LocalNodeInfoAvailableListener listener) {
		this.nilisteners.add( listener );
	}
	
	/**
	 * Invoke local node info available listeners.
	 */
	abstract protected void doLocalNodeInfoAvailableActions();
	
	public void removeLocalNodeInfoAvailableListener(LocalNodeInfoAvailableListener listener) {
		this.nilisteners.remove(listener);
	}
	
	/**
	 * Add an owner termination listener to this directory. Listener operations
	 * are invoked when the directory has been notified that the owner node is
	 * no longer active.
	 * @param listener the listener to add.
	 */
	public void addOwnerTerminationListener(OwnerTerminationListener listener) {
		this.otlisteners.add(listener);
	}
	
	/**
	 * Invoke owner termination listeners
	 * @param owner the owner's node info
	 */
	abstract protected void doOwnerTerminationActions(NodeInfo owner);
	
	public void removeOwnerTerminationListener(OwnerTerminationListener listener) {
		this.otlisteners.remove(listener);
	}
	
	/**
	 * Add a network status changed listener to this directory. Listener operations
	 * are invoked when doNetworkStatusChangedActions is invoked.
	 * @param listener the listener to add.
	 */
	public void addNetworkStatusChangedListener(NetworkStatusChangedListener listener) {
		this.nsclisteners.add(listener);
	}
	
	/**
	 * Invoke network status changed listeners. The doWifiAdapterStatusChangedAction
	 * of each listener will be called. This method is intended to be called by a
	 * network monitoring service (e.g., a BroadcastReceiver that registers on network events.).
	 * @param enabled true if the wifi adapter is enabled.
	 */
	public void doWifiAdapterStatusChanged(boolean enabled) {
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
	public void doWifiConnectionStatusChanged(boolean connected) {
		for(NetworkStatusChangedListener listener: nsclisteners) {
			listener.doWifiConnectionStatusChangedAction(connected);
		}		
	}
	
	public void removeNetworkStatusChangedListener(NetworkStatusChangedListener listener) {
		this.nsclisteners.remove(listener);
	}
	
	////////////////////////////////
	// Network Adapter Operations //
	////////////////////////////////
	
	public void initNetworkNotifications() { }
	
	public void resumeNetworkNotifications() { }
	
	public void pauseNetworkNotifications() { }
		
	public void displayDirectorySetupDialogs(int rowId, int nameId, int locId, int ipId) { }
	
}
