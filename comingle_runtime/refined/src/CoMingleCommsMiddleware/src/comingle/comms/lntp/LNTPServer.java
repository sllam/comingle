package comingle.comms.lntp;

import java.io.IOException;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Calendar;


public class LNTPServer extends LNTPChannel {

	protected boolean terminateServer = false;
	protected Server server = null;
	
	public LNTPServer(String refIPAddress) {
		super(refIPAddress);
	}

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
						// e.printStackTrace();
						invokeException("Processing Time Client Request", e);
					}
					
				}
				
			} catch (IOException e) {
				invokeException("Initiating Time Server Socket", e);
				// e.printStackTrace();
			}

		}
		
		public void close() {
			if(serverSocket != null) {
				try {
					serverSocket.close();
				} catch (IOException e) {
					invokeException("Closing Time Server Socket", e);
					// e.printStackTrace();
				}
			}
		}
	}
	
	@Override
	public void init() {
		server = new Server();
		server.start();
	}
	
	@Override
	public long getTimeOffset() {
		return 0;
	}

	@Override
	public void close() {
		terminateServer = true;
		if(server != null) {
			server.close();
		}
	}

}
