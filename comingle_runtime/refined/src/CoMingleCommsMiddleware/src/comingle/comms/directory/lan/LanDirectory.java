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

package comingle.comms.directory.lan;

import java.util.LinkedList;
import java.util.List;

import comingle.comms.directory.BaseDirectory;
import comingle.comms.directory.NodeInfo;
import comingle.comms.message.Accept;
import comingle.comms.message.JoinRequest;
import comingle.comms.message.Message;
import comingle.comms.message.MessageHandler;
import comingle.comms.message.Ping;
import comingle.comms.message.Pong;
import comingle.comms.message.Quit;
import comingle.comms.message.Reject;
import comingle.comms.message.Service;
import comingle.comms.message.Update;
import comingle.comms.multicast.MulticastDataPipe;
import comingle.comms.receiver.DataReceivedListener;
import comingle.comms.receiver.ExceptionListener;
import comingle.comms.sockets.SocketDataPipe;

/**
 * 
 * An instance of a base directory that establishes and maintains a Local-Area-Network based directory.
 * This directory utilizes a multicast IP address for broadcasting and establishing service request between owner and member,
 * while establishing and maintaining a directory of standard IP address of each participant of the directory, for P2P 
 * messaging. 
 * 
 * @author Edmund S.L. Lam
 *
 */
abstract public class LanDirectory extends BaseDirectory<Message> implements MessageHandler {

	public final static String DEFAULT_MULTICAST_ADDR = "224.0.0.3";
	public final static int DEFAULT_MULTICAST_PORT = 8888;

	public final static int LAN_NETWORK_ID = 101;
	public final static String OWNER_IP = "127.0.0.1";
	
	protected MulticastDataPipe multicastPipe;
	protected boolean ownerIPAvailable = false;
	
	/**
	 * Basic Constructor
	 * @param multicastAddr multicast address of the directory
	 * @param multicastPort multicast port number of the directory
	 * @param p2pPort standard P2P address port number
	 * @param reqCode request code of this directory service
 	 * @param localDeviceID device ID that uniquely identifies device running this directory
	 */
	public LanDirectory(String multicastAddr, int multicastPort, int p2pPort, String reqCode, String localDeviceID) {
		super(new SocketDataPipe<Message>(p2pPort), reqCode, LAN_NETWORK_ID, localDeviceID);
		multicastPipe = new MulticastDataPipe(multicastAddr, multicastPort, 256);
		setOwnerIP(OWNER_IP);
	}
	
	/**
	 * Default Lan directory constructor. Sets multicast address to 224.0.0.3 and multicast port to 8888
	 * @param p2pPort standard P2P address port number
	 * @param reqCode request code of this directory service
	 * @param localDeviceID device ID that uniquely identifies device running this directory
	 */
	public LanDirectory(int p2pPort, String reqCode, String localDeviceID) {
		this(DEFAULT_MULTICAST_ADDR, DEFAULT_MULTICAST_PORT, p2pPort, reqCode, localDeviceID);
	}
	
	/**
	 * Default Lan directory constructor. Sets multicast address to 224.0.0.3 and multicast port to 8888. Also sets
	 * request code to null
	 * @param p2pPort standard P2P address port number
	 * @param localDeviceID device ID that uniquely identities device running this directory
	 */
	public LanDirectory(int p2pPort, String localDeviceID) {
		this(DEFAULT_MULTICAST_ADDR, DEFAULT_MULTICAST_PORT, p2pPort, null, localDeviceID);		
	}

	@Override
	public void processMessage(Service service) {
		this.setAccepted();
		JoinRequest msg = new JoinRequest(this.reqCode, getDisplayName(), getDeviceID(), service.ipAddress); 
		this.ownerIP = service.ipAddress;
		this.sendData(msg, this.ownerIP);
	}
	
	@Override
	public synchronized void processMessage(JoinRequest join) {
		if( !this.isOwner() ) { return; }
		if( this.reqCode.equalsIgnoreCase( join.req_code ) ) {
			if(!hasNode(join.ipAddress)) {
				if(!ownerIPAvailable) {
					this.ownerIP = join.owner_ip;
					local.setIPAddress(this.ownerIP);
					this.doLocalNodeInfoAvailableActions();
					ownerIPAvailable = true;
				}
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
	public void setOwnerRole() {
		super.setOwnerRole();
		multicastPipe.addDataReceivedListener(new DataReceivedListener<String,String>() {
			@Override
			public void performDataReceivedAction(List<String> data_list, String addr) {
					String data = data_list.get(0);
					char   header = data.charAt(0);
					String receivedReqCode = data.substring(2);
					switch(header) {
					case 'R' :
						if(receivedReqCode.equals(reqCode)) {
							log( String.format("Received valid connect code from %s", addr) );
							sendData(new Service(local), addr);
						} else {
							log( String.format("Received invalid connect code (\"%s\") from %s", receivedReqCode, addr) );
							// sendData(new Reject(Reject.REQ_CODE_MISMATCH), addr);
						}
						return;
					case 'I' :
						if(receivedReqCode.equals(reqCode)) {
							log( String.format("Received valid IP code from %s", addr) );
							setOwnerIP(addr);
							establishRole();
						} else {
							log( String.format("Received invalid IP code (\"%s\") from %s", receivedReqCode, addr) );
						}
						return;
					}
					
			}
		});
		multicastPipe.setExceptionListener(new ExceptionListener() {
			@Override
			public void performExceptionAction(String task, Exception e) {
				err(task + ": " + e.toString());				
			}
		});
		multicastPipe.initReceiver();
		// multicastPipe.sendDataThreaded(String.format("I:%s", reqCode), "");
		log("Multicast pipe initialized");
	}
	
	@Override
	public void serve() {
		
	}

	@Override
	public boolean connect(int tries, int time_out) {
		log(String.format("Attempting to connect to %s, for %s times, %s ms apart.", reqCode, tries, time_out));
		int count = 0;
		if(tries < 0) { tries = 10000; }
		while(count <= tries && !isAccepted() && !isRejected() && !isClosing()) {
			// log("calling send message...");
			multicastPipe.sendData(String.format("R:%s",reqCode), "");
			this.sleep(time_out);
			count++;
		}
		if(isRejected() || isClosing()) { return false; }
		else { return isAccepted(); }
	}

	@Override
	protected void receiveData(List<Message> data_list, String addr) {
		for(Message msg: data_list) {
			log(String.format("%s received from %s", msg, addr));
			msg.setIPAddress(addr);
			msg.enter(this);
		}
	}

	@Override
	protected void handleReceiveException(String task, Exception e) {
		err(task + ": " + e.toString());
	}


}
