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

import java.io.*;

public abstract class Fact implements Serializable {

	private static final long serialVersionUID = 6110921483471399271L;

	protected static int fact_id = 0;

	/**
	 * Unique id, representing the fact
	 */
	protected int id;
	/**
	 * Liveness flag, set to true iff fact is still valid.
	 */
	public boolean alive;
	public int loc;
	public int priority;

	
	/**
	 * @param loc
	 */
	public Fact(int loc) {
		alive = true;
		this.loc = loc;
		id = fact_id;
		fact_id += 1;
		priority = 1;
	}

	public void set_priority(int p) { priority = p; }

	public int get_loc() { return loc; }

	public int fact_idx() { return 0; }

	public boolean is_alive() { return alive; }

	public void set_dead() { alive = false; }

	public int identity() { return id; }

	public boolean equals(Fact f) { return id == f.identity(); }

	public SerializedFact serialize() { return null; }
	
	public void runSideEffect() {  }

	private void writeObject(ObjectOutputStream aOutputStream) throws IOException {
		aOutputStream.defaultWriteObject();
	}

	private void readObject(ObjectInputStream aInputStream) throws ClassNotFoundException, IOException {
		aInputStream.defaultReadObject();
	}

}

