/**
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

CoMingle Version 1.0, Beta Prototype

Authors:
Edmund S. L. Lam      sllam@qatar.cmu.edu

* This implementation was made possible by an JSREP grant (JSREP 4-003-2-001, Effective Distributed 
Programming via Join Patterns with Guards, Propagation and More) from the Qatar National Research Fund 
(a member of the Qatar Foundation). The statements made herein are solely the responsibility of the authors.
**/

package p2pdirectory;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import comingle.actuation.ActuatorAction;
import comingle.facts.SerializedFact;
import comingle.mset.SimpMultiset;
import comingle.nodes.SendListener;
import comingle.rewrite.QuiescenceEvent;
import comingle.rewrite.QuiescenceListener;
import comingle.tuple.Tuple3;
import comingle.tuple.Tuple4;
import comingle.tuple.Unit;

import p2pdirectory.P2pdirectory;

import sllam.extras.admin.NodeInfo;
import sllam.extras.directory.BaseDirectory;
import sllam.extras.wifidirect.listeners.RoleEstablishedListener;
import android.app.Activity;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.util.Log;

public class WifiDirectComingleDirectory extends BaseDirectory<SerializedFact>  {

	private final static String TAG = "WifiDirectComingleDirectory";
	
	public final static int OWNER_ROLE = 0;
	public final static int MEMBER_ROLE = 1;
	public final static int UNKNOWN_ROLE = -1;
	
	public final static String OWNER_IP = "192.168.49.1";
	
	private final static int DEFAULT_LOCATION = -101;
	private final static String DEFAULT_IP = "100.100.100.100";
	
	public static int nextLoc(int loc) {
		return loc+1;
	}
	
	public static int ownerLoc(int i) { return ownerMacHash;  }
	
	private final static Map<Integer,String> macHashToIP = new HashMap<Integer,String>();
	private static List<NodeInfo> nodeDirList = null;
	private static int ownerMacHash = -1;
	
	public static String lookupIP(int macHash) {
		return macHashToIP.get(macHash);
	}
	
	private static void setOwnerMacHash(String mac) {
		ownerMacHash = macHash(mac);
	}
	
	private static void setNodeDirList(List<NodeInfo> nodes) {
		nodeDirList = nodes;
	}
	
	public static int macHash(String mac) {
		String[] macComp = mac.split(":");
		String macHash = (macComp[2].substring(1))+ macComp[3] + macComp[4] + macComp[5];
		return Integer.parseInt(macHash, 16);
	}
	
	public static SimpMultiset<Tuple3<Integer,String,String>> retrieveDir(int i) {
		SimpMultiset<Tuple3<Integer,String,String>> mset = new SimpMultiset<Tuple3<Integer,String,String>>();
		synchronized(nodeDirList) {
			for(NodeInfo node: nodeDirList) {
				mset.add( new Tuple3<Integer,String,String>(node.location, node.ip_address, node.name) );
			}
		}
		return mset;
	}
	
	
	private final P2pdirectory p2pDir;
	
	private int myLocation = DEFAULT_LOCATION;
	
	public WifiDirectComingleDirectory(Activity activity, int port, String reqCode, String ownerIP) {
		super(activity, port, reqCode, ownerIP);
		this.p2pDir = new P2pdirectory();
		setNodeDirList(peers);
		initP2pDir();
	}
	
	public WifiDirectComingleDirectory(Activity activity, int port, String reqCode) {
		this(activity, port, reqCode, OWNER_IP);
	}
	
	private void initP2pDir() {
		
		p2pDir.setAddedActuator(new ActuatorAction<Tuple3<Integer,String,String>>() {
			@Override
			public void doAction(Tuple3<Integer, String, String> tup) {
				int loc       = tup.t1; 
				String ipAddr = tup.t2;
				String name   = tup.t3;
				String role   = showRole(MEMBER_ROLE);
				NodeInfo newNode = new NodeInfo(loc, ipAddr, name, String.format("%s", loc), role);
				addNode( newNode );
				List<NodeInfo> newNodes = new LinkedList<NodeInfo>();
				newNodes.add( newNode );
				if(myLocation == loc) { updateMyLocation(newNode); }		
				doDirectoryChangedActions( newNodes, new LinkedList<NodeInfo>() );
			}
		});
		
		p2pDir.setRemovedActuator(new ActuatorAction<Integer>() {
			@Override
			public void doAction(Integer location) {
				NodeInfo node = removeNode(location);
				if(node != null) {
					List<NodeInfo> rmNodes = new LinkedList<NodeInfo>();
					rmNodes.add(node);
					doDirectoryChangedActions( new LinkedList<NodeInfo>(), rmNodes );
				}
			}
		});
		
		p2pDir.setConnectedActuator(new ActuatorAction<Unit>() {
			@Override
			public void doAction(Unit input) {
				// NodeInfo myNode = getNode(myLocation);
				// setLocalNode(myNode);
				// setIsConnected(true);
				setAccepted();
				// doConnectionEstablishedActions(myNode);
				// setMyLocation(myLocation);
			}
		});
		
		p2pDir.setOwnerQuitActuator(new ActuatorAction<Unit>() {
			@Override
			public void doAction(Unit input) {
				NodeInfo ownerNode = getNode(OWNER_LOC);
				setIsConnected(false);
				doOwnerTerminationActions( ownerNode );
			}
		});
		
		p2pDir.setDeleteDirActuator(new ActuatorAction<Unit>() {
			@Override
			public void doAction(Unit input) {
				
			}
		});
	
		p2pDir.addPersistentQuiescenceListener(new QuiescenceListener() {
			@Override
			public void performQuiescenceAction(QuiescenceEvent qe) {
				p2pDir.updateNeighbors(getNodeMap());
			}
		});
		
		this.addRoleEstablishedListener(new RoleEstablishedListener() {
			@Override
			public void doRoleEstablishedAction(WifiP2pGroup group, int role) {
				startP2pDir(group, role);
			}
		});
		
	}
	
