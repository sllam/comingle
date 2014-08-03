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

package comingle.misc;

import java.util.LinkedList;
import java.util.ListIterator;

import comingle.tuple.*;

public class Equality {

	public static boolean is_eq(int i, int j) { return i == j; }

	public static boolean is_eq(char i, char j) { return i == j; }

	public static boolean is_eq(float i, float j) { return i == j; }

	public static boolean is_eq(double i, double j) { return i == j; }

	public static boolean is_eq(boolean i, boolean j) { return i == j; }

	public static boolean is_eq(Integer i, Integer j) { return i.equals(j); }

	public static boolean is_eq(Character i, Character j) { return i.equals(j); }

	public static boolean is_eq(Float i, Float j) { return i.equals(j); }

	public static boolean is_eq(Double i, Double j) { return i.equals(j); }

	public static boolean is_eq(Boolean i, Boolean j) { return i.equals(j); }

	public static boolean is_eq(LinkedList<?> ls1, LinkedList<?> ls2) {
		if (ls1.size() == ls2.size()) {
			ListIterator<?> it1 = ls1.listIterator();
			ListIterator<?> it2 = ls2.listIterator();
			while(it1.hasNext()) {
				if (!it1.next().equals(it2.next())) {
					return false;
				}
			}
			return true;
		} else {
			return false;
		}
	}

	public static boolean is_eq(Tuple2<?,?> i, Tuple2<?,?> j) { return i.equals(j); }

	public static boolean is_eq(Tuple3<?,?,?> i, Tuple3<?,?,?> j) { return i.equals(j); }

	public static boolean is_eq(Tuple4<?,?,?,?> i, Tuple4<?,?,?,?> j) { return i.equals(j); }

	public static boolean is_eq(Tuple5<?,?,?,?,?> i, Tuple5<?,?,?,?,?> j) { return i.equals(j); }

	public static boolean is_eq(Tuple6<?,?,?,?,?,?> i, Tuple6<?,?,?,?,?,?> j) { return i.equals(j); }

}
