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

package comingle.goals;

import java.util.List;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Collections;

import comingle.facts.Fact;

public class ListGoals<F extends Fact> extends Goals<F> {

	private List<F> goals;

	public ListGoals() { 
		goals = Collections.synchronizedList(new LinkedList<F>());
		// goals = new LinkedList<F>();
	}

	@Override
	public synchronized F next() {
		if(goals.size() > 0) {
			// return goals.pop();
			F f = goals.get(0);
			goals.remove(0);
			return f;
		} else {
			return null;
		}
	}

	@Override
	public synchronized boolean has_goals() { return goals.size() > 0; }

	@Override
	public synchronized void add(F f) { 
		// goals.push(f); 
		goals.add(0, f);
	}

	@Override
	public synchronized void add(F f, int p) { add(f); }

	@Override
	public synchronized List<F> toList() { return goals; }

	public String toString() {
		String output = ""; // "========== List Goals ==========\n";
		ListIterator<F> ls = goals.listIterator();
		while(ls.hasNext()) {
			output += String.format("%s ",ls.next());
		}
		// output += "\n==================================";
		return output;
	}

}
