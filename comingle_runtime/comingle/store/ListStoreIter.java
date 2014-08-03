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

CoMingle Version 0.8, Prototype Alpha

Authors:
Edmund S. L. Lam      sllam@qatar.cmu.edu
Nabeeha Fatima        nhaque@andrew.cmu.edu

* This implementation was made possible by an JSREP grant (JSREP 4-003-2-001, Effective Distributed 
Programming via Join Patterns with Guards, Propagation and More) from the Qatar National Research Fund 
(a member of the Qatar Foundation). The statements made herein are solely the responsibility of the authors.
*/

package comingle.store;

import java.util.LinkedList;
import java.util.ListIterator;

import comingle.facts.Fact;

public class ListStoreIter<F extends Fact> implements StoreIter<F> {

	private LinkedList<F> store;
	private ListIterator<F> iter;

	public ListStoreIter() {
		store = new LinkedList<F>();
		iter  = store.listIterator();
	}

	public ListStoreIter(LinkedList<F> st) {
		store = st;
		iter  = st.listIterator();
	}

	public void add(F f) {
		store.add(f);
	}

	public void init_iter() { 
		iter = store.listIterator();
	}

	public boolean contains(F elem) { 
		ListIterator<F> local_iter = store.listIterator();
		while(local_iter.hasNext()) {
			F cons = local_iter.next();
			if(elem.equals(cons)) { return true; }
		}
		return false;
	}

	public F get_next_alive() { 
		while(iter.hasNext()) {
			F cons = iter.next();
			if(cons.is_alive()) { 
				return cons; 
			} 
		}
		return null;
	}

	public F get_next() {
		if(iter.hasNext()) {
			return iter.next();
		}
		return null;
	}

}

