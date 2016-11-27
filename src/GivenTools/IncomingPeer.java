package GivenTools;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Arrays;


//This class listens to incoming connections that want to request pieces we have downloaded
public class IncomingPeer implements Runnable{
	
	private ServerSocket svc;
	private Socket incomingPeerSocket;
	
	public IncomingPeer () {

	}

	@Override
	public void run() {
		try {
			int count = 0;
			
			while (true) { //maybe not while true?
				svc = new ServerSocket(6881 + count, 10);
		    	// a "blocking" call which waits until a connection is requested	
		    	incomingPeerSocket = svc.accept(); //Get a connection
		    	//Peer constructor - byte[] id, String ip, int port, int threadID
		    	Peer incomingPeer = new Peer(null, incomingPeerSocket.getInetAddress().toString(), incomingPeerSocket.getPort(), RUBTClient.getThreadID());  	
		    	Thread incoming_thread = new Thread(incomingPeer);
		    	incoming_thread.start();
		    	
		    	count++;
		    	RUBTClient.setThreadID(RUBTClient.getThreadID() + 1);
			}
		} catch (IOException e) {
			System.err.print("Incoming connection error in IncomingPeer.java, run(): " + e);
		}
		finally {
			try {
				svc.close();
			} catch (IOException e) {
				System.err.print("Failed to close incoming server socket: " + e);
			}
		}
			
		
	}
	
	

}
