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

package comingle.facts;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class SerializedFact extends Fact {

	private static final long serialVersionUID = 8772431864702152019L;

	public int fact_idx;
	public Serializable[] arguments;
	
	public SerializedFact(int loc, int fact_idx, Serializable[] arguments) {
		super(loc);
		this.fact_idx  = fact_idx;
		this.arguments = arguments;
	}
	
	@Override
	public SerializedFact serialize() {
		return this;
	}
	
	private void writeObject(ObjectOutputStream aOutputStream) throws IOException {
		aOutputStream.defaultWriteObject();
	}

	private void readObject(ObjectInputStream aInputStream) throws ClassNotFoundException, IOException {
		aInputStream.defaultReadObject();
	}
	
	@Override
	public String toString() {
		String args = "";
		for (int x=0; x<arguments.length; x++) {
			args += arguments[x].toString();
			if (x < arguments.length-1) {
				args += ",";
			}
		}
		return String.format("fact#%s(%s)", fact_idx, args);
	}
	
}
