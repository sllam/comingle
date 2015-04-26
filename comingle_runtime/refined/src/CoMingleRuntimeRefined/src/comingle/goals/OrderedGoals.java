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

import java.util.PriorityQueue;
import java.util.List;
import java.util.LinkedList;
import java.util.Iterator;

import comingle.facts.Fact;

public class OrderedGoals<F extends Fact> extends Goals<F> {

	class OrderedGoal<F> {

		private F f; private int p;

		public OrderedGoal(F f, int p) {
			this.f = f; this.p = p;
		}

		public F get_fact() { return f; }

		public int priority() { return p; }

		public int compareTo(OrderedGoal other) {
			if(p < other.priority()) { 
				return -1; 
			} else if(p == other.priority()) {
				return 0;
			} else {
				return 1;
			}
		}

		public String toString() {
			return String.format("%s@%s",f,p);
		}

	}

	private PriorityQueue<OrderedGoal<F>> goals;

	public OrderedGoals() { 
		goals = new PriorityQueue<OrderedGoal<F>>();
	}

	@Override
	public synchronized boolean has_goals() { return goals.size() > 0; }

	@Override
	public synchronized F next() {
		return goals.poll().get_fact();
	}

	@Override
	public synchronized void add(F f) {
		OrderedGoal<F> g = new OrderedGoal<F>(f, f.priority); 
		goals.add(g);
	}

	@Override
	public synchronized void add(F f, int p) {
		OrderedGoal<F> g = new OrderedGoal<F>(f, p); 
		goals.add(g);
	}

	@Override
	public synchronized List<F> toList() {
		LinkedList<F> ls = new LinkedList<F>();
		Iterator<OrderedGoal<F>> it = goals.iterator();
		while(it.hasNext()) {
			ls.add( it.next().get_fact() );
		}
		return ls;
	}

	public String toString() {
		String output = ""; // "========== Ordered Goals ==========\n";
		Iterator<OrderedGoal<F>> ls = goals.iterator();
		while(ls.hasNext()) {
			output += String.format("%s ",ls.next());
		}
		// output += "\n==================================";
		return output;
	}

}

