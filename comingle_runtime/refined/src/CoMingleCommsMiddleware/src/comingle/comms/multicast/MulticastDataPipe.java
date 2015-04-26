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

package comingle.comms.multicast;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.List;

import comingle.comms.datapipe.DataPipe;

/**
 * 
 * An instance of a DataPipe that utilizes the multicast IP channel to broadcast String messages to
 * any other DataPipes listening on the same multicast IP and port.
 * 
 * @author Edmund S.L. Lam
 *
 */
public class MulticastDataPipe extends DataPipe<String, String> {

	protected static final int SOCKET_TIMEOUT = 5000;

    public final static String DEFAULT_MULTICAST_ADDR = "224.0.0.3";
	
	protected String multicastAddr;
	protected int port;
	protected byte[] buffer;
	protected DatagramSocket senderSocket;
	protected boolean started;
	
	/**
	 * Basic Constructor
	 * @param multicastAddr multicast address that this DataPipe communicates on.
	 * @param port port number that this DataPipe communicates on.
	 * @param bufferSize maximum size of messages sent and received by this DataPipe
	 */
	public MulticastDataPipe(String multicastAddr, int port, int bufferSize) {
		this.multicastAddr = multicastAddr;
		this.port = port;
        this.buffer = new byte[bufferSize];
		this.senderSocket = null;
		this.started = false;
		this.setReceiverDaemon(new MulticastReceiverDaemon(multicastAddr, port, bufferSize));
	}
	/**
	 * Default constructor. Sets multicast address to 224.0.0.3
	 * @param port port number that this DataPipe communicates on.
	 * @param bufferSize maximum size of messages sent and received by this DataPipe
	 */
	public MulticastDataPipe(int port, int bufferSize) {
		this(DEFAULT_MULTICAST_ADDR, port, bufferSize);
	}
	
	/**
	 * Initialize receiver daemon
	 */
	@Override
	public void initReceiver() { 
		if(!started) {
			receiver.start();
			started = true;
		}
	}

	/**
	 * Close this DataPipe
	 */
	@Override
	public void close() {
		receiver.close();
	}

	protected void close(DatagramSocket socket) {
		if (socket.isConnected()) {
			try {
				socket.close();
			} catch (Exception e) {
				if(connect_except_listener != null) {
					connect_except_listener.performExceptionAction(e);
				}
			}
		}
	}
	
	/**
	 * Broadcasts a list of String messages. Input argument ip_address is ignored, since
	 * this is a broadcast and has no destination point.
	 */
	@Override
	public void sendData(List<String> data_list, String ip_address) {
		InetAddress address;
		DatagramSocket socket;
		try {
			address = InetAddress.getByName(multicastAddr);
			socket  = new DatagramSocket();
		} catch(Exception e) {
			if(connect_except_listener != null) {
				connect_except_listener.performExceptionAction(e);
			}
			return;
		}
		try {
			for(String data: data_list) {
				DatagramPacket msgPacket = new DatagramPacket(data.getBytes(), data.getBytes().length, address, port);
				socket.send(msgPacket);
			}
		} catch(Exception e) {
			close(socket);
			if(send_except_listener != null) {
				send_except_listener.performExceptionAction(e);
			}
			return;
		}
		close(socket);
	}

	
	
}
