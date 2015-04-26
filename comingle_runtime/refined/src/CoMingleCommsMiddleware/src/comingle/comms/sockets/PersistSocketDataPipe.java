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

import java.io.Serializable;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;

/**
 * 
 * Similar to SocketDataPipe, but this alternative implementation maintains open sockets to previously 
 * connected addresses. All sockets are closed only when close() is called.
 * 
 * @author Edmund S.L. Lam
 *
 * @param <D> type of data this socket sends and receives
 */
public class PersistSocketDataPipe<D extends Serializable> extends SocketDataPipe<D> {

	protected HashMap<String,Socket> opened_sockets;
	
	public PersistSocketDataPipe(int port) {
		super(port);
		opened_sockets = new HashMap<String,Socket>();
	}
	
	@Override
	public synchronized void close() {
		super.close();
		for (Socket socket: opened_sockets.values()) {
			close(socket);
		}
	}
	
	@Override
	public synchronized void sendData(List<D> data_list, String ip_address) {
		Socket socket;
		if (opened_sockets.containsKey(ip_address)) {
			socket = opened_sockets.get(ip_address);
		} else {
			socket = connect(ip_address);
			opened_sockets.put(ip_address, socket);
		}
		send(data_list, socket);
	}
	
}
