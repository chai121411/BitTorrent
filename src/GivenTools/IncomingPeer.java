package GivenTools;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;

//This class listens to incoming connections that want to request pieces we have downloaded
public class IncomingPeer implements Runnable{

	private ServerSocket svc;
	private Socket incomingPeerSocket;
	private DataOutputStream toPeer;
	private DataInputStream fromPeer;
	private int incomingThreadID;
	
	public IncomingPeer (int tID) {
		incomingThreadID = tID;
	}

	@Override
	public void run() {
		byte[] incomingPeersHandshake = new byte[68]; //28 + 20 + 20 ; fixedHeader, info_Hash, peerID
		try {
			svc = new ServerSocket(6881 + incomingThreadID, 10);
			
			while (true) {
			
		    	// a "blocking" call which waits until a connection is requested	
		    	incomingPeerSocket = svc.accept(); //Get a connection
		    	System.out.println("PAST BLOCKING CALL IN INCOMINGPEER.JAVA");
		    	
		    	toPeer = new DataOutputStream(incomingPeerSocket.getOutputStream());
				fromPeer = new DataInputStream(incomingPeerSocket.getInputStream());
				fromPeer.readFully(incomingPeersHandshake, 0, incomingPeersHandshake.length); //read fromPeer and store 68 bytes into peersHandshake
				
				System.out.println("From incoming peer: " + Arrays.toString(incomingPeersHandshake));
				
				/**
				 * When serving files, you should check the incoming peer’s handshake to verify that the info_hash
				 * matches one that you are serving and close the connection if not.
				 */
				if (!checkIncomingInfoHash(incomingPeersHandshake)) {
					System.err.println("Incoming peer sent an invalid info hash.");
					closeResources();
					return;
				}
				
				toPeer.write(createHandshakeHeader(RUBTClient.info_hash, RUBTClient.getGeneratedPeerID()));
				
				//TODO KEEPALIVE? LISTEN FOR REQUESTS, SEND PIECES(sendPiece is implemented in PeerMessages.java)
				
				closeResources();
			}
		} catch (IOException e) {
			System.err.print("Incoming connection error in IncomingPeer.java, run(): " + e);
		}
	}
	
	private byte[] createHandshakeHeader(byte[] info_hash, String generatedPeerID) {
		ByteArrayOutputStream header = new ByteArrayOutputStream();
		byte[] fixedHeader = {19, 'B','i','t','T','o','r','r','e','n','t',' ', 'p','r','o','t','o','c','o','l',0,0,0,0,0,0,0,0};
		
		try {
			header.write(fixedHeader);
			header.write(info_hash);
			header.write(generatedPeerID.getBytes());
		} catch (IOException e) {
			System.err.println("Failed to generate handshake header.");
		}
		
		return header.toByteArray();
	}
	
	private boolean checkIncomingInfoHash(byte[] incomingHandshake) {
		byte[] incomingInfoHash = new byte[20];
		
		//From peersHandshake starting at index 28, copy 20 bytes into peersInfo starting at index 0
		System.arraycopy(incomingHandshake, 28, incomingInfoHash, 0, 20);
		if (!isEqualByteArray(RUBTClient.info_hash, incomingInfoHash)) {
			System.out.println("The info hash is wrong");
			return false;
		}
		return true;
	}
	
	public boolean isEqualByteArray(byte[] b1, byte[] b2) {
		if (Arrays.equals(b1, b2)) {
			return true;
		} else {
			return false;
		}
	}
	
	private void closeResources() {
		try {
			toPeer.close();
			fromPeer.close();
			incomingPeerSocket.close();
			svc.close();
		} catch (IOException e) {
			System.err.println("Closing resources failed: " + e);
		}
	}
	
	

}
