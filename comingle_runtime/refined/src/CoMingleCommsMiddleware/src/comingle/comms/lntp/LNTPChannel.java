package comingle.comms.lntp;

import comingle.comms.receiver.ExceptionListener;

public abstract class LNTPChannel {

	protected static final int SOCKET_TIMEOUT = 5000;
	protected static final int DEFAULT_PORT   = 1230;

	protected String refIPAddress;
	protected int port;
	protected ExceptionListener listener = null;
	
	public LNTPChannel(String refIPAddress, int port) {
		this.refIPAddress = refIPAddress;
		this.port = port;
	}
	
	public LNTPChannel(String refIPAddress) {
		this(refIPAddress, DEFAULT_PORT);
	}
	
	public abstract void init();
	
	public abstract long getTimeOffset();
	
	public abstract void close();
	
	public void setExceptionListener(ExceptionListener listener) {
		this.listener = listener;
	}
	
	public void invokeException(String task, Exception e) {
		if(listener != null) {
			listener.performExceptionAction(task, e);
		}
	}
}