	private void startP2pDir(WifiP2pGroup group, int role) {
		WifiP2pDevice ownerDevice = group.getOwner();
		
		int initLoc   = DEFAULT_LOCATION;
		String initIP = DEFAULT_IP;
		
		setOwnerMacHash( ownerDevice.deviceAddress );
		
		if(isOwner()) {
			local.location = WifiDirectComingleDirectory.macHash( ownerDevice.deviceAddress );
			initLoc = local.location;
			initIP  = ownerIP;
			myLocation = initLoc;
		} else {
			int ownerLoc = WifiDirectComingleDirectory.macHash( ownerDevice.deviceAddress );
			NodeInfo ownerInfo = new NodeInfo(ownerLoc, ownerIP, ownerDevice.deviceName, ownerDevice.deviceAddress
					                         ,showRole(OWNER_ROLE));
			addNode(ownerInfo);
			
			initLoc = WifiDirectComingleDirectory.macHash( thisDevice.deviceAddress );
			initIP  = DEFAULT_IP;
			myLocation = initLoc;
		}
		
		HashMap<Integer,String> neighbors = getNodeMap();
		SendListener rmSendListener = new SendListener() {
			@Override
			public void performSendAction(final String ip_address, final List<SerializedFact> facts) {
				activity.runOnUiThread(
						new Runnable() {
							@Override
							public void run() {
								sendData(facts, ip_address);
							}
						});
			}
		};
		
		p2pDir.setupNeighborhood(initLoc, initIP, rmSendListener, neighbors);
		
		p2pDir.start();

		switch(role) {
		case OWNER_ROLE: 
			p2pDir.addStartOwner(reqCode); 
			Log.v(TAG,String.format("Owner started at location %s", local.location));
			break;
		case MEMBER_ROLE: p2pDir.addStartMember(reqCode); break;
	}
		
	}
	
	
	private void setMyLocation(int loc) {
		myLocation = loc;
		for(NodeInfo node: peers) {
			if(node.location == myLocation) {
				updateMyLocation(node);
				return;
			}
		}
	}
	
	private void updateMyLocation(NodeInfo local) {
		setLocalNode(local);
		p2pDir.updateLocation(local.location);
		p2pDir.updateHostAddress(local.ip_address);
		// p2pDir.addStartMember(reqCode);
		setIsConnected(true);
		doConnectionEstablishedActions(local);
		doLocalNodeInfoAvailableActions();
	}

	@Override
	protected void receiveData(List<SerializedFact> facts, InetAddress addr) {
		String output = "";
		for(SerializedFact fact: facts) { output += fact.toString() + " "; }
		Log.v(TAG, String.format("Received from %s: %s", addr.getHostAddress(), output));
		for(SerializedFact fact: facts) {
			if(fact.fact_idx == 7) {
				macHashToIP.put((Integer) fact.arguments[2], addr.getHostAddress());
			}
		}
		p2pDir.addExternalGoals(facts);
	}

	@Override
	protected void handleReceiveException(Exception e) {
		Log.e(TAG, String.format("Error while receiving data: %s", e.toString()));
	}

	@Override
	public boolean connect(int tries, int time_out) {
		Log.v(TAG, "Attempting connection...");

		int count = 0;
		while((count <= tries && !isAccepted()) || tries < 0) {
			Log.v(TAG,String.format("Sending connect message %s to %s", thisDevice.deviceName, ownerMacHash) );
			p2pDir.addConnect(thisDevice.deviceName);
			sleep(time_out);
			count++;
		}
		return false;
	}
	
	@Override
	public void close() {
		p2pDir.addQuit();
		p2pDir.addOneTimeQuiescenceListener(new QuiescenceListener() {
			@Override
			public void performQuiescenceAction(QuiescenceEvent qe) {
				p2pDir.stop_rewrite();
			}
		});
		super.close();
	}

	@Override
	public void onPeersAvailable(WifiP2pDeviceList peers) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onConnectionInfoAvailable(WifiP2pInfo wifiP2pinfo) {
		
	}
	
}
