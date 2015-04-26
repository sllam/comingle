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
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.LinkedList;
import java.util.List;

import comingle.comms.receiver.ReceiverDaemon;

/**
 * 
 * Instance of ReceiverDaemon for the MulticastDataPipe. Receives String data from the given
 * multicast address and port.
 * 
 * @author Edmund S.L. Lam
 *
 */
public class MulticastReceiverDaemon extends ReceiverDaemon<String, String> {

	protected String multicastAddr;
	protected int port;
	protected byte[] buffer;
	protected MulticastSocket clientSocket;
	
	public MulticastReceiverDaemon(String multicastAddr, int port, int bufferSize) {
		this.multicastAddr = multicastAddr;
		this.port = port;
        this.buffer = new byte[bufferSize];
        this.clientSocket = null;
	}
	
	@Override
	protected void initDaemon() throws Exception {
		InetAddress address = InetAddress.getByName(multicastAddr);
		clientSocket = new MulticastSocket(port);
        clientSocket.joinGroup(address);
	}

	@Override
	protected void receiveData() throws Exception {
        DatagramPacket msgPacket = new DatagramPacket(buffer, buffer.length);
        clientSocket.receive(msgPacket);
        String msg  = new String(buffer, 0, msgPacket.getLength());// buffer.length);
        String addr = msgPacket.getAddress().getHostAddress();
        List<String> data_list = new LinkedList<String>();
        data_list.add(msg);
		performDataReceivedActions(data_list, addr);
	}

	@Override
	public void close() {
		clientSocket.close();
	}

}
