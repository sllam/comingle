package p2pdirectory;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import comingle.actuation.ActuatorAction;
import comingle.facts.SerializedFact;
import comingle.mset.SimpMultiset;
import comingle.nodes.SendListener;
import comingle.rewrite.QuiescenceEvent;
import comingle.rewrite.QuiescenceListener;
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
	
	public static int ownerLoc(int i) { return OWNER_LOC;  }
	
	private final static Map<String,String> macToIP = new HashMap<String,String>();
	private static List<NodeInfo> nodeDirList = null;
	
	public static String lookupIP(String mac) {
		return macToIP.get(mac);
	}
	
	private static void setNodeDirList(List<NodeInfo> nodes) {
		nodeDirList = nodes;
	}
	
	public static SimpMultiset<Tuple4<Integer,String,String,String>> retrieveDir(int i) {
		SimpMultiset<Tuple4<Integer,String,String,String>> mset = new SimpMultiset<Tuple4<Integer,String,String,String>>();
		synchronized(nodeDirList) {
			for(NodeInfo node: nodeDirList) {
				mset.add( new Tuple4<Integer,String,String,String>(node.location, node.ip_address, node.name, node.mac) );
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
		
		p2pDir.setAddedActuator(new ActuatorAction<Tuple4<Integer,String,String,String>>() {
			@Override
			public void doAction(Tuple4<Integer, String, String, String> tup) {
				int loc       = tup.t1; 
				String ipAddr = tup.t2;
				String name   = tup.t3;
				String mac    = tup.t4;
				String role   = showRole(locRole(loc));
				NodeInfo newNode = new NodeInfo(loc, ipAddr, name, mac, role);
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
		
		p2pDir.setYouActuator(new ActuatorAction<Integer>() {
			@Override
			public void doAction(Integer myLocation) {
				// NodeInfo myNode = getNode(myLocation);
				// setLocalNode(myNode);
				// setIsConnected(true);
				setAccepted();
				// doConnectionEstablishedActions(myNode);
				setMyLocation(myLocation);
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
		NodeInfo ownerInfo = new NodeInfo(OWNER_LOC, OWNER_IP, ownerDevice.deviceName, ownerDevice.deviceAddress
				                         ,showRole(OWNER_ROLE));
		addNode(ownerInfo);
		
		int initLoc   = DEFAULT_LOCATION;
		String initIP = DEFAULT_IP;
		if(isOwner()) {
			initLoc = OWNER_LOC;
			initIP  = ownerIP;
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
		case OWNER_ROLE: p2pDir.addStartOwner(reqCode); break;
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
		p2pDir.addStartMember(reqCode);
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
			if(fact.fact_idx == 9) {
				macToIP.put((String) fact.arguments[2], addr.getHostAddress());
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
			Log.v(TAG,String.format("Sending connect message: (%s,%s)", thisDevice.deviceName, thisDevice.deviceAddress) );
			p2pDir.addConnect(thisDevice.deviceName, thisDevice.deviceAddress);
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
