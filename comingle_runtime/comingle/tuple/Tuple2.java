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

public class Tuple2<T1,T2> {

	public T1 t1;
	public T2 t2;

	public Tuple2(T1 t1, T2 t2) { this.t1 = t1; this.t2 = t2; }

	public boolean equals(Tuple2<T1,T2> other) {
		return t1.equals(other.t1) && t2.equals(other.t2);
	}

	public String toString() {
		return String.format("(%s,%s)",t1,t2);
	}

}
