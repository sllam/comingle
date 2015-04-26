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

package comingle.comms.receiver;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

/**
 * 
 * Abstract class of a daemon thread created by a DataPipe, whose sole purpose is to receive data
 * for its creator DataPipe. 
 * 
 * @author Edmund S.L. Lam
 *
 * @param <Data> 
 * @param <Addr>
 */
abstract public class ReceiverDaemon<Data extends Serializable, Addr> extends Thread {

	protected List<DataReceivedListener<Data,Addr>> data_received_listeners;
	protected ExceptionListener except_listener;
	protected boolean proceed;
	
	public ReceiverDaemon() {
		data_received_listeners = new LinkedList<DataReceivedListener<Data,Addr>>();
		proceed = true;
	}
	
	/**
	 * Add a data received listener. Listener operations will be invoked every time
	 * data is received by this network socket.
	 * @param dr_listener the listener to add.
	 */
	public void addDataReceivedListener(DataReceivedListener<Data,Addr> dr_listener) {
		data_received_listeners.add( dr_listener );
	}

	/**
	 * Invoke data received listeners.
	 * @param data_list the list of data received by the network socket.
	 * @param addr the source address of the received data.
	 */
	protected void performDataReceivedActions(List<Data> data_list, Addr addr) {
		for(DataReceivedListener<Data,Addr> dr_listener : data_received_listeners) {
			dr_listener.performDataReceivedAction( data_list, addr );
		}
	}
	
	/**
	 * Set a exception listener to this network socket. This listener operation will
	 * be invoked each time this socket throws an exception.
	 * @param except_listener the listener to set.
	 */
	public void setExceptionListener(ExceptionListener except_listener) {
		this.except_listener = except_listener;
	}

	/**
	 * Invoke exception listener
	 * @param e the exception that caused this invocation. 
	 */
	protected void performExceptionAction(Exception e) {
		if (except_listener != null && proceed) { 
			except_listener.performExceptionAction(e);
		}
	}
	

	/**
	 * Inform the receiver daemon process to stop.
	 */
	public synchronized void flagNotProceed() { proceed = false; }
	
	protected void castInput(Object input_obj, Addr addr) {
		List<Data> data_list = null;
		try {
			data_list = (List<Data>) input_obj;
		} catch (Exception e1) {
			try { AdminToken admin = (AdminToken) input_obj; }
			catch (Exception e2) { performExceptionAction(e2); }
			// TODO Admin stuff, current we only anticipate terminate orders, so do nothing.
			return;
		}
		performDataReceivedActions(data_list, addr);
	}
	
	abstract protected void initDaemon() throws Exception;
	
	abstract protected void receiveData() throws Exception;

	public abstract void close();
	
	/**
	 * Main receiver daemon routine. 
	 */
	@Override
	public void run() {
		try {
			initDaemon();
			while(proceed) {
				try {
					receiveData();
				} catch (Exception e) {
					performExceptionAction(e);
				}
			}
			// serverSocket.close();
		} catch (Exception e) {
			performExceptionAction(e);
		}
	}
	
}
