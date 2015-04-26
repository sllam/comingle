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
import comingle.comms.receiver.ReceiverDaemon;


/**
 * 
 * Abstraction of a Bidirectional communication pipeline (channel), that asynchronously sends and receives data of type
 * Data, over addresses of type Addr. This class contains interfaces to asynchronously send data, and create a daemon process 
 * that listens for data. Data received events are handled by a DataReceivedListener attached to an instance of this class.
 * 
 * @author Edmund S.L. Lam
 *
 * @param <Data> The type of data this pipeline sends and receives. Must be serializable.
 * @param <Addr> The type of the address that the underlying communication protocol uses.
 */
abstract public class DataPipe<Data extends Serializable, Addr> {

	protected ReceiverDaemon<Data,Addr> receiver;
	protected ExceptionListener receive_except_listener = null;
	protected ExceptionListener send_except_listener    = null;
	protected ExceptionListener connect_except_listener = null;
	
	/**
	 * Set receiver as the receiver daemon of this data pipe.
	 * @param receiver a receiver daemon, that handles incoming data.
	 */
	public void setReceiverDaemon(ReceiverDaemon<Data,Addr> receiver) {
		this.receiver = receiver;
	}
	
	/**
	 * Initialize the receiver daemon
	 */
	abstract public void initReceiver();
	
	/**
	 * Close all non-serializable entities of this data pipe (e.g., opened sockets, daemon threads, etc..)
	 */
	abstract public void close();
	
	/**
	 * Send the given list of data to the given address
	 * @param data_list list of data to send.
	 * @param address the recipient of the data.
	 */
	abstract public void sendData(List<Data> data_list, Addr address);
	
	/**
	 * Adds a data receiver listener to the receiver daemon.
	 * @param listener the listener to add.
	 */
	public void addDataReceivedListener(DataReceivedListener<Data,Addr> listener) {
		receiver.addDataReceivedListener( listener );
	}

	/**
	 * Sets an exception listener to the receiver daemon. Also sets this listener for
	 * sending exceptions
	 * @param listener the listener to use.
	 */
	public void setExceptionListener(ExceptionListener listener) {
		send_except_listener = listener;
		connect_except_listener = listener;
		receiver.setExceptionListener( listener );
	}

	/**
	 * Sets an exception listener to the receiver daemon.
	 * @param listener the listener to use.
	 */
	public void setReceiveExceptionListener(ExceptionListener listener) {
		receiver.setExceptionListener( listener );
	}
	
	/**
	 * Sets an exception listener for sending operations.
	 * @param listener the listener to use.
	 */
	public void setSendExceptionListener(ExceptionListener listener) {
		send_except_listener = listener; 
	}

	/**
	 * Sets an exception listener for connecting operations.
	 * @param listener the listener to use.
	 */
	public void setConnectExceptionListener(ExceptionListener listener) {
		connect_except_listener = listener; 
	}
	
	/**
	 * Creates a thread that sends the given data
	 * @param data_list list of data to send.
	 * @param address the recipient of the data.
	 */
	public void sendDataThreaded(final List<Data> data_list, final Addr ip_address) {
		new Thread() {
			@Override
			public void run() {
				sendData(data_list, ip_address);
			}
		}.start();
	}
	
	/**
	 * A general send data interface. Decision of sending via a thread is passed in as
	 * the argument threaded.
	 * @param data_list list of data to send.
	 * @param address the recipient of the data.
	 * @param threaded true if data is to be sent by a dedicated thread.
	 */
	public void sendData(List<Data> data_list, Addr ip_address, boolean threaded) {
		if(threaded) {
			sendDataThreaded(data_list, ip_address);
		} else {
			sendData(data_list, ip_address);
		}
	}
	
	/**
	 * Interface for non-threaded sending of singleton data
	 * @param data the data to send.
	 * @param ip_address the recipient of the data.
	 */
	public void sendData(Data data, Addr ip_address) {
		List<Data> data_list = new LinkedList<Data>();
		data_list.add( data );
		sendData( data_list, ip_address );
	}
	
	/**
	 * Interface for threaded sending of singleton data
	 * @param data the data to send.
	 * @param ip_address the recipient of the data.
	 */
	public void sendDataThreaded(Data data, Addr ip_address) {
		List<Data> data_list = new LinkedList<Data>();
		data_list.add( data );
		sendDataThreaded( data_list, ip_address );
	}
	
	/**
	 * Interface for sending of singleton data. Decision of sending via a thread is passed in as
	 * the argument threaded.
	 * @param data the data to send.
	 * @param ip_address the recipient of the data.
	 * @param threaded true if data is to be sent by a dedicated thread.
	 */
	public void sendData(Data data, Addr ip_address, boolean threaded) {
		if(threaded) {
			sendDataThreaded(data, ip_address);
		} else {
			sendData(data, ip_address);
		}
	}
	
}
