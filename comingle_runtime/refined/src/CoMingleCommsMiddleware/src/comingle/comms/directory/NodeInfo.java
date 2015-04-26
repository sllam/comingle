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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

/**
 * 
 * A record class containing information on a particular node. A BaseDirectory essentially maintains a list of NodeInfo
 * in each participating node's directory.
 * 
 * @author Edmund S.L. Lam
 *
 */
public class NodeInfo implements Serializable {

	private static final long serialVersionUID = 8780345863373630822L;

	public final int location;
	public final int networkID;
	public final String displayName;
	public final String deviceID;
	public final String role;
	public String ipAddress;
	
	/**
	 * Basic Constructor
	 * @param location Assigned location ID of the node
	 * @param networkID Unique ID of the network that node is part of
	 * @param ipAddress Address of the node
	 * @param name Display name of the node
	 * @param deviceID Unique ID of the node
	 * @param role Role of the node in the directory
	 */
	public NodeInfo(int location, int networkID, String ipAddress, String name, String deviceID, String role) {
		this.location    = location;
		this.networkID   = networkID;
		this.ipAddress   = ipAddress;
		this.displayName = name;
		this.deviceID    = deviceID;
		this.role        = role;
	}
	
	/**
	 * Set argment as IP Address of the node
	 * @param ipAddress
	 */
	public void setIPAddress(String ipAddress) {
		this.ipAddress = ipAddress;
	}
	
	/**
	 * returns true only if other is a NodeInfo with the same device ID.
	 */
	@Override
	public boolean equals(Object other) {
		NodeInfo other_peer = null;
		try {
			other_peer = (NodeInfo) other;
		} catch (ClassCastException ex) {
			return false;
		}
		return other_peer.deviceID.equals(this.deviceID);
	}
	
	@Override
	public String toString() {
		return String.format("NodeInfo(%s,%s,%s,%s,%s,%s)", location, networkID, displayName, ipAddress, deviceID, role);
	}
	
	private void writeObject(ObjectOutputStream aOutputStream) throws IOException {
		aOutputStream.defaultWriteObject();
	}

	private void readObject(ObjectInputStream aInputStream) throws ClassNotFoundException, IOException {
		aInputStream.defaultReadObject();
	}
	
	public static List<NodeInfo> addedDiff(List<NodeInfo> ns, List<NodeInfo> os) {
		List<NodeInfo> added = new LinkedList<NodeInfo>();
		for (NodeInfo n: ns) {
			if(!os.contains(n)) { added.add(n); }
		}
		return added;
	}
	
	public static List<NodeInfo> addedDiff(NodeInfo[] arr, List<NodeInfo> os) {
		List<NodeInfo> ns = new LinkedList<NodeInfo>();
		for(NodeInfo n: arr) { ns.add(n); }
		return addedDiff(ns, os);
	}

	public static List<NodeInfo> droppedDiff(List<NodeInfo> ns, List<NodeInfo> os) {
		List<NodeInfo> dropped = new LinkedList<NodeInfo>();
		for (NodeInfo o: os) {
			if(!ns.contains(o)) { dropped.add(o); }
		}
		return dropped;
	}

	public static List<NodeInfo> droppedDiff(NodeInfo[] arr, List<NodeInfo> os) {
		List<NodeInfo> ns = new LinkedList<NodeInfo>();
		for(NodeInfo n: arr) { ns.add(n); }
		return droppedDiff(ns, os);
	}
	
}
