package GivenTools;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.util.Arrays;

public class PeerMessages {
	
	private boolean choking;
	private boolean interested;
	private Peer peer;
	private Socket peerSocket;
	private DataOutputStream toPeer;
	private DataInputStream fromPeer;
	private ByteArrayOutputStream out;
	
	
	private static final byte[] keep_alive = {0,0,0,0};
	
	private static final byte[] length_prefix = {0,0,0,1};
	
	/**
	 * Key for choke message
	 */
	private static final int KEY_CHOKE = 0;
	
	/**
	 * Key for unchoke message
	 */
	private static final int KEY_UNCHOKE = 1;
	
	/**
	 * Key for interested message
	 */
	private static final int KEY_INTERESTED = 2;
	
	/**
	 * Key for uninterested message
	 */
	private static final int KEY_UNINTERESTED = 3;
	
	/**
	 * Key for have message
	 */
	private static final int KEY_HAVE = 4;

	/**
	 * Key for request message
	 */
	private static final int KEY_REQUEST = 6;
	
	/**
	 * Key for piece message
	 */
	private static final int KEY_PIECE = 7;
	
	
	public void start (Peer p) {
		choking = true;
		interested = false;
		peer = p;
		out = new ByteArrayOutputStream();
		peerSocket = p.getSocket();
		toPeer = p.getOutput();
		fromPeer = p.getInput();
		
		readBitfield();
	}
	
	public void readBitfield () {
		byte[] data = new byte [5];
		
		try {
			fromPeer.read(data);
			
			int x = data[3];
			byte[] bit = new byte[x];
			fromPeer.read(bit);
		} catch (Exception e) {
			System.out.println(e);
		}
	}
	
	public void sendInterest () {
		byte[] data = new byte[5];
		
		try {
			out.flush();
			out.write(length_prefix);
			out.write(KEY_INTERESTED);
			
			
			toPeer.write(out.toByteArray());
			System.out.println("Sent to Peer: " + Arrays.toString(out.toByteArray()));
			
			fromPeer.readFully(data, 0, data.length);
			System.out.println("Received from Peer: " + Arrays.toString(data));
			
			
			
		} catch (Exception e) {
			System.out.println(e);
		}
	}
	
	
	
	public boolean isChoking () {
		return choking;
	}
	
	public boolean isInterested () {
		return interested;
	}
	
	
	public void choke () {
		choking = true;
	}
	
	public void unchoke () {
		choking = false;
	}
	
	public void showInterest () {
		interested = true;
	}
	
	public void uninterested () {
		interested = false;
	}
	
	
}
