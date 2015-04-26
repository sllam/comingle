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

package comingle.store;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import comingle.facts.Fact;

public class ConcListStore<F extends Fact> extends Store<F>  {

	private List<F> store;
	
	public ConcListStore() { 
		store = Collections.synchronizedList(new LinkedList<F>());
	}
	
	public void add(F elem) { 
		store.add(elem);
	}

	public void add(F elem, Integer key) { add(elem); }
	
	public void remove(F elem) { 
		elem.set_dead();
	}

	public void remove(int pos) {
		store.get( pos ).set_dead();
	}
	
	public StoreIter<F> lookup_candidates() {
		return new ConcListStoreIter<F>(store);
	}

	public StoreIter<F> lookup_candidates(Integer key) { 
		return lookup_candidates();
	} 
	
	public synchronized void purge() {
		/*
		ListIterator<F> ls = store.listIterator();
		while(ls.hasNext()) {
			if(!ls.next().is_alive()) { ls.remove(); }
		} */
		for(int i=store.size()-1; i>=0; i--) {
			if(!store.get(i).is_alive()) {
				store.remove(i);
			}
		}
	}

	public int actual_size() { return store.size(); }

	public synchronized int size() {
		ListIterator<F> ls = store.listIterator();
		int count = 0;
		while(ls.hasNext()) {
			if(ls.next().is_alive()) { count++; }
		}
		return count;
	}
	
	public List<F> toList() {
		return store;
	}
	
	@Override
	public String toString() {
		String output = ""; // String.format("======== %s ========\n", super.get_name());
		
		synchronized (store) {
			for(int x=0; x<store.size(); x++) {
				F item = store.get(x);
				if (item.is_alive()) {
					output += String.format("%s ", item);
				}
			}
		}
		
		return output;
	}
	
}
