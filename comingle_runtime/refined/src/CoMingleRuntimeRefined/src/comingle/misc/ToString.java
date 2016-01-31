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

import java.io.File;
import java.util.LinkedList;
import java.util.ListIterator;

import comingle.mset.Multiset;
import comingle.mset.SimpMultiset;
import comingle.tuple.*;

public class ToString {

	public static String to_str(int i) { return String.format("%s", i); }

	public static String to_str(char c) { return String.format("%s", c); }

	public static String to_str(float f) { return String.format("%s", f); }

	public static String to_str(double d) { return String.format("%s", d); }

	public static String to_str(boolean b) { return String.format("%s", b); }

	public static String to_str(Integer i) { return String.format("%s", i); }

	public static String to_str(Character c) { return String.format("%s", c); }

	public static String to_str(Float f) { return String.format("%s", f); }

	public static String to_str(Double d) { return String.format("%s", d); }

	public static String to_str(Boolean b) { return String.format("%s", b); }

	public static String to_str(File f) { return String.format("%s", f); }
	
	public static String to_str(Object o) { return String.format("%s", o); }
	 
	public static String to_str(LinkedList<?> ls) {
		ListIterator<?> it = ls.listIterator();
		String output = "[";
		boolean init = true;
		while(it.hasNext()) {
			if(init) {
				output += String.format("%s", it.next());
				init = false;
			} else {
				output += String.format(" --> %s", it.next());
			}
		}
		output += "]";
		return output;
	}
	
	public static <T> String to_str(SimpMultiset<T> mset) {
		ListIterator<T> list = mset.listiterator();
		String output = "{ ";
		while (list.hasNext()) {
			output += String.format("%s", list.next());
			if (list.hasNext()) {
				output += ", ";
			}
		}
		output += " }";
		return output;
	}

	public static String to_str(Tuple2<?,?> t) { return String.format("%s", t); }

	public static String to_str(Tuple3<?,?,?> t) { return String.format("%s", t); }

	public static String to_str(Tuple4<?,?,?,?> t) { return String.format("%s", t); }

	public static String to_str(Tuple5<?,?,?,?,?> t) { return String.format("%s", t); }

	public static String to_str(Tuple6<?,?,?,?,?,?> t) { return String.format("%s", t); }

}
