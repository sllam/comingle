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
import java.util.Iterator;
import java.util.HashMap;
import java.util.Set;

import comingle.facts.Fact;

public class MultiMapStore<F extends Fact> extends Store<F> {

	private HashMap<Integer,LinkedList<F>> store;
	private int count;

	public MultiMapStore() {
		store = new HashMap<Integer,LinkedList<F>>();
	}

	public void add(F elem) { 
		add(elem,-1);
	}

	public void add(F elem, Integer key) { 
		count++;
		if(store.containsKey(key)) {
			LinkedList<F> ls = store.get(key);
			ls.add( elem );
		} else {
			LinkedList<F> ls = new LinkedList<F>();
			ls.add( elem );
			store.put(key, ls);
		}
	}

	public void remove(F elem) { 
		elem.set_dead();
	}

	public StoreIter<F> lookup_candidates() {
		return lookup_candidates(-1);
	}

	public StoreIter<F> lookup_candidates(Integer key) { 
		if(store.containsKey(key)) {
			return new ListStoreIter<F>(store.get(key));
		} else {
			return new EmptyStoreIter<F>();
		}
	} 

	public void purge() {
		Iterator<Integer> keys = store.keySet().iterator();
		while(keys.hasNext()) {
			ListIterator<F> ls = store.get(keys.next()).listIterator();
			while(ls.hasNext()) {
				if(!ls.next().is_alive()) { 
					ls.remove(); 
					count--;
				}
			}
		}
	}

	public int actual_size() { return count; }

	public int size() {
		int lcount = 0;
		Iterator<Integer> keys = store.keySet().iterator();
		while(keys.hasNext()) {
			ListIterator<F> ls = store.get(keys.next()).listIterator();
			while(ls.hasNext()) {
				if(ls.next().is_alive()) { lcount++; }
			}
		}
		return lcount;
	}

	public String toString() {
		String output = ""; // String.format("======== %s ========\n", super.get_name());

		Iterator<Integer> keys = store.keySet().iterator();
		while(keys.hasNext()) {
			Integer key = keys.next();
			ListIterator<F> ls = store.get(key).listIterator();
			// output += String.format("\n%s -> ", key);
			while(ls.hasNext()) {
				Fact cons = ls.next();
				if(cons.is_alive()) { 
					output += String.format("%s ", cons);
				}
			}
		}

		// output += "\n==========================";
		return output;
	}


}
