package GivenTools;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Arrays;

public class PeerConnection {
	
	//Peer's id, ip, port 
	private static String peer_id;
	private static String peer_ip;
	private static int peer_port;
	
	/**
	 * Verify the info_hash and generatedpeerid
	 * The info_hash should be the same as sent to the tracker,
	 * 	and the peer_id is the same as sent to the tracker. 
	 * If the info_hash is different between two peers, then the connection is dropped.
	 */
	private static byte[] info_hash;
	private static String generatedPeerId;
	
	/**
	 * used in openSocket()
	 */
	private static Socket peerSocket;
	private static DataOutputStream toPeer;
	private static DataInputStream fromPeer;
	
	public PeerConnection(Peer p, byte[] info_hash, String generatedPeerId) {
		peer_id = p.getPeerID();
		peer_ip = p.getPeerIP();
		peer_port = p.getPeerPort();
		this.info_hash = info_hash;
		this.generatedPeerId = generatedPeerId;
	}
	
	public static String getPeer_id() {
		return peer_id;
	}

	public static String getPeer_ip() {
		return peer_ip;
	}

	public static int getPeer_port() {
		return peer_port;
	}

	public static Socket getPeerSocket() {
		return peerSocket;
	}

	public static DataOutputStream getToPeer() {
		return toPeer;
	}

	public static DataInputStream getFromPeer() {
		return fromPeer;
	}
	
	//Call PeerConnection.openSocket
	public static void openSocket() {
	    try {
	    	peerSocket = new Socket(peer_ip, peer_port);
	    	toPeer = new DataOutputStream(peerSocket.getOutputStream());
			fromPeer = new DataInputStream(peerSocket.getInputStream());
	    }	
	    catch (IOException e) {
	        System.out.println(e);
	    }
	}
	
	//Maybe create another class to do handshake...? 
	@SuppressWarnings("unused")
	private static void handshakePeer() {
		
	}
	
	//Use Arrays.equals() if you want to compare the actual content of arrays that contain primitive types values (like byte).
	//Checks the info_hash from torrent info and a peer
	public static boolean checkInfoHash(byte[] peersHash) {
		if (Arrays.equals(info_hash, peersHash)) {
			return true;
		} else {
			return false;
		}
	}
}
