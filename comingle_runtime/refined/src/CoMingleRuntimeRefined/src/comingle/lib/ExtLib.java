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

package comingle.lib;

import java.util.*;

import comingle.mset.SimpMultiset;
import comingle.tuple.*;

public class ExtLib {

	public static <T> int len(LinkedList<T> ls) {
		return ls.size();
	}
	
	public static <T> int size(LinkedList<T> ls) {
		return ls.size();
	}
	
	public static <T> int size(SimpMultiset<T> ls) {
		return ls.size();
	}
	
	public static <T> Tuple2<LinkedList<T>,LinkedList<T> > split(LinkedList<T> ls) {
		LinkedList<T> ls1 = new LinkedList<T>();
		LinkedList<T> ls2 = new LinkedList<T>();
		int ls1_size = ls.size() / 2;
		int count = 0;
		ListIterator<T> it = ls.listIterator(0);
		while(it.hasNext()) {
			if(count < ls1_size) {
				ls1.addLast( it.next() );
			} else {
				ls2.addLast( it.next() );
			}
			count++;
		}
		return new Tuple2<LinkedList<T>,LinkedList<T> >(ls1,ls2);
	}
	
	public static <T> T computemedian(LinkedList<T> ls) {
		int count = ls.size() / 2;
		T curr = null;
		ListIterator<T> it = ls.listIterator(0);
		while(count > 0) {
			curr = it.next();
			count--;
		}
		return curr;
	}
	
	public static <T> T pickone(LinkedList<T> ls) {
		if (ls.size() > 0) {
			return ls.get(0);
		} else {
			return null;
		}
	}

	public static <T> T pickone(SimpMultiset<T> ls) {
		if (ls.size() > 0) {
			return ls.get(0);
		} else {
			return null;
		}
	}
	
	public static <T> LinkedList<Tuple2<T,T> > zip(LinkedList<T> ls1, LinkedList<T> ls2) {
		LinkedList<Tuple2<T,T>> ls = new LinkedList<Tuple2<T,T>>();
		ListIterator<T> it1 = ls1.listIterator(0);
		ListIterator<T> it2 = ls2.listIterator(0);
		while(it1.hasNext() && it2.hasNext()) {
			ls.add( new Tuple2<T,T>(it1.next(),it2.next()) );
		}
		return ls;
	}

	public static <T> boolean in(T t, SimpMultiset<T> ts) {
		return ts.contains(t);
	}
	
	public static <T> boolean in(T t, LinkedList<T> ts) {
		return ts.contains(t);
	}

	public static <T> boolean contains(T t, LinkedList<T> ts) {
		return in(t,ts);
	}
	
	public static <T> LinkedList<T> union_it(LinkedList<T> ts1, LinkedList<T> ts2) {
		LinkedList<T> ts3 = new LinkedList<T>();
		for (T t1: ts1) { ts3.addLast(t1); }
		for (T t2: ts2) { ts3.addLast(t2); }
		return ts3;
	}
	
	public static <T> SimpMultiset<T> union(SimpMultiset<T> ts1, SimpMultiset<T> ts2) {
		SimpMultiset<T> ts3 = new SimpMultiset<T>();
		for (T t1: ts1) { ts3.addLast(t1); }
		for (T t2: ts2) { ts3.addLast(t2); }
		return ts3;
	}
	
	public static <T> LinkedList<T> union(LinkedList<T> ts1, LinkedList<T> ts2) {
		return union_it(ts1,ts2);
	}
	
	public static <T> LinkedList<T> diff(LinkedList<T> ts1, LinkedList<T> ts2) {
		LinkedList<T> ts3 = new LinkedList<T>();
		for (T t1: ts1) {
			if (!ts2.contains(t1)) {
				ts3.addLast(t1);
			}
		}
		return ts3;
	}

	public static <T> SimpMultiset<T> diff(SimpMultiset<T> ts1, SimpMultiset<T> ts2) {
		SimpMultiset<T> ts3 = new SimpMultiset<T>();
		for (T t1: ts1) {
			if (!ts2.contains(t1)) {
				ts3.addLast(t1);
			}
		}
		return ts3;
	}
	
	public static boolean not(boolean b) { return !b; }
	
	public static <T> String format2(String input, T arg) {
		return String.format(input, arg);
	}
	
