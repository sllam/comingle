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

import java.io.*;

import java.net.ServerSocket;
import java.net.Socket;

import java.util.List;

import comingle.comms.receiver.DataReceivedListener;
import comingle.comms.receiver.ExceptionListener;
import comingle.comms.receiver.ReceiverDaemon;
/**
 * Instance of ReceiverDaemon for standard Socket DataPipes. 
 * 
 * @author Edmund S. L. Lam
 *
 * @param <Data> Type of data that this receiver daemon expects.
 */
public class SocketReceiverDaemon<Data extends Serializable> extends ReceiverDaemon<Data,String> {

	protected int port;
	protected List<DataReceivedListener<Data,String>> data_received_listeners;
	protected ExceptionListener except_listener;
	protected ServerSocket serverSocket;

	/**
	 * Basic constructor
	 * @param port the port number to use for network socket communications.
	 */
	public SocketReceiverDaemon(int port) {
		super();
		this.port = port;
		this.serverSocket = null;
	}

	@Override
	protected void initDaemon() throws Exception {
		serverSocket = new ServerSocket(port);
	}

	@Override
	protected void receiveData() throws Exception {
		Socket client = serverSocket.accept();
		InputStream inputstream = client.getInputStream();
		ObjectInput input = new ObjectInputStream(inputstream);
		castInput( input.readObject(), client.getInetAddress().getHostAddress() );
		input.close();
	}	
	
	/**
	 * Close this receiver daemon.
	 */
	@Override
	public void close() {
		proceed = false;
		if(serverSocket != null) { 
			try {
				serverSocket.close();
			} catch (IOException e) { }
		}
	}
	
}
