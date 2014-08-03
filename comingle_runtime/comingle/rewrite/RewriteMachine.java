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

package comingle.rewrite;

import java.lang.Thread;

import java.util.LinkedList;
import java.util.ListIterator;

import comingle.goals.Goals;
import comingle.goals.ListGoals;
import comingle.goals.OrderedGoals;
import comingle.store.Store;
import comingle.store.StoreIter;
import comingle.facts.Fact;
import comingle.misc.Beeper;

import comingle.logging.CoLogger;

public abstract class RewriteMachine extends Thread {

	protected static final int SWEEP_THRESHOLD = 20;

	protected Goals<? extends Fact> goals;
	protected LinkedList<Store> stores;
	protected int count_id;
	protected Boolean proceed   = true;
	protected Boolean terminate = false;

	protected Beeper has_new_goal;
	protected Beeper quiescent;
	protected Beeper trigger;

	protected LinkedList<QuiescenceListener> persistent_qlisteners;
	protected LinkedList<QuiescenceListener> one_time_qlisteners;
	protected LinkedList<StopListener> persistent_slisteners;
	protected LinkedList<StopListener> one_time_slisteners;
	protected LinkedList<TerminateListener> persistent_tlisteners;

	public RewriteMachine() {
		stores = new LinkedList<Store>();
		count_id = 0;
		has_new_goal = new Beeper();
		quiescent = new Beeper();
		trigger = new Beeper();
		one_time_qlisteners   = new LinkedList<QuiescenceListener>();
		persistent_qlisteners = new LinkedList<QuiescenceListener>();
		one_time_slisteners   = new LinkedList<StopListener>();
		persistent_slisteners = new LinkedList<StopListener>();
		persistent_tlisteners = new LinkedList<TerminateListener>();
	}

	public int next_exist_id(int i) {
		return count_id++;
	}

	public Goals<? extends Fact> getGoals() { return goals; }

	public LinkedList<Store> getStores() { return stores; }

	public void addPersistentQuiescenceListener(QuiescenceListener ql) {
		persistent_qlisteners.add( ql );
	}

	public void addOneTimeQuiescenceListener(QuiescenceListener ql) {
		one_time_qlisteners.add( ql );
	}

	public void addPersistentStopListener(StopListener sl) {
		persistent_slisteners.add( sl );
	}

	public void addOneTimeStopListener(StopListener sl) {
		one_time_slisteners.add( sl );
	}

	public void addPersistentTerminateListener(TerminateListener tl) {
		persistent_tlisteners.add( tl );
	}

	protected void set_goal_component(Goals<? extends Fact> goals) {
		this.goals = goals;
	}

	protected void set_store_component(Store store) {
		stores.add(store);
	}

	protected boolean rewrite() {
		return false;
	}
	
	protected boolean rewrite(int max_steps) {
		return false;
	}

	public void init() { }	

	public void rewriteloop() {
		boolean done_something = true;
		while(done_something) {
			done_something = rewrite(SWEEP_THRESHOLD);
			purge();
		}
	}

	public void run() {
		while(true) {
			quiescent.unbeep();
			rewriteloop();
			quiescent.beep();
			performQuiescenceActions();
			if(terminate) {
				performTerminateActions();
				return;
			} else if(!proceed) { 
				performStopActions();
				trigger.wait_for_beep(); 
			}
			has_new_goal.wait_for_beep();
		}
	}

	public void performQuiescenceActions() {
		// TODO: Currently, QEs are empty class. Add stuff in them if needed.
		QuiescenceEvent qe = new QuiescenceEvent();
		for(int i=0; i<one_time_qlisteners.size(); i++) {
			one_time_qlisteners.get(i).performQuiescenceAction(qe);
		}
		for(int i=0; i<persistent_qlisteners.size(); i++) {
			persistent_qlisteners.get(i).performQuiescenceAction(qe);
		}
		one_time_qlisteners = new LinkedList<QuiescenceListener>();
	}

	public void performStopActions() {
		// TODO: Currently, SEs are empty class. Add stuff in them if needed.
		StopEvent qe = new StopEvent();
		for(int i=0; i<one_time_slisteners.size(); i++) {
			one_time_slisteners.get(i).performStopAction(qe);
		}
		for(int i=0; i<persistent_slisteners.size(); i++) {
			persistent_slisteners.get(i).performStopAction(qe);
		}
		one_time_slisteners = new LinkedList<StopListener>();
		proceed = true;
	}

	public void performTerminateActions() {
		// TODO: Currently, TEs are empty class. Add stuff in them if needed.
		TerminateEvent te = new TerminateEvent();
		for(int i=0; i<persistent_tlisteners.size(); i++) {
			persistent_tlisteners.get(i).performTerminateAction(te);
		}
	}

	public void notify_new_goals() { has_new_goal.beep(); }

	public void wait_for_quiescence() {
		quiescent.wait_for_beep();
	}

	public void restart_rewrite() {
		proceed = true;
		trigger.beep();
	}

	public void stop_rewrite() {
		proceed = false;
		has_new_goal.beep();
	}

	public void terminate_rewrite() {
		terminate = true;
		has_new_goal.beep();
	}

	protected void purge() {
		ListIterator<Store> it = stores.listIterator();
		while(it.hasNext()) {
			it.next().purge();
		}
	}

	public String toString() {
		String output = goals.toString() + "\n";
		ListIterator<Store> it = stores.listIterator();
		while(it.hasNext()) {
			output += String.format("%s\n", it.next());
		}
		return output;
	}

	public String getFacts() {
		String output = goals.toString() + "\n";
		ListIterator<Store> it = stores.listIterator();
		while(it.hasNext()) {
			StoreIter sit = it.next().lookup_candidates();
			Fact f = sit.get_next_alive();
			while(f != null) {
				output += String.format("%s\n", f);
				f = sit.get_next_alive();
			}
		}
		return output;
	}

}
