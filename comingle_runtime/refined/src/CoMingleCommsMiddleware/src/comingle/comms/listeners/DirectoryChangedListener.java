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

package comingle.comms.listeners;

import java.util.List;

import comingle.comms.directory.NodeInfo;

public abstract class DirectoryChangedListener {
	/**
	 * Listener action to invoke when the directory has been updated
	 * @param new_peers the list of new peers in the directory.
	 * @param added_nodes list of nodes that have been added.
	 * @param dropped_nodes list of nodes that have been dropped.
	 * @param role the role of this directory.
	 */
	public abstract void doDirectoryChangedAction(final List<NodeInfo> new_peers
			                                     ,final List<NodeInfo> added_nodes 
			                                     ,final List<NodeInfo> dropped_nodes
			                                     ,final int role);
}
