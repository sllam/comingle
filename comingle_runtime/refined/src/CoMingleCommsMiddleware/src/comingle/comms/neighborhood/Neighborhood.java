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

package comingle.comms.neighborhood;

import java.util.Collection;
import java.util.Map;

/**
 * 
 * Interface of a class that provides neighborhood connectivity functionality
 * 
 * @author Edmund S.L. Lam
 *
 */
public interface Neighborhood {
	
	/**
	 * Get the location of the local node
	 * @return location of local node
	 */
	public int getLocation();
	
	/**
	 * Get all locations associated with a given network type
	 * @param networkID the unique identifier of the network
	 * @return all locations associated with this type of network
	 */
	public Collection<Integer> getLocations(int networkID);

	/**
	 * Get all locations, period.
	 * @return all locations, period.
	 */
	public Collection<Integer> getLocations();
	
	/**
	 * Checks if given location is connected to local node via the given network type.
	 * @param destLoc destination location
	 * @param networkID unique identifier of the network
	 * @return true if destLoc is connected to local node via network networkID
	 */
	public boolean isConnectedTo(int destLoc, int networkID);
	
	/**
	 * Checks if given location is connected to local node, period.
	 * @param destLoc destination location
	 * @return true if destLoc is connected to local node.
	 */
	public boolean isConnectedTo(int destLoc);
	
	/**
	 * Lookup for given location's address
	 * @param loc location to lookup
	 * @return address of loc
	 */
	public String lookupIPAddress(int loc);
	
	/**
	 * Lookup for all possible network-identifier/address pairs of a given location
	 * @param loc location to lookup
	 * @return a map containing mappings of network-identifier to addresses, of the given location
	 */
	public Map<Integer,String> lookupIPAddresses(int loc);
	
}
