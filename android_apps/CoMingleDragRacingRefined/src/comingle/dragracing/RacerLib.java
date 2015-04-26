/**
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

CoMingle Version 1.0, Beta Prototype

Authors:
Edmund S. L. Lam      sllam@qatar.cmu.edu
Nabeeha Fatima        nhaque@andrew.cmu.edu

* This implementation was made possible by an JSREP grant (JSREP 4-003-2-001, Effective Distributed 
Programming via Join Patterns with Guards, Propagation and More) from the Qatar National Research Fund 
(a member of the Qatar Foundation). The statements made herein are solely the responsibility of the authors.
**/

package comingle.dragracing;

import java.util.LinkedList;

import comingle.mset.SimpMultiset;
import comingle.tuple.Tuple2;

public class RacerLib {

	public static final int SPEED = 600;
	
	public static int newPos(int pos, int time_start, int time_end) {
		int time_diff = time_end - time_start;
		return pos + (SPEED * time_diff / 1000);
	}
	
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
	
}
