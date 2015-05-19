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

package comingle.misc;

import java.util.Collection;
import java.util.LinkedList;

import comingle.mset.Multiset;
import comingle.mset.SimpMultiset;

public class Misc {

	public static <T> LinkedList<T> to_list(T[] arr) {
		LinkedList<T> ls = new LinkedList<T>();
		for (int x=0; x<arr.length; x++) {
			ls.add( arr[x] );
		}
		return ls;
	}

	public static <T> SimpMultiset<T> to_mset(T[] arr) {
		SimpMultiset<T> ls = new SimpMultiset<T>();
		for (int x=0; x<arr.length; x++) {
			ls.add( arr[x] );
		}
		return ls;
	}
	
	public static <T> LinkedList<T> to_list(Collection<T> cs) {
		LinkedList<T> ls = new LinkedList<T>();
		for (T c: cs) {
			ls.add(c);
		}
		return ls;
	}
	
	public static <T> SimpMultiset<T> to_mset(Collection<T> cs) {
		SimpMultiset<T> ls = new SimpMultiset<T>();
		for (T c: cs) {
			ls.add(c);
		}
		return ls;
	}
	
}
