package GivenTools;

import static java.util.concurrent.TimeUnit.NANOSECONDS;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

/**
 * @author Min Chai
 * @author Terence Williams
 *
 */

public class Peer {
	private byte[] peer_id;
	private String peer_ip;
	private int peer_port;
	private Socket peerSocket;
	private DataOutputStream toPeer;
	private DataInputStream fromPeer;
	private int block_length;
	private int blocks_per_piece;
	private static TorrentInfo TI;
	private long elapsedTime;
	
	private static int last_piece_length; 
	
	public Peer (byte[] id, String ip, int port) {
		peer_id = id;
		peer_ip = ip;
		peer_port = port;
		TI = RUBTClient.getTorrentInfo();
		block_length = RUBTClient.block_length;
		blocks_per_piece = TI.piece_length / block_length;
		
		//calculates the length of the last piece
		last_piece_length = TI.file_length - ((TI.piece_hashes.length-1) * blocks_per_piece * block_length );
		
		elapsedTime = 0;
	}
	
	public String getPeerID() {
		try {
			return (new String(peer_id,"ASCII"));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public String getPeerIP() {
		return peer_ip;
	}
	
	public int getPeerPort() {
		return peer_port;
	}

	public Socket getSocket() {
		return peerSocket;
	}
	
	public DataOutputStream getOutput() {
		return toPeer;
	}
	
	public DataInputStream getInput() {
		return fromPeer;
	}
	
	public long getElapsedTime () {
		return elapsedTime;
	}
	
	public boolean tryHandshakeAndDownload(byte[] info_hash, String generatedPeerID, ByteBuffer[] piece_hashes) {
    	byte[] peersHandshake = new byte[68]; //28 + 20 + 20 ; fixedHeader, info_Hash, peerID
    	
	    try {
	    	peerSocket = new Socket(peer_ip, peer_port);
	    	toPeer = new DataOutputStream(peerSocket.getOutputStream());
			fromPeer = new DataInputStream(peerSocket.getInputStream());
			
			byte[] handshakeHeader = createHandshakeHeader(info_hash, generatedPeerID);

			//Perform handshake?
			toPeer.write(handshakeHeader);
			fromPeer.readFully(peersHandshake, 0, peersHandshake.length); //read fromPeer and store 68 bytes into peersHandshake
			
			//System.out.println("What I sent.........: " + Arrays.toString(handshakeHeader));
			//System.out.println("Response from server: " + Arrays.toString(peersHandshake));

			//Check if peersHandshake contains the same info_hash as the one inside the tracker AND it has the same peerID has the peerID stored inside this instance of Peer!
			//Extract info_hash and peerID out of the peersHandshake!
			//And call isEqualByteArray(info_hash, peersHandshake.info_hash) and isEqualByteArray(peer_id, peersHandshake.peerID)
			
			if (!checkHandshakeResponse(info_hash, peersHandshake)){
				System.err.println("Peer responded with an invalid handshake.");
				closeResources();
				return false;
			} 
			
			/**
			 * The peer should immediately respond with his own handshake message, which takes the same form as yours. Otherwise drop connection.
			 */
			
			//Download file?
			PeerMessages p = new PeerMessages();
			boolean result;
			
			p.start(this);
			result = p.showInterest();
			
/**
 *  It is likely that at least the last block of the last piece will be smaller than the block request size 
 *  (and the last piece smaller than the piece_length) 
 *  because the total torrent length is unlikely to be evenly divisible by the piece size.
 *  You will need to take this into account when requesting the end of the torrent
 */
			//unchoked and interested, can download file
			if (result) {
				/*Do a loop to request all the pieces...
				**For the number of pieces needed to download the file??????? HOW MANY PIECES DO WE NEED???????????????????????????????????????????
		
					*Find piece length you need to download for the current piece index, apparently only the last piece has a different piece length
					*Make a request to the peer with this piece index, piece length 
					*(request: <len prefix> is 13 and msg ID is 6. The payload: <index><begin><length> 
					
					
					* A peer should respond to a Request message with a ‘Piece’ message that includes the block requested. 
					* Though the message type is called ‘Piece’, 
					* it includes the information for a block, not necessarily a full piece.
					* A Piece message consists of the 4-byte length prefix, 1-byte message ID, and a payload with a 4-byte piece index,
					* 4-byte block offset within the piece in bytes (so far the same as for the Request message),
					* and a variable length block containing the raw bytes for the requested piece.
					* The length of this should be the same as the length requested.
					*
					
					* When all blocks for a piece have been received, 
					* you should perform a hash check to verify that the piece matches what is expected 
					* and you have not been sent bad or malicious data.
					* The ‘pieces’ element in the .torrent metafile includes a string of 20-byte hashes, one for each piece in the torrent. 
					* Note that this is NOT a list, but is a single long string.
					* You should perform a SHA1 hash on the downloaded piece contents 
					* and compare that to the hash provided for that particular piece.
					* If they do not match, you should discard the downloaded piece and request the blocks for it again.
					***Looks like we need something in the torrent file which had a list of hashes i think...****
				
					*Write to peer that you "have" this piece. (verify the piece download with the piece_index just downloaded, PeerMessage.sendHave(piece_index)).
			    	*have: <length prefix> is 5 and message ID is 4. The payload is a zero-based index of the piece that has just been downloaded and verified.
				*/
				
				//The first time you begin the download,
				//you need to contact the tracker and let it know you are starting to download.
				contactTrackerWithStartedEvent();
				System.out.println("Download is starting \n------------ \nPlease wait patiently for download to finish\n");
				 
				long two_minutes = System.nanoTime();
				long keep_alive = 0;
				
				long started = System.nanoTime();
				
				for (int i = 0; i < piece_hashes.length; i++) { //piece_hashes.length - number of pieces to download
					//System.out.println("Requesting piece index: " + (i+1));
					ByteArrayOutputStream piece = new ByteArrayOutputStream ();
					int x = 0;
					
					keep_alive = System.nanoTime() - two_minutes;
					
					if(NANOSECONDS.toMinutes(keep_alive) >= 2){
						p.keepAlive();
						two_minutes = System.nanoTime();
					}
						
					if(i+1 == piece_hashes.length){
						
						int temp = last_piece_length;
						byte[] resultingPiece;
						
						while(temp > 0){
							
							if(temp > block_length){
								p.request(i, x, block_length);
								resultingPiece = p.getPiece(block_length);
							}else{
								p.request(i, x, temp);
								 resultingPiece= p.getPiece(temp);
							}
							
							temp -= block_length;
		 					x+= block_length;
							piece.write(resultingPiece);	
						}
						
					}else{
					
						// gets all the blocks that make up a given piece
						for(int j = 0; j < blocks_per_piece; j++){
		 					p.request(i, x, block_length);
		 					byte[] resultingPiece = p.getPiece(block_length);
		 					x+= block_length;
							piece.write(resultingPiece);	
						}
					}
					//System.out.println ("getPiece result: " + Arrays.toString(piece.toByteArray()) );
					//VERIFY SHA-1 HASH
					/** 
					 *  has an SHA1 hash for each piece of the file and the pieces are verified as the finish downloading, 
					 *  and are discarded if they fail to match the hash, indicating something wrong was transmitted to you.
					 *  
					 **  The MessageDigest class provides the functionality of a message digest algorithm, such as MD5 or SHA. 
					 *  Message digests are secure one-way hash functions that take arbitrary-sized data and output a fixed-length hash value.
					 *	
					 * getInstance(String)
					Generates a MessageDigest object that implements the specified digest algorithm.	
					
					 *  public byte[] digest(byte input[])
					Performs a final update on the digest using the specified array of bytes, then completes the digest computation. That is, this method first calls update on the array, then calls digest().
						Parameters:
							input - the input to be updated before the digest is completed.
						Returns:
							the array of bytes for the resulting hash value.
					
					 * isEqual(byte[], byte[])
					Compares two digests for equality.
					
					If not equal, resend request
					 */
					byte[] SHA1digest = digestToSHA1(piece.toByteArray());
					if (isEqualSHA1(piece_hashes[i].array(), SHA1digest)) {
						if( (i+1) % 10 == 0)
							System.out.println("Piece " + (i+1) +" verified");
						p.sendHave(i);
					} else {
						System.out.println("Piece " + (i+1) +" IS NOT verified");
						
						//invalid piece need to re-send request for that piece
						i--;
						continue;
					}
					
					writeToFile(piece.toByteArray());
				}
				
				elapsedTime = System.nanoTime() - started;
			}
			
			closeResources();
	    }
	    catch (IOException e) {
	    	System.err.println("Could not perform handshake and download file: " + e);
	    }
	    
	    return true;
	}
	
	private byte[] digestToSHA1(byte[] buffer) {
		
		MessageDigest md = null;
		try {
			md = MessageDigest.getInstance("SHA-1");
		} catch (NoSuchAlgorithmException e) {
			System.err.println("Failed to convert bytes to SHA-1: " + e);
		}
		
		md.update(buffer);
		byte[] digest = md.digest(); //SHA-1 bytes
		
		return digest;
	}
	
	//digestToSHA1 then compare with piece_hash...
	private boolean isEqualSHA1(byte[] piece_hash, byte[] downloaded_hash) {
		//System.out.println(Arrays.toString(piece_hash));
		//System.out.println(Arrays.toString(downloaded_hash));
		if (MessageDigest.isEqual(piece_hash, downloaded_hash)) {
			return true;
		} else {
			return false;
		}
	}
	
	//you need to contact the tracker and let it know you are STARTING the download
	private void contactTrackerWithStartedEvent() throws MalformedURLException {
		URL url = TI.announce_url; 
		int portno = url.getPort();
		URL tracker = null;
		String getRequest = null;
		HttpURLConnection tracker_connect = null;
		String hash = null;
		
		try {
			hash = URLEncoder.encode(new String(TI.info_hash.array(), "ISO-8859-1"),"ISO-8859-1");
			getRequest = url +
					String.format("?info_hash=%s&peer_id=%S&port=%s&uploaded=0&downloaded=0&left=%s&event=started", //**"started"**
					hash, RUBTClient.getGeneratedPeerID(), portno, TI.file_length);
			tracker = new URL(getRequest);
		} catch (UnsupportedEncodingException e) {
			System.err.println("Could not contact tracker with started event: " + e);
		}
		
		try {
			tracker_connect = (HttpURLConnection)tracker.openConnection();
		} catch (IOException e) {
			System.err.println("Could not contact tracker with started event: " + e);
		}
		return;
	}
	
	/**
	 * Handshaking between peers begins with byte nineteen followed by the string 'BitTorrent protocol'.
	 ** After the fixed headers are 8 reserved bytes which are set to 0. 
	 * Next is the 20-byte SHA-1 hash of the bencoded form of the info value from the metainfo (.torrent) file.
	 * The next 20-bytes are the peer id generated by the client. 
	 * The info_hash should be the same as sent to the tracker, and the peer_id is the same as sent to the tracker. 
	 * If the info_hash is different between two peers, then the connection is dropped.
	 **/
	private byte[] createHandshakeHeader(byte[] info_hash, String generatedPeerID) {
		ByteArrayOutputStream header = new ByteArrayOutputStream();
		byte[] fixedHeader = {19, 'B','i','t','T','o','r','r','e','n','t',' ', 'p','r','o','t','o','c','o','l',0,0,0,0,0,0,0,0};
		
		try {
			header.write(fixedHeader);
			header.write(info_hash);
			header.write(generatedPeerID.getBytes());
//			System.out.println(Arrays.toString(header.toByteArray()));
		} catch (IOException e) {
			System.err.println("Failed to generate handshake header.");
		}
		
		return header.toByteArray();
	}
	
	public boolean checkHandshakeResponse(byte[] info_hash, byte[] peersHandshake) {
		byte[] fixedHeader = {19, 'B','i','t','T','o','r','r','e','n','t',' ', 'p','r','o','t','o','c','o','l',0,0,0,0,0,0,0,0}; //Used for checking
		byte[] peersHeader = new byte[28];
		byte[] peersInfoHash = new byte[20];
		byte[] peersID = new byte[20];
		
		//From peersHandshake starting at index 0, copy 28 bytes into peersHeader starting at index 0
		System.arraycopy(peersHandshake, 0, peersHeader, 0, 28); 
		//Check if valid fixed header
//		System.out.println(Arrays.toString(peersHeader));
//		System.out.println(Arrays.toString(fixedHeader));
		if (!isEqualByteArray(fixedHeader, peersHeader)) {
			System.out.println("The header is wrong");
			return false;
		}
		
		//From peersHandshake starting at index 28, copy 20 bytes into peersInfo starting at index 0
		System.arraycopy(peersHandshake, 28, peersInfoHash, 0, 20);
		if (!isEqualByteArray(info_hash, peersInfoHash)) {
			System.out.println("The info hash is wrong");
			return false;
		}
		
		//From peersHandshake starting at index 48, copy 20 bytes into peersID starting at index 0
		System.arraycopy(peersHandshake, 48, peersID, 0, 20);
		if (!isEqualByteArray(peer_id, peersID)) {
			return false;
		}
		
		return true;
	}
	
	//Use Arrays.equals() if you want to compare the actual content of arrays that contain primitive types values (like byte).
	//Checks if two byte arrays are equal
	public boolean isEqualByteArray(byte[] b1, byte[] b2) {
		if (Arrays.equals(b1, b2)) {
			return true;
		} else {
			return false;
		}
	}
	
	//Writes bytes to a filepath. Will be used to write downloaded file into provided file path at args[1]
	private static void writeToFile(byte[] bytes) {
			
		try {
		    RUBTClient.file_stream.write(bytes);
    	} catch (IOException e) {
    		System.err.println("Writing to filestream failed: " + e);
		}
	}
	
	private void closeResources() {
		try {
			toPeer.close();
			fromPeer.close();
			peerSocket.close();
		} catch (IOException e) {
			System.err.println("Closing resources failed: " + e);
		}
	}
	
	public void printPeer() {
		System.out.println("---"); 
		System.out.println("peerID: " + getPeerID());
		System.out.println("peerIP: " + getPeerIP());
		System.out.println("peerPort: " + getPeerPort());
	}
}