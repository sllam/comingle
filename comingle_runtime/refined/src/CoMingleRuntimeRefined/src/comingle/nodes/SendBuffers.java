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

package comingle.nodes;

import java.util.*;

import comingle.comms.neighborhood.Neighborhood;
import comingle.facts.Fact;
import comingle.facts.SerializedFact;

public class SendBuffers {

	protected Neighborhood neighborhood;
	protected HashMap<Integer,List<SerializedFact>> buffers;
	protected SendListener send_listener = null;

	protected HashMap<Integer,List<SerializedFact>> misses;

	public SendBuffers() {
		buffers   = new HashMap<Integer,List<SerializedFact>>();
		misses    = new HashMap<Integer,List<SerializedFact>>();
	}
		
	public void setSendListener(SendListener send_listener) { 
		this.send_listener = send_listener;
	}

	public void setNeighborhood(Neighborhood neighborhood) {
		this.neighborhood = neighborhood;
	}
	
	public Collection<Integer> getLocations() {
		return neighborhood.getLocations();
	}
	 
	public boolean reBufferMissed() {
		List<Integer> hits = new LinkedList<Integer>();
		for(Map.Entry<Integer, List<SerializedFact>> entry : misses.entrySet()) {
			Integer loc = entry.getKey();
			List<SerializedFact> facts = (List<SerializedFact>) entry.getValue();
			if(neighborhood.isConnectedTo(loc)) {
				hits.add(loc);
				List<SerializedFact> ls;
				if(buffers.containsKey(loc)) {
					ls = buffers.get(loc);
				} else {
					ls = new LinkedList<SerializedFact>();
					buffers.put(loc, ls);
				}
				ls.addAll(facts);
				
			}
		}
		for(int hit: hits) { misses.remove(hit); }		
		return hits.size() > 0;
	}

	protected String lookupIPAddress(Integer loc) {
		if (!neighborhood.isConnectedTo(loc)) { return null; }
		return neighborhood.lookupIPAddress(loc);
	}

	public void sendAll() {
		if(send_listener == null) { return; }
		for(Map.Entry<Integer,List<SerializedFact>> entry : buffers.entrySet()) {
			Integer loc = entry.getKey();
			List<SerializedFact> facts = (List<SerializedFact>) entry.getValue();
			String ipAddr = lookupIPAddress(loc);
			if (ipAddr != null) { 
				send_listener.performSendAction(ipAddr, facts); 		
			} else {
				misses.put(loc, facts);
			}
		}
		buffers = new HashMap<Integer,List<SerializedFact>>();
	}

	public <F extends Fact> void add(List<F> facts) {
		for(F fact: facts) {
			add(fact);
		}
	}

	public <F extends Fact> void add(F fact) {
		int f_loc = fact.get_loc();

		List<SerializedFact> ls = null;
		if(buffers.containsKey(f_loc)) {
			ls = buffers.get(f_loc);	
		} else {
			ls = new LinkedList<SerializedFact>();
			buffers.put(f_loc,ls);
		}

		ls.add( fact.serialize() );
	}

}


