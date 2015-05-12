package comingle.comms.ntp;

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

import comingle.comms.misc.Barrier;

public class PseudoNTPClient {

	private static final int SOCKET_TIMEOUT = 5000;
	public static int DEFAULT_PORT = 7230;
	
	private String ipAddress;
	private int port;
	
	public PseudoNTPClient(String ipAddress, int port) {
		this.ipAddress = ipAddress;
		this.port = port;
	}
	
	public PseudoNTPClient(String ipAddress) {
		this(ipAddress, DEFAULT_PORT);
	}
	
	/////////////////////
	// Client Routines //
	/////////////////////
	
	long repReceiveTime;
	long repReturnTime;
	long localReceiveTime;
	String latestRepIPAddress;
	
	private class ReceiveFuture extends Thread {
		
		Barrier barrier;
		
		public ReceiveFuture(Barrier barrier) {
			this.barrier = barrier;
		}
		
		ServerSocket serverSocket = null;
		@Override
		public void run() {
			
			try {
				serverSocket = new ServerSocket(port);
				Socket client = serverSocket.accept();
				InputStream inputstream = client.getInputStream();
				ObjectInput input = new ObjectInputStream(inputstream);
				repReceiveTime = input.readLong();
				repReturnTime  = input.readLong();
				localReceiveTime = Calendar.getInstance().getTimeInMillis();
				// latestRepIPAddress = client.getInetAddress().getHostAddress();
				input.close();
				barrier.release();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
		
		public void close() {
			if(serverSocket != null) {
				try {
					serverSocket.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		
	}
	
	private ReceiveFuture recvFuture = null;
	
	public long getTimeOffset() {
		long localTransmitTime;
		try {
			Socket socket = new Socket();
			socket.bind(null);
			socket.connect(new InetSocketAddress(ipAddress, port), SOCKET_TIMEOUT);

			OutputStream stream = socket.getOutputStream();
			ObjectOutput output = new ObjectOutputStream(stream);
			output.writeLong(1);
			output.flush();
			localTransmitTime = Calendar.getInstance().getTimeInMillis();
			output.close();
		
			socket.close();
		
			Barrier barrier = new Barrier();
			recvFuture = new ReceiveFuture(barrier);
			recvFuture.start();
			barrier.await();
		
			// return latestRepTime - (localTransmitTime + localReceiveTime)/2;
			long repProcessTimeDiff = Math.abs( repReturnTime - repReceiveTime );
			long ntpTime = repReceiveTime - ((localReceiveTime - repProcessTimeDiff - localTransmitTime)/2);
			return localTransmitTime - ntpTime;

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return -1;
	}
	
	////////////////////
	// Server Routine //
	////////////////////
	
	boolean terminateServer = false;
	
	private class Server extends Thread {
		
		ServerSocket serverSocket = null;
		
		@Override
		public void run() {
			try {
				serverSocket = new ServerSocket(port);
				
				while(!terminateServer) {
					
					try {	
						Socket client = serverSocket.accept();
						long repReceiveTime = Calendar.getInstance().getTimeInMillis();
						String clientIPAddress = client.getInetAddress().getHostAddress();
					
						Socket socket = new Socket();
						socket.bind(null);
						socket.connect(new InetSocketAddress(clientIPAddress, port), SOCKET_TIMEOUT);

						OutputStream stream = socket.getOutputStream();
						ObjectOutput output = new ObjectOutputStream(stream);
					
						output.writeLong(repReceiveTime);						
						output.flush();
						long repReturnTime = Calendar.getInstance().getTimeInMillis();
						// output.writeChars(String.format("%s:%s", repReceiveTime, repReturnTime));
						output.writeLong(repReturnTime);						
						output.flush();
						output.close();
						
						socket.close();
					} catch (Exception e) {
						e.printStackTrace();
					}
					
				}
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
		
		public void close() {
			if(serverSocket != null) {
				try {
					serverSocket.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		
	}
	
	private Server server = null;
	
	public void servePseudoNTPTime() {
		server = new Server();
		server.start();
	}
	
	public void close() {
		terminateServer = true;
		if (server != null) {
			server.close();
		}
		if (recvFuture != null) {
			recvFuture.close();
		}
	}
	
}
