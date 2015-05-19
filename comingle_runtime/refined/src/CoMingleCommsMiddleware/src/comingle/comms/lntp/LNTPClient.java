package comingle.comms.lntp;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Calendar;
import java.util.Queue;

import comingle.comms.misc.Barrier;
import comingle.comms.misc.MailBox;

public class LNTPClient extends LNTPChannel {

	// protected long repReceiveTime;
	// protected long repReturnTime;
	// protected long localReceiveTime;
	protected String latestRepIPAddress;
	
	protected ReceiveFuture recvFuture = null;
	protected MailBox<TimePayLoad> mbox = null;
	
	public LNTPClient(String refIPAddress) {
		super(refIPAddress);
	}

	private class TimePayLoad {
		
		public long repReceiveTime;
		public long repReturnTime;
		public long localReceiveTime;
		
		public TimePayLoad(long t2, long t3, long t4) {
			repReceiveTime = t2;
			repReturnTime  = t3;
			localReceiveTime = t4;
		}
		
	}
	
	private class ReceiveFuture extends Thread {
		
		
		public ReceiveFuture() { }
		
		ServerSocket serverSocket = null;
		boolean terminated = false;
		@Override
		public void run() {
			
			try {
				serverSocket = new ServerSocket(port);
				
				while(!terminated) {
				
					try {
					
						Socket client = serverSocket.accept();
						InputStream inputstream = client.getInputStream();
						ObjectInput input = new ObjectInputStream(inputstream);
						long repReceiveTime = input.readLong();
						long repReturnTime  = input.readLong();
						long localReceiveTime = Calendar.getInstance().getTimeInMillis();
						// latestRepIPAddress = client.getInetAddress().getHostAddress();
				
						mbox.putData(new TimePayLoad(repReceiveTime, repReturnTime, localReceiveTime));
				
						input.close();
						client.close();	
					} catch(IOException e) {
						invokeException("Receiving Time Server Response", e);
					}
						
				}
				
			} catch (IOException e) {
				invokeException("Initializing Receive Server Socket" , e);
				// e.printStackTrace();
			}
			
		}
		
		public void close() {
			terminated = true;
			if(serverSocket != null) {
				try {
					serverSocket.close();
				} catch (IOException e) {
					invokeException("Closing ReceiveFuture Socket", e);
					// e.printStackTrace();
				}
			}
		}
		
	}
	
	@Override
	public long getTimeOffset() {
		// long m1 = medianOf3(getTimeOffsetInt(),getTimeOffsetInt(),getTimeOffsetInt());
		// return medianOf3(m1,getTimeOffsetInt(),getTimeOffsetInt());
		return medianOf3(getTimeOffsetInt(),getTimeOffsetInt(),getTimeOffsetInt());
	}
	
	public long medianOf3(long l1, long l2, long l3) {
		if (l1 <= l2) {
			if(l2 <= l3) {
				return l2;
			} else {
				return l1 <= l3 ? l3 : l2;
			}
		} else {
			if(l1 <= l3) {
				return l1;
			} else {
				return l2 <= l3 ? l3 : l2;
			}
		}
	}
	
	public long getTimeOffset(int times) {
		long offsets = 0;
		for(int x=0; x<times; x++) {
			offsets += getTimeOffsetInt();
		}
		return offsets/times;
	} 
	
	public long getTimeOffsetInt() {
		mbox.reset();
		long localTransmitTime;
		try {
			Socket socket = new Socket();
			socket.bind(null);
			socket.connect(new InetSocketAddress(refIPAddress, port), SOCKET_TIMEOUT);

			// Barrier barrier = new Barrier();
			// recvFuture = new ReceiveFuture(barrier);
			
			OutputStream stream = socket.getOutputStream();
			ObjectOutput output = new ObjectOutputStream(stream);
			output.writeLong(1);
			output.flush();
			localTransmitTime = Calendar.getInstance().getTimeInMillis();
			
			// recvFuture.start();
			
			output.close();
			socket.close();
		
			TimePayLoad pl = mbox.getData();
			long repReceiveTime = pl.repReceiveTime;
			long repReturnTime  = pl.repReturnTime;
			long localReceiveTime = pl.localReceiveTime;
			
			// barrier.await();
		
			// return latestRepTime - (localTransmitTime + localReceiveTime)/2;
			long repProcessTimeDiff = Math.abs( repReturnTime - repReceiveTime );
			long ntpTime = repReceiveTime - ((localReceiveTime - repProcessTimeDiff - localTransmitTime)/2);
			return localTransmitTime - ntpTime;

		} catch (IOException e) {
			invokeException("Requesting Time Server Response", e);
			// e.printStackTrace();
		}
		return -1;
	}

	@Override
	public void close() {
		if(recvFuture != null) {
			recvFuture.close();
		}
	}
	
	@Override
	public void init() { 
		mbox = new MailBox<TimePayLoad>();
		recvFuture = new ReceiveFuture();
		recvFuture.start();
	}
	
}
