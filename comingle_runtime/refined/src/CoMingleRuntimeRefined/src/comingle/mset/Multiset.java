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

package comingle.mset;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Map.Entry;

public class Multiset<T> implements Collection<T>, Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6478070283347814341L;
	
	protected HashMap<T,Integer> mset;
	protected int total;
	
	public Multiset() {
		mset  = new HashMap<T,Integer>();
		total = 0;
	}
	
	public HashMap<T,Integer> toHashMap() {
		return mset;
	}
	
	public LinkedList<T> toList() {
		LinkedList<T> list = new LinkedList<T>();
		int count = 0;
		for(Entry<T,Integer> entry: mset.entrySet()) {
			T key = entry.getKey();
			int value = entry.getValue();
			for(int x=0; x<value; x++) {
				list.add(key);
			}
		}
		return list;
	}
	
	public ListIterator<T> listiterator() {
		return listiterator(0);
	}
	
	public ListIterator<T> listiterator(int x) {
		return toList().listIterator(0);
	}
	
	public boolean addFirst(T arg0) {
		return add(arg0);
	}
	
	public boolean addLast(T arg0) {
		return add(arg0);
	}
 	
	@Override
	public boolean add(Object arg0) {
		if (mset.containsKey(arg0)) {
			int new_count = mset.get(arg0)+1;
			mset.put((T) arg0, new_count);
		} else {
			mset.put((T) arg0, 1);
		}
		total++;
		return true;
	}

	@Override
	public boolean addAll(Collection arg0) {
		for(Object o: arg0) {
			add(o);
		}
		return true;
	}

	@Override
	public void clear() {
		mset.clear();
		total = 0;
	}

	@Override
	public boolean contains(Object arg0) {
		return mset.containsKey(arg0);
	}

	public boolean subset(Multiset<T> other) {
		HashMap<T,Integer> other_map = other.toHashMap();
		for(Entry<T,Integer> entry :other_map.entrySet()) {
			T key     = entry.getKey();
			int value = entry.getValue();
			if (mset.containsKey(key)) {
				if(mset.get(key) < value) {
					return false;
				}
			} else {
				return false;
			}
		}
		return true;
	}
	
	@Override
	public boolean containsAll(Collection arg0) {
		Multiset<T> other = new Multiset<T>();
		other.addAll(arg0);
		return this.subset(other);
	}

	@Override
	public boolean isEmpty() {
		return total == 0;
	}

	@Override
	public Iterator iterator() {
		return listiterator();
	}

	@Override
	public boolean remove(Object arg0) {
		if(!mset.containsKey(arg0)) { return false; }
		int value = mset.get(arg0);
		if(value > 1) {
			mset.put((T) arg0, value-1);
		} else {
			mset.remove(arg0);
		}
		total--;
		return true;
	}

	@Override
	public boolean removeAll(Collection arg0) {
		boolean modified = false;
		for(Object o: arg0) { modified = remove(o) || modified; }
		return modified;
	}

	@Override
	public boolean retainAll(Collection arg0) {
		boolean modified = false;
		for(T key: mset.keySet()) {
			if(!arg0.contains(key)) {
				mset.remove(arg0);
				modified = true;
			}
		}
		return modified;
	}

	@Override
	public int size() {
		return total;
	}

	@Override
	public Object[] toArray() {
		Object[] arr = new Object[total];
		int count = 0;
		for(Entry<T,Integer> entry: mset.entrySet()) {
			T key = entry.getKey();
			int value = entry.getValue();
			for(int x=0; x<value; x++) {
				arr[count] = key;
				count++;
			}
		}
		return arr;
	}

	@Override
	public Object[] toArray(Object[] arg0) {
		if (arg0.length < total) {
			return toArray();
		}	
		int count = 0;
		for(Entry<T,Integer> entry: mset.entrySet()) {
			T key = entry.getKey();
			int value = entry.getValue();
			for(int x=0; x<value; x++) {
				arg0[count] = key;
				count++;
			}
		}
		while(count < arg0.length) {
			arg0[count] = null;
			count++;
		}
		return arg0;
	}
		
	@Override
	public boolean equals(Object obj) {
		Multiset<T> other = null;
		try {
			other = (Multiset<T>) obj;
		} catch(ClassCastException e) {
			return false;
		}
		if(other.size() != size()) {
			return false;
		}
		HashMap<T,Integer> other_map = other.toHashMap();
		for(Entry<T,Integer> entry: mset.entrySet()) {
			T key = entry.getKey();
			int value = entry.getValue();
			if (other_map.containsKey(key)) {
				if(other_map.get(key) != value) {
					return false;
				}
			} else {
				return false;
			}
		}
		return true;
	}

	private void writeObject(ObjectOutputStream aOutputStream) throws IOException {
		aOutputStream.defaultWriteObject();
	}

	private void readObject(ObjectInputStream aInputStream) throws ClassNotFoundException, IOException {
		aInputStream.defaultReadObject();
	}
	
}
