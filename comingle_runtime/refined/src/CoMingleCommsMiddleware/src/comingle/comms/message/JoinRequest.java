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

package comingle.comms.message;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class JoinRequest extends Message implements Serializable {
	
	public final String req_code;
	public final String device_name;
	public final String device_id;
	public final String owner_ip;
	
	public JoinRequest(String req_code, String device_name, String device_id, String owner_ip) {
		this.req_code = req_code;
		this.device_name = device_name;
		this.device_id  = device_id;
		this.owner_ip = owner_ip;
	}
	
	public JoinRequest(String req_code, String device_name, String device_id) {
		this(req_code, device_name, device_id, null);
	}
	
	public String toString() {
		return String.format("JoinRequest(%s,%s,%s)", req_code, device_name, device_id);
	}
	
	public void enter(MessageHandler manager) { manager.processMessage(this); }
	
	private static final long serialVersionUID = -1841240056043253796L;
	
	private void writeObject(ObjectOutputStream aOutputStream) throws IOException {
		aOutputStream.defaultWriteObject();
	}

	private void readObject(ObjectInputStream aInputStream) throws ClassNotFoundException, IOException {
		aInputStream.defaultReadObject();
	}
	
}