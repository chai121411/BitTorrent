/**
 * 
 */
package GivenTools;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

/**
 * @author chai1
 * @author trw63
 *
 */
public class RUBTClient {

	/**
	 * @param args
	 */
	
	/**
     * Key used to retrieve the interval from the tracker 
     */
	//public final static ByteBuffer KEY_INTERVAL = 
			//ByteBuffer.wrap(new byte[]{ 'i', 'n', 't', 'e','r','v','a','l' });
	/**
     * Key used to retrieve the peer list from the tracker 
     */
	public final static ByteBuffer KEY_PEERS = 
			ByteBuffer.wrap(new byte[]{ 'p', 'e', 'e', 'r','s'});
	/**
     * Key used to retrieve the peer id from the tracker 
     */
	public final static ByteBuffer KEY_PEER_ID = 
			ByteBuffer.wrap(new byte[]{ 'p', 'e', 'e', 'r',' ','i','d'});
	/**
     * Key used to retrieve the peer port from the tracker 
     */
	public final static ByteBuffer KEY_PEER_PORT = 
			ByteBuffer.wrap(new byte[]{ 'p', 'o', 'r', 't'});
	/**
     * Key used to retrieve the peer ip from the tracker 
     */
	public final static ByteBuffer KEY_PEER_IP = 
			ByteBuffer.wrap(new byte[]{ 'i', 'p'});
	
	//block length set to 2 ^ 14
	public static int block_length = 16384;
	
	//private static int interval;
	private static List<Peer> peers;
	
	//The info_hash should be the same as sent to the tracker, and the peer_id is the same as sent to the tracker.
	//If the info_hash is different between two peers, then the connection is dropped.
	private static byte[] info_hash = null;
	private static String generatedPeerID = null;
	private static ByteBuffer[] piece_hashes = null; //The SHA-1 hash of each piece!
	private static TorrentInfo TI;
	
	
	public static void main(String[] args) {
		String path = "src/GivenTools/newfile.mp4";
		URL url = null;
		String hostName = null;
		int portno = -1;
		HashMap tracker_info = null;
		TI = null;
		
		//set args[0] ** Run -> Run Configurations -> Arguments -> {Type in args}
		//Open the torrent file and retrieve the TorrentInfo
		TI = openTorrent("src/GivenTools/CS352_Exam_Solutions.mp4.torrent");
		url = getURL(TI);
		//Get the host and portno by using the TorrentInfo
		hostName = url.getHost();
		portno = url.getPort();
		//System.out.println(hostName);
		//System.out.println(portno);
		
		//System.out.println(TI.piece_length);
		//System.out.println(TI.piece_hashes); //?
 		//System.out.println(TI.piece_hashes.length); //"Our download time is quite long for all 511 pieces." -Sakai forums, this could be it! ***
		//May need these piece_hashes to do the SHA-1 verification to check the piece downloaded...
		
 		piece_hashes = TI.piece_hashes;
		
		//connects to the tracker and retrieves the interval and peer list data
		tracker_info = connectTracker(TI, url, portno);
		
		//interval = ((Integer)tracker_info.get(KEY_INTERVAL)).intValue();
		buildPeerList(tracker_info);
		
		//prints out the info decoded from the tracker
		//ToolKit.print(tracker_info);
		
		//Look at list of peers
		for (Peer peer : peers) {
			//peer.printPeer();
			peer.tryHandshakeAndDownload(info_hash, generatedPeerID, piece_hashes); //Pass info_hash and generatedpeerid to create handshakeheader
			//Pass piece_hashes to verify SHA-1 of each download for each piece, did not do it yet*
		}
		
		//write downloaded file to location specified by args[1]
		/*To do*/
		
	    //8.    When the file is finished, you must contact the tracker and send it the completed event and properly close all TCP connections
		try {
			contactTrackerWithCompletedEvent();
		} catch (MalformedURLException e) {
			System.err.println("Could not contact tracker with completed event.");
		}

		
	}
	
	//Gets TorrentInfo from torrent file
	private static TorrentInfo openTorrent(String args0) {
		byte[] bytes = null;
		File file = null;
		TorrentInfo TI = null;

		try {
			file = new File(args0);
			DataInputStream dis = new DataInputStream(new FileInputStream(file));
			bytes = new byte[(int) file.length()];
			dis.readFully(bytes);
			dis.close();
		} catch (final FileNotFoundException e) {
			System.out.println("The torrent file was not found " + args0);
			System.exit(1);
		} catch (final IOException e) {
			System.out.println("IO Exception. Exiting.");
			System.exit(1);
		}
		
		try {
			TI = new TorrentInfo(bytes);
		} catch (BencodingException e) {
			e.printStackTrace();
		}
		return TI;
	}
	
