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

package comingle.tuple;

public class Tuples {

	public static <T1,T2> Tuple2<T1,T2> make_tuple(T1 t1, T2 t2) {
		return new Tuple2<T1,T2>(t1,t2);
	}

	public static <T1,T2,T3> Tuple3<T1,T2,T3> make_tuple(T1 t1, T2 t2, T3 t3) {
		return new Tuple3<T1,T2,T3>(t1,t2,t3);
	}

	public static <T1,T2,T3,T4> Tuple4<T1,T2,T3,T4> make_tuple(T1 t1, T2 t2, T3 t3, T4 t4) {
		return new Tuple4<T1,T2,T3,T4>(t1,t2,t3,t4);
	}

	public static <T1,T2,T3,T4,T5> Tuple5<T1,T2,T3,T4,T5> make_tuple(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5) {
		return new Tuple5<T1,T2,T3,T4,T5>(t1,t2,t3,t4,t5);
	}

	public static <T1,T2,T3,T4,T5,T6> Tuple6<T1,T2,T3,T4,T5,T6> make_tuple(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6) {
		return new Tuple6<T1,T2,T3,T4,T5,T6>(t1,t2,t3,t4,t5,t6);
	}

}
