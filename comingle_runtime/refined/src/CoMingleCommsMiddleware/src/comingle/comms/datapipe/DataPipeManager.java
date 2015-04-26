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

package comingle.comms.datapipe;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;


import comingle.comms.receiver.DataReceivedListener;
import comingle.comms.receiver.ExceptionListener;

/**
 * 
 * An Abstract class that contains and manages a DataPipe.
 * 
 * @author Edmund S.L. Lam
 *
 * @param <Data> The type of data this pipeline sends and receives. Must be serializable.
 * @param <Addr> The type of the address that the underlying communication protocol uses.
 */
abstract public class DataPipeManager<Data extends Serializable, Addr> extends Thread {

	protected DataPipe<Data, Addr> pipe;
	
	protected boolean initialized = false;
	
	/**
	 * Mandatory constructor class
	 * @param pipe the instance of the data pipe contained and managed by this class
	 */
	public DataPipeManager(DataPipe<Data, Addr> pipe) {
		this.pipe = pipe;
	}

	/**
	 * Data received routine invoked whenever data is received by the data pipe.
	 * @param data_list the list of data received.
	 * @param addr the address of the sender.
	 */
	abstract protected void receiveData(List<Data> data_list, Addr addr);
	
	/**
	 * The routine invoked whenever an exception occurred during receiving of data
	 * @param e the exception that occurred.
	 */
	abstract protected void handleReceiveException(Exception e);
	
	/**
	 * Initializes the data pipe.
	 */
	protected void init() {
		if(initialized) { return; }
		pipe.addDataReceivedListener(
				new DataReceivedListener<Data,Addr>() {
					@Override				
					public void performDataReceivedAction(final List<Data> msg_list, Addr addr) {
						receiveData(msg_list, addr);
					}
				}
			);

			ExceptionListener except_listener = new ExceptionListener() {
				public void performExceptionAction(final Exception e) {
					handleReceiveException(e);
				}
			};
			
		pipe.setExceptionListener( except_listener );
		pipe.initReceiver();
		initialized = true;
	}
	
	/**
	 * Method floated from data pipe: Creates a thread that sends the given data
	 * @param data_list list of data to send.
	 * @param address the recipient of the data.
	 */
	public void sendData(List<Data> messages, Addr ip_address, boolean threaded) {
		pipe.sendData(messages, ip_address, threaded);
	}
	
	/**
	 * Method floated from data pipe: Interface for sending of singleton data. Decision of sending via a thread is passed in as
	 * the argument threaded.
	 * @param data the data to send.
	 * @param ip_address the recipient of the data.
	 * @param threaded true if data is to be sent by a dedicated thread.
	 */
	public void sendData(Data message, Addr ip_address, boolean threaded) {
		LinkedList<Data> messages = new LinkedList<Data>();
		messages.add(message);
		sendData(messages, ip_address, threaded);
	}
	
	/**
	 * Method floated from data pipe: Send the given list of data to the given address
	 * @param data_list list of data to send.
	 * @param address the recipient of the data.
	 */
	public void sendData(List<Data> messages, Addr ip_address) {
		pipe.sendData(messages, ip_address);
	}
	
	/**
	 * Method floated from data pipe: Interface for threaded sending of singleton data
	 * @param data the data to send.
	 * @param ip_address the recipient of the data.
	 */
	public void sendData(Data message, Addr ip_address) {
		LinkedList<Data> messages = new LinkedList<Data>();
		messages.add(message);
		sendData(messages, ip_address);
	}

	/**
	 * Method floated from data pipe: A general send data interface. Decision of sending via a thread is passed in as
	 * the argument threaded.
	 * @param data_list list of data to send.
	 * @param address the recipient of the data.
	 * @param threaded true if data is to be sent by a dedicated thread.
	 */
	public void sendDataThreaded(List<Data> messages, Addr ip_address) {
		pipe.sendDataThreaded(messages, ip_address);
	}
	
	/**
	 * Method floated from data pipe: Creates a thread that sends the given data
	 * @param data_list list of data to send.
	 * @param address the recipient of the data.
	 */
	public void sendDataThreaded(Data message, Addr ip_address) {
		LinkedList<Data> messages = new LinkedList<Data>();
		messages.add(message);
		sendDataThreaded(messages, ip_address);
	}
	
	/**
	 * Close the data pipe.
	 */
	public void close() {
		pipe.close();
	}
	
	
}
