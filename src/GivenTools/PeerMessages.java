package GivenTools;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class PeerMessages {
	
	private boolean choking;
	private boolean interested;
	private boolean peer_interested;
	private boolean peer_choking;
	private DataOutputStream toPeer;
	private DataInputStream fromPeer;
	private ByteArrayOutputStream out;
	
	
	private static final byte[] keep_alive = {0,0,0,0};
	
	private static final byte[] length_prefix = {0,0,0,1};
	
	private static final byte[] request_length = {0,0,0,13};
	
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
	 * Key for request message
	 */
	private static final int KEY_REQUEST = 6;
	
	
	public void start (Peer p) {
		choking = true;
		interested = false;
		peer_choking = true;
		peer_interested = false;
		out = new ByteArrayOutputStream();
		toPeer = p.getOutput();
		fromPeer = p.getInput();
		
		readBitfield();
	}
	
	public void request (int index, int begin, int length){
		
		try {
			out.reset();
			out.write(request_length);
			out.write(KEY_REQUEST);
			
			out.write(ByteBuffer.allocate(4).putInt(index).array());
			out.write(ByteBuffer.allocate(4).putInt(begin).array());
			out.write(ByteBuffer.allocate(4).putInt(length).array());
			
			toPeer.write(out.toByteArray());
			System.out.println("toPeer Request in PeerMessages: " + Arrays.toString(out.toByteArray()));
		} catch (Exception e) {
			System.out.println(e);
		}
	}
	
	public byte[] getPiece () {
		byte[] data = new byte [16384];
		
		try {
			fromPeer.readFully(data);
		} catch (Exception e) {
			System.out.println(e);
		}
		
		return data;
	}
	
	public void keepAlive () {
		
		try {
			out.reset();
			out.write(keep_alive);
			
			toPeer.write(out.toByteArray());
		} catch (Exception e) {
			System.out.println(e);
		}
	}
	
	public void readBitfield () {
		byte[] data = new byte [5];
		
		try {
			fromPeer.read(data);
			
			int x = data[3];
			byte[] bit = new byte[x];
			fromPeer.read(bit);
			
			System.out.println(Arrays.toString(bit));
	
		} catch (Exception e) {
			System.out.println(e);
		}
	}
	
	public boolean showInterest () {
		byte[] data = new byte[5];
		try {
			out.flush();
			out.write(length_prefix);
			out.write(KEY_INTERESTED);
			
			toPeer.write(out.toByteArray());
			fromPeer.readFully(data, 0, data.length);
			
			//Server sent back the choke message
			System.out.println("Response to interest " + Arrays.toString(data));
			
			out.reset();
			out.write(length_prefix);
			out.write(KEY_UNCHOKE);
			
			if(Arrays.equals(data, out.toByteArray())){
				choking = false;
				interested = true;
				peer_choking = false;
				peer_interested = true;
				return true;
			}
			
		} catch (Exception e) {
			System.out.println(e);
		}
		
		return false;
	}
	
	public void uninterested () {

		try {
			out.reset();
			out.write(length_prefix);
			out.write(KEY_UNINTERESTED);
			toPeer.write(out.toByteArray());
			
			interested = false;
			
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
	
	public boolean Peer_choking () {
		return peer_choking;
	}
	
	public boolean Peer_interested () {
		return peer_interested;
	}
	
	public void choke () {
		
		try {
			out.reset();
			out.write(length_prefix);
			out.write(KEY_CHOKE);
			toPeer.write(out.toByteArray());
			
			choking = true;
			
		} catch (Exception e) {
			System.out.println(e);
		}	
	}
	
	public void unchoke () {
		try {
			out.reset();
			out.write(length_prefix);
			out.write(KEY_UNCHOKE);
			toPeer.write(out.toByteArray());
			
			choking = false;
			
		} catch (Exception e) {
			System.out.println(e);
		}	
	}
	
}