	public static <T1,T2> String format2(String input, Tuple2<T1,T2> tup) {
		return String.format(input, tup.t1, tup.t2);
	}

	public static <T1,T2,T3> String format2(String input, Tuple3<T1,T2,T3> tup) {
		return String.format(input, tup.t1, tup.t2, tup.t3);
	}

	public static <T1,T2,T3,T4> String format2(String input, Tuple4<T1,T2,T3,T4> tup) {
		return String.format(input, tup.t1, tup.t2, tup.t3, tup.t4);
	}

	public static <T1,T2,T3,T4,T5> String 
	       format2(String input, Tuple5<T1,T2,T3,T4,T5> tup) {
		return String.format(input, tup.t1, tup.t2, tup.t3, tup.t4, tup.t5);
	}

	public static <T1,T2,T3,T4,T5,T6> String 
           format2(String input, Tuple6<T1,T2,T3,T4,T5,T6> tup) {
		return String.format(input, tup.t1, tup.t2, tup.t3, tup.t4, tup.t5, tup.t6);
	}
	
	public static <T> String format(String input, T arg) {
		return String.format(input, arg);
	}
	
	public static <T1,T2> String format(String input, Tuple2<T1,T2> tup) {
		return String.format(input, tup.t1, tup.t2);
	}

	public static <T1,T2,T3> String format(String input, Tuple3<T1,T2,T3> tup) {
		return String.format(input, tup.t1, tup.t2, tup.t3);
	}

	public static <T1,T2,T3,T4> String format(String input, Tuple4<T1,T2,T3,T4> tup) {
		return String.format(input, tup.t1, tup.t2, tup.t3, tup.t4);
	}

	public static <T1,T2,T3,T4,T5> String 
	       format(String input, Tuple5<T1,T2,T3,T4,T5> tup) {
		return String.format(input, tup.t1, tup.t2, tup.t3, tup.t4, tup.t5);
	}

	public static <T1,T2,T3,T4,T5,T6> String 
           format(String input, Tuple6<T1,T2,T3,T4,T5,T6> tup) {
		return String.format(input, tup.t1, tup.t2, tup.t3, tup.t4, tup.t5, tup.t6);
	}
	
	/*
	public static <T> Tuple2<LinkedList<Tuple2<T,T>>,T> makeChain(LinkedList<T> ls) {
		LinkedList<Tuple2<T,T>> new_ls = new LinkedList<Tuple2<T,T>>();
		for(int x=1; x<ls.size(); x++) {
			new_ls.addLast(new Tuple2<T,T>(ls.get(x-1),ls.get(x)));
		}
		T last = null;
		if(ls.size() > 0) {
			last = ls.getLast();
		}
		return new Tuple2<LinkedList<Tuple2<T,T>>,T>(new_ls,last);
	} */
	
	public static <T> Tuple2<SimpMultiset<Tuple2<T,T>>,T> makeChain(T first, LinkedList<T> ls) {
		SimpMultiset<Tuple2<T,T>> new_ls = new SimpMultiset<Tuple2<T,T>>();
		for(int x=1; x<ls.size(); x++) {
			new_ls.addLast(new Tuple2<T,T>(ls.get(x-1),ls.get(x)));
		}
		T last = null;
		if(ls.size() > 0) {
			last = ls.getLast();
		}
		return new Tuple2<SimpMultiset<Tuple2<T,T>>,T>(new_ls,last);
	}
	
	public static boolean stronger(int x, int y) {
		return x > y;
	}
	
	public static <T> LinkedList<T> toList(SimpMultiset mset) {
		return mset.toList();
	}
	
	public static <T> SimpMultiset<T> toMSet(LinkedList<T> list) {
		SimpMultiset<T> mset = new SimpMultiset<T>();
		mset.addAll(list);
		return mset;
	}
	
	public static <T> SimpMultiset<T> mset(LinkedList<T> list) {
		return toMSet(list);
	}
	/*
	public static int mac(String macAddr) {
		StringTokenizer st = new StringTokenizer(macAddr,".");
		int macInt = 0;
		int pos = 0;
		while(st.hasMoreTokens()) {
			
		}
	}*/
	
	public static boolean strongest(String t, SimpMultiset<String> ts) {
		for(String s: ts) {
			int compare = t.compareTo(s);
			if (compare < 0) { return false; }
		}
		return true;
	}
	
}
