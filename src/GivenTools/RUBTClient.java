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
import java.net.HttpURLConnection;
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
	public final static ByteBuffer KEY_INTERVAL = 
			ByteBuffer.wrap(new byte[]{ 'i', 'n', 't', 'e','r','v','a','l' });
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
	
	private static int interval;
	private static List<Peer> peers;
	private static String info_hash = null;
	//The info_hash should be the same as sent to the tracker, and the peer_id is the same as sent to the tracker.
	//If the info_hash is different between two peers, then the connection is dropped.
	
	public static void main(String[] args) {
		String path = "src/GivenTools/newfile.txt";
		URL url = null;
		String hostName = null;
		int portno = -1;
		HashMap tracker_info = null;
		TorrentInfo TI = null;
		//set args[0] ** Run -> Run Configurations -> Arguments -> {Type in args}
		//Open the torrent file and retrieve the TorrentInfo
		TI = openTorrent("src/GivenTools/CS352_Exam_Solutions.mp4.torrent");
		url = getURL(TI);
		//Get the host and portno by using the TorrentInfo
		hostName = url.getHost();
		portno = url.getPort();
		System.out.println(hostName);
		System.out.println(portno);
		
		//connects to the tracker and retrieves the interval and peer list data
		tracker_info = connectTracker(TI, url, portno);
		
		interval = ((Integer)tracker_info.get(KEY_INTERVAL)).intValue();
		buildPeerList(tracker_info);
		
		//prints out the info decoded from the tracker
		ToolKit.print(tracker_info);
		
		//Communicate with the peers
		/*To do*/
		
		//write downloaded file to location specified by args[1]
		/*To do*/
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
			hash = URLEncoder.encode(new String(TI.info_hash.array(), "ISO-8859-1"),"ISO-8859-1");
			System.out.println(hash);
			info_hash = hash; //Is this right, Terence? we need to check info_hash between two peers
			
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
		
		for (int i = 0; i < list.size(); i++) {
			HashMap peer_info = (HashMap)list.get(i);
			
			//gets peer id, ip and port
			String peer_id = new String(((ByteBuffer)peer_info.get(KEY_PEER_ID)).array());
			String ip = new String(((ByteBuffer)peer_info.get(KEY_PEER_IP)).array());
			int port = ((Integer)peer_info.get(KEY_PEER_PORT)).intValue();
			
			//creates new peer and adds him to the peer list
			//need to implement check according to assignment
			// use only the peers at IP address with peer_id prefix RU11
			Peer p = new Peer(peer_id, ip, port);
			peers.add(p);
		}
	}
	
	//Generates a random peerId with length 20
	private static String generatePeerID() {
		String s = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
		Random r = new Random();
		String peerID = "";
		
		for (int i = 0; i < 20; i++)
			peerID += s.charAt(r.nextInt(s.length()));
		
		return peerID;
	}
	
	/**
	 * Handshaking between peers begins with byte nineteen followed by the string 'BitTorrent protocol'.
	 * After the fixed headers are 8 reserved bytes which are set to 0. 
	 * Next is the 20-byte SHA-1 hash of the bencoded form of the info value from the metainfo (.torrent) file.
	 * The next 20-bytes are the peer id generated by the client. 
	 * The info_hash should be the same as sent to the tracker, and the peer_id is the same as sent to the tracker. 
	 * If the info_hash is different between two peers, then the connection is dropped.
	 **/
	private static void handshakePeer() {
		
	}
	
	//Checks the info_hash from torrent info and a peer, this may not be right... Help Terence :) are the hashes Strings or bytes?
	private static boolean checkInfoHash(String peersHash) {
		if (info_hash.equals(peersHash)) {
			return true;
		} else {
			return false;
		}
	}
		
	//Writes bytes to a filepath. Will be used to write downloaded file into provided file path at args[1]
	private static void writeToFile(String path) {
		try {
			File file = new File(path);
			String hello = "Hello World \n Hi";
			byte[] bytes = hello.getBytes();
			
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
