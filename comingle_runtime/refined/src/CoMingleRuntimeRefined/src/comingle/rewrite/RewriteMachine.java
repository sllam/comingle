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

package comingle.rewrite;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.Thread;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import comingle.goals.Goals;
import comingle.goals.ListGoals;
import comingle.goals.OrderedGoals;
import comingle.store.ConcListStore;
import comingle.store.Store;
import comingle.store.StoreIter;
import comingle.actuation.ActuatorAction;
import comingle.actuation.Actuators;
import comingle.comms.neighborhood.Neighborhood;
import comingle.facts.Fact;
import comingle.facts.SerializedFact;
import comingle.misc.Beeper;
import comingle.nodes.SendBuffers;
import comingle.nodes.SendListener;

import comingle.logging.CoLogger;

/**
 * 
 * @author Edmund S.L. Lam
 *
 */
public abstract class RewriteMachine extends Thread {
	
	protected static final int SWEEP_THRESHOLD = 10;

	protected LinkedList<SerializedFact> external_goals = null;
	
	protected Goals<Fact> goals;
	protected LinkedList<Store> stores;

	protected Neighborhood neighborhood;
	protected SendBuffers send_buffers = null;

	protected Actuators actuators;
	
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

	// Deprecated stuff
	protected boolean isSolo = false;
	protected int location;
	
	public RewriteMachine(Neighborhood neighborhood, SendListener send_listener) {
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
		actuators = new Actuators();
		// setupNeighborhood(neighborhood, send_listener);
	}
	
	public RewriteMachine() {
		this(null, null);
	}

	public void setupNeighborhood(Neighborhood neighborhood, SendListener send_listener) {
		this.neighborhood = neighborhood;
		this.send_buffers = new SendBuffers();
		this.send_buffers.setNeighborhood(neighborhood);
		this.send_buffers.setSendListener(send_listener);
		this.external_goals = new LinkedList<SerializedFact>();
		this.location = neighborhood.getLocation();
	}
	
	public int getLocation() { return neighborhood.getLocation(); }
	
	public Collection<Integer> getLocations() {
		return send_buffers.getLocations();
	}

	public synchronized void addExternalGoals(List<SerializedFact> new_goals) {
		this.external_goals.addAll(new_goals);
		notify_new_goals();
	}
	
	public synchronized void addExternalGoal(SerializedFact new_goal) {
		this.external_goals.add(new_goal);
		notify_new_goals();
	}
	
	protected synchronized boolean flushGoals() {
		if (external_goals.size() == 0) { return false; }
		for (Fact new_goal : external_goals) {
			goals.add(new_goal, new_goal.priority);
		}
		external_goals.clear();
		return true;
	}
	
	public String next_exist_id(int i) {
		int id = count_id++;
		return String.format("%s-%s", neighborhood.getLocation(), id);
	}
	
	public Goals<Fact> getGoals() { return goals; }

	public LinkedList<Store> getStores() { return stores; }

	public LinkedList<SerializedFact> getExternalGoals() { return this.external_goals; }
	
	public String getPrettyBrief() {
		String output = "";
		
		ConcListStore[] stores = getLinearStores();
		
		for(int x=0; x<stores.length; x++) {
			output += stores[x].toString() + " ";
		}
		
		return output;
	}
	
	public String getPretty() {
		String output = "";

		ConcListStore[] stores = getLinearStores();
		
		for(int x=0; x<stores.length; x++) {
			output += String.format( "============= %s ==============\n", stores[x].get_name());
			output += stores[x].toString() + "\n";
			output += "========================================\n";
		}
		
		return output;
	}
	
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
		this.goals = (Goals<Fact>) goals;
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

	public <F extends Fact> void introLocalGoal(F fact) {
		goals.add(fact);
	}

	public <F extends Fact> void introGoal(F fact) {
		if(neighborhood.getLocation() == fact.get_loc()) {
			goals.add(fact);
		} else {
			send_buffers.add(fact);
		}
	}

	public void run() {
		while(true) {
			quiescent.unbeep();
			rewriteloop();
			quiescent.beep();
			synchronized(send_buffers) {
				send_buffers.sendAll(); 
			}
			performQuiescenceActions(); 
			if(terminate) {
				performTerminateActions();
				return;
			} else if(!proceed) { 
				performStopActions();
				trigger.wait_for_beep(); 
			}
			if (this.flushGoals()) { continue; }
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

	public void add_new_goal(Fact f) {
		goals.add(f);
		notify_new_goals();
	}

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

	private void writeObject(ObjectOutputStream aOutputStream) throws IOException {
		aOutputStream.defaultWriteObject();
	}

	private void readObject(ObjectInputStream aInputStream) throws ClassNotFoundException, IOException {
		aInputStream.defaultReadObject();
	}
	
	// External Actuation
	
	public <T> void setActuator(String act_name, ActuatorAction<T> action) {
		actuators.setActuator(act_name, action);
	}
	
	public <T> boolean invokeActuator(String act_name, T input) {
		return actuators.invokeActuator(act_name, input);
	}
	
	public ConcListStore[] getLinearStores() {
		return new ConcListStore[0];
	}
	
	
	public void changeLocation(final int newLoc, final String newHost) {
		addOneTimeQuiescenceListener(new QuiescenceListener() {
			public void performQuiescenceAction(QuiescenceEvent qe) {
				List<Fact> myFacts = new LinkedList<Fact>();
				for(Store store: stores) {
					StoreIter iter = store.lookup_candidates();
					Fact fact = iter.get_next_alive();
					while(fact != null) {
						if(fact.get_loc() == newLoc) {
							fact.set_dead();
							myFacts.add(fact);
						}
						fact = iter.get_next_alive();
					}
				}
				purge();
				for(Fact fact: myFacts) {
					fact.alive = true;
					fact.loc = newLoc;
					goals.add(fact);
				}
				notify_new_goals();
			}
		});
	}
	
}
