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

public class SimpMultiset<T> implements Collection<T>, Serializable {

	
	/**
	 * 
	 */
	private static final long serialVersionUID = 2097982476854665798L;
	protected LinkedList<T> mset;
	
	public SimpMultiset() {
		mset  = new LinkedList<T>();
	}
	
	public int count(T other) {
		int amount = 0;
		for (T item: mset) {
			if (item.equals(other)) {
				amount++;
			}
		}
		return amount;
	}
	
	public LinkedList<T> toList() {
		return mset;
	}
	
	public ListIterator<T> listIterator() {
		return listIterator(0);
	}
	
	public ListIterator<T> listIterator(int x) {
		return mset.listIterator(0);
	}
	
	public ListIterator<T> listiterator() {
		return listiterator(0);
	}
	
	public ListIterator<T> listiterator(int x) {
		return mset.listIterator(0);
	}
	
	public boolean addFirst(T arg0) {
		mset.addFirst(arg0);
		return true;
	}
	
	public boolean addLast(T arg0) {
		mset.addLast(arg0);
		return true;
	}
	
	public T get(int x) {
		return mset.get(x);
	}
 	
	@Override
	public boolean add(Object arg0) {
		return mset.add((T) arg0);
	}

	@Override
	public boolean addAll(Collection arg0) {
		return mset.addAll(arg0);
	}

	@Override
	public void clear() {
		mset.clear();
	}

	@Override
	public boolean contains(Object arg0) {
		return mset.contains(arg0);
	}
	
	public boolean subset(SimpMultiset<T> other) {
		LinkedList<T> other_mset = other.toList();
		for(T other_item: other_mset) {
			if (other.count(other_item) > count(other_item)) {
				return false;
			}
		}
		return true;
	}
	
	@Override
	public boolean containsAll(Collection arg0) {
		SimpMultiset<T> other = new SimpMultiset<T>();
		other.addAll(arg0);
		return this.subset(other);
	}

	@Override
	public boolean isEmpty() {
		return mset.isEmpty();
	}

	@Override
	public Iterator iterator() {
		return listiterator();
	}

	@Override
	public boolean remove(Object arg0) {
		return mset.remove(arg0);
	}

	@Override
	public boolean removeAll(Collection arg0) {
		return mset.removeAll(arg0);
	}

	@Override
	public boolean retainAll(Collection arg0) {
		return mset.retainAll(arg0);
	}

	@Override
	public int size() {
		return mset.size();
	}

	@Override
	public Object[] toArray() {
		return mset.toArray();
	}

	@Override
	public Object[] toArray(Object[] arg0) {
		return mset.toArray(arg0);
	}
		
	@Override
	public boolean equals(Object obj) {
		SimpMultiset<T> other = null;
		try {
			other = (SimpMultiset<T>) obj;
		} catch(ClassCastException e) {
			return false;
		}
		if(other.size() != size()) {
			return false;
		}
		return this.subset(other) && other.subset(this);
	}

	private void writeObject(ObjectOutputStream aOutputStream) throws IOException {
		aOutputStream.defaultWriteObject();
	}

	private void readObject(ObjectInputStream aInputStream) throws ClassNotFoundException, IOException {
		aInputStream.defaultReadObject();
	}
	
}
