package GivenTools;

public class PeerMessages {
	
	private boolean choking;
	private boolean interested;
	private Peer peer;
	
	
	private static final byte[] keep_alive = {0,0,0,0};
	
	private static final byte[] length_prefix = {0,0,01};
	
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
