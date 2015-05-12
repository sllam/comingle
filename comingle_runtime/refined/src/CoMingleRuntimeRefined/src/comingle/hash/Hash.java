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

package comingle.hash;

import java.util.Calendar;
import java.util.LinkedList;

import comingle.mset.SimpMultiset;
import comingle.tuple.*;

public class Hash {

	public static final int MSRE_HASH_SALT = 0;

	public static final int MSRE_PRIME1 = 1319;
	public static final int MSRE_PRIME2 = 7919;
	public static final int MSRE_PRIME3 = 13259;
	public static final int MSRE_PRIME4 = 31547;
	public static final int MSRE_PRIME5 = 53173;
	public static final int MSRE_PRIME6 = 72577;
	public static final int MSRE_PRIME7 = 91099;
	public static final int MSRE_PRIME8 = 103421;
	public static final int MSRE_PRIME9 = 224737;
	public static final int MSRE_PRIME10 = 350377;

	public static int join(int h1, int h2) {
		return h1 * MSRE_PRIME1 + h2 * MSRE_PRIME2;
	} 

	public static int join(int h1, int h2, int h3) {
		return h1 * MSRE_PRIME1 + h2 * MSRE_PRIME2 + h3 * MSRE_PRIME3;
	} 

	public static int join(int h1, int h2, int h3, int h4) {
		return h1 * MSRE_PRIME1 + h2 * MSRE_PRIME2 + h3 * MSRE_PRIME3 + h4 * MSRE_PRIME4;
	} 

	public static int join(int h1, int h2, int h3, int h4, int h5) {
		return h1 * MSRE_PRIME1 + h2 * MSRE_PRIME2 + h3 * MSRE_PRIME3 + h4 * MSRE_PRIME4 + h5 * MSRE_PRIME5;
	} 

	public static int join(int h1, int h2, int h3, int h4, int h5, int h6) {
		return h1 * MSRE_PRIME1 + h2 * MSRE_PRIME2 + h3 * MSRE_PRIME3 + h4 * MSRE_PRIME4 + h5 * MSRE_PRIME5 + h6 * MSRE_PRIME6;
	} 

	public static int hash(int x) { return x + MSRE_HASH_SALT; }

	public static int hash(Integer x) { return x.hashCode() + MSRE_HASH_SALT; }

	public static int hash(String s) { return s.hashCode() + MSRE_HASH_SALT; }

	public static int hash(float f) { return ((int) Math.round(f * 1000001)) + MSRE_HASH_SALT; }

	public static int hash(Float f) { return f.hashCode() + MSRE_HASH_SALT; }

	public static int hash(double d) { return ((int) Math.round(d * 1000001)) + MSRE_HASH_SALT; }

	public static int hash(Double d) { return d.hashCode() + MSRE_HASH_SALT; }

	public static int hash(LinkedList<?> ls) { return ls.hashCode() + MSRE_HASH_SALT; }

	public static int hash(SimpMultiset<?> ms) { return ms.hashCode() + MSRE_HASH_SALT; }
	
	public static int hash(Tuple2<?,?> t) { return join(t.t1.hashCode(),t.t2.hashCode()); }

	public static int hash(Tuple3<?,?,?> t) { return join(t.t1.hashCode(),t.t2.hashCode(),t.t3.hashCode()); }

	public static int hash(Tuple4<?,?,?,?> t) { return join(t.t1.hashCode(),t.t2.hashCode(),t.t3.hashCode(),t.t4.hashCode()); }

	public static int hash(Tuple5<?,?,?,?,?> t) { return join(t.t1.hashCode(),t.t2.hashCode(),t.t3.hashCode(),t.t4.hashCode(),t.t5.hashCode()); }

	public static int hash(Tuple6<?,?,?,?,?,?> t) { 
		return join(t.t1.hashCode(),t.t2.hashCode(),t.t3.hashCode(),t.t4.hashCode(),t.t5.hashCode(),t.t6.hashCode()); 
	}

	public static int hash(Calendar cal) {
		return cal.hashCode();
	}
	
}
