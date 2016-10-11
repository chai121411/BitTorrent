package GivenTools;

/**
 * @author chai1
 * @author trw63
 *
 */

public class Peer {
	private String peer_id;
	private String peer_ip;
	private int peer_port;
	
	public Peer(String id, String ip, int port){
		peer_id = id;
		peer_ip = ip;
		peer_port = port;
	}
	
	public String getPeerID(){
		return peer_id;
	}
	
	public String getPeerIP(){
		return peer_ip;
	}
	
	public int getPeerPort(){
		return peer_port;
	}
}