package GivenTools;

public class PeerMessage {
	
	private int lengthPrefix;
	private byte id;
	
	public int getLengthPrefix() {
		return lengthPrefix;
	}

	public byte getId() {
		return id;
	}
	
	/**
	 * Below is a list of messages that need to be implemented for this assignment.
	 * 
	 * keep-alive: <length prefix> is 0. There is no message ID and no payload. These should be sent around once every 2 minutes to prevent peers from closing connections. These only need to be sent if no other messages are sent within a 2-minute interval.
	 * choke: <length prefix> is 1 and message ID is 0. There is no payload.
	 * unchoke: <length prefix> is 1 and the message ID is 1. There is no payload.
	 * interested: <length prefix> is 1 and message ID is 2. There is no payload.
	 * uninterested: <length prefix> is 1 and message ID is 3. There is no payload.
	 * 
	 * have: <length prefix> is 5 and message ID is 4. 
	 * request: <length prefix> is 13 and message ID is 6. 
	 * piece: <length prefix> is 9+X and message ID is 7.
	 **/
	
	//No payloads
	public static byte keepAliveID = -1;
	public static byte chokeID = 0;
	public static byte unchokeID = 1;
	public static byte interestedID = 2;
	public static byte uninterestedID = 3;
	
	//Payloads
	public static byte haveID = 4;
	public static byte requestID = 6;
	public static byte pieceID = 7;
	
	/**
	 * After the handshake, messages between peers take the form of <length prefix><message ID><payload>
	 * The length prefix is a four byte big-endian value. 
	 * The message ID is a single decimal byte. 
	 * The payload depends on the message.
	 */
	public PeerMessage(int lengthPrefix, byte id) {
		this.lengthPrefix = lengthPrefix;
		this.id = id;
	}
}