	private static URL getURL(TorrentInfo TI) {
		URL	url = TI.announce_url;
		return url;
	}
	
	public static String getGeneratedPeerID() {
		return generatedPeerID;
	}
	
	public static TorrentInfo getTorrentInfo () {
		return TI;
	}
	
	private static HashMap connectTracker(TorrentInfo TI, URL url, int portno) {
		URL tracker = null;
		String getRequest = null;
		String peerID = null;
		HttpURLConnection tracker_connect = null;
		HashMap decode = null;
		String hash = null;
		
		//creating tracker connection url
		try {
			peerID = generatePeerID();
			generatedPeerID = peerID;
			hash = URLEncoder.encode(new String(TI.info_hash.array(), "ISO-8859-1"),"ISO-8859-1");
			info_hash = TI.info_hash.array();
			
			getRequest = url +
					String.format("?info_hash=%s&peer_id=%S&port=%s&uploaded=0&downloaded=0&left=%s", 
					hash,peerID,portno,TI.file_length);
			tracker = new URL(getRequest);
		} catch (Exception e) {
			System.out.println(e);
		}
		
		//Making the connection with the tracker 
		try {
			tracker_connect = (HttpURLConnection)tracker.openConnection();
		} catch (Exception e) { 
			System.out.println(e);
		}
		
		//try reading info from tracker
		try {
			BufferedInputStream tracker_response = new BufferedInputStream(tracker_connect.getInputStream());
			ByteArrayOutputStream write_bytes = new ByteArrayOutputStream(); //write_bytes just holds the bytes; not being sent
			byte[] bytes = new byte[1];
			
			while(tracker_response.read(bytes) != -1)
				write_bytes.write(bytes);
			
			byte[] response = write_bytes.toByteArray();
			
			//ToolKit.print(response);//info before it's decoded
			decode = (HashMap)Bencoder2.decode(response);	
		} catch (Exception e) {
			System.out.println(e);
			System.exit(1);
		}
		
		
		return decode;
	}
	
	private static void buildPeerList(HashMap info){
		ArrayList list = (ArrayList)info.get(KEY_PEERS);
		peers = new ArrayList<Peer>();
		CharSequence cs= "-RU";
		
		for (int i = 0; i < list.size(); i++) {
			HashMap peer_info = (HashMap)list.get(i);
			
			//gets peer id, ip and port
			String peer_id = new String(((ByteBuffer)peer_info.get(KEY_PEER_ID)).array());
			String ip = new String(((ByteBuffer)peer_info.get(KEY_PEER_IP)).array());
			int port = ((Integer)peer_info.get(KEY_PEER_PORT)).intValue();
			
			//creates new peer and adds it to the peer list
			// use only the peers with peer_id prefix -RU
			if(peer_id.contains(cs)){
				Peer p = new Peer(peer_id, ip, port);
				peers.add(p);
			}
			
		}
	}
	
	//Generates a random peerId with length 20
	private static String generatePeerID() {
		String s = "0123456789ABCDEFGHIJKLMNOPQSTVWXYZabcdefghijklmnopqstvwxyz";
		Random r = new Random();
		String peerID = "";
		
		for (int i = 0; i < 20; i++)
			peerID += s.charAt(r.nextInt(s.length()));
		
		return peerID;
	}
	
	//you need to contact the tracker and let it know you are completing the download
	private static void contactTrackerWithCompletedEvent() throws MalformedURLException {
		URL url = TI.announce_url; 
		int portno = url.getPort();
		URL tracker = null;
		String getRequest = null;
		HttpURLConnection tracker_connect = null;
		String hash = null;
		
		try {
			hash = URLEncoder.encode(new String(TI.info_hash.array(), "ISO-8859-1"),"ISO-8859-1");
			getRequest = url +
					String.format("?info_hash=%s&peer_id=%S&port=%s&uploaded=0&downloaded=%s&left=0&event=completed", //**"completed"**
					hash, RUBTClient.getGeneratedPeerID(), portno, TI.file_length);
			tracker = new URL(getRequest);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		
		try {
			tracker_connect = (HttpURLConnection)tracker.openConnection();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return;
	}
	
	//Writes bytes to a filepath. Will be used to write downloaded file into provided file path at args[1]
	private static void writeToFile(byte[] bytes, String path) {
		try {
			File file = new File(path);
			
			System.out.println("Writing to new file");
			
			FileOutputStream stream = new FileOutputStream(file);
			try {
			    stream.write(bytes);
			} finally {
			    stream.close();
			}
    	} catch (IOException e) {
	      e.printStackTrace();
		}
	}

}
