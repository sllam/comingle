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

package comingle.comms.sockets;

import java.util.List;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.net.Socket;

import comingle.comms.datapipe.DataPipe;

/**
 * This class facilitates a bi-directional communication pipeline through network sockets.
 * Data is sent through network sockets created internally,
 * while data is received by an instance of the SocketReceiverDaemon. 
 * 
 * @author Edmund S. L. Lam
 *  
 * @param <D> The type of data that the DataPipe instance communicates.
 */
public class SocketDataPipe<Data extends Serializable> extends DataPipe<Data, String> {

	protected static final int SOCKET_TIMEOUT = 5000;
	
	protected int port;
	protected boolean started;

	/**
	 * Basic constructor for Datapipes
	 * @param port the port number to use 
	 */
	public SocketDataPipe(int port) {
		this.port = port;
		started = false;
		setReceiverDaemon(new SocketReceiverDaemon<Data>(port));
	}


	
	/**
	 * Initializes the data pipe by starting a thread to run the
	 * receiver daemon routines.
	 */
	@Override
	public void initReceiver() { 
		if(!started) {
			receiver.start();
			started = true;
		}
	}

	/**
	 * Closes this data pipe.
	 */
	@Override
	public void close() {
		receiver.close();
	}
	
	protected Socket connect(String ip_address) {
		Socket socket = new Socket();
		try {
			socket.bind(null);
			socket.connect(new InetSocketAddress(ip_address, port), SOCKET_TIMEOUT);
			return socket;
		} catch(Exception e) {
			if(connect_except_listener != null) {
				connect_except_listener.performExceptionAction("Connecting to Socket", e);
			}
			return null;
		}
	}
	
	protected boolean send(List<Data> data_list, Socket socket) {
		try {
			OutputStream stream = socket.getOutputStream();
			ObjectOutput output = new ObjectOutputStream(stream);
			output.writeObject( (Serializable) data_list );
			output.flush();
			output.close();
			return true;
		} catch (Exception e) {
			if(send_except_listener != null) {
				send_except_listener.performExceptionAction("Sending Data", e);
			}
			return false;
		}
	}

	protected void close(Socket socket) {
		if (socket.isConnected()) {
			try {
				socket.close();
			} catch (IOException e) {

				if(connect_except_listener != null) {
					connect_except_listener.performExceptionAction("Closing Socket", e);
				}
			}
		}
	}
	
	/**
	 * Send data_list to a specified IP address. Opens and closes a new socket
	 * at given ip address
	 * @param data_list the list of data to send.
	 * @param ip_address the destination IP address.
	 */
	@Override
	public void sendData(List<Data> data_list, String ip_address) {
		Socket socket = connect(ip_address);
		if (socket != null) {
			send(data_list, socket);
			close(socket); 
		}		
	}

}
