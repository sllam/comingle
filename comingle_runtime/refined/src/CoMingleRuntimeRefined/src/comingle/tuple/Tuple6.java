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

package comingle.tuple;

public class Tuple6<T1,T2,T3,T4,T5,T6> {

	public T1 t1;
	public T2 t2;
	public T3 t3;
	public T4 t4;
	public T5 t5;
	public T6 t6;

	public Tuple6(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6) { 
		this.t1 = t1; 
		this.t2 = t2; 
		this.t3 = t3; 
		this.t4 = t4;
		this.t5 = t5;
		this.t6 = t6;
	}

	public boolean equals(Tuple6<T1,T2,T3,T4,T5,T6> other) {
		return t1.equals(other.t1) && t2.equals(other.t2) && t3.equals(other.t3) && t4.equals(other.t4) && t5.equals(other.t5) && t6.equals(other.t6);
	}

	public String toString() {
		return String.format("(%s,%s,%s,%s,%s,%s)",t1,t2,t3,t4,t5,t6);
	}

}
