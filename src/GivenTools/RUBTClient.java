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
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import static java.util.concurrent.TimeUnit.NANOSECONDS;

/**
 * @author Min Chai 
 * @author Terence Williams
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
	public static final int block_length = 16384;
	
	//private static int interval;
	private static List<Peer> peers;
	
	//The info_hash should be the same as sent to the tracker, and the peer_id is the same as sent to the tracker.
	//If the info_hash is different between two peers, then the connection is dropped.
	public static byte[] info_hash = null;
	private static String generatedPeerID = null;
	public static ByteBuffer[] piece_hashes = null; //The SHA-1 hash of each piece!
	private static TorrentInfo TI;
	public static FileOutputStream file_stream;
	public static int peersListLength;
	public static int threadID = 0; //Used to give each Peer a threadID
	public static int portno = -1;
	public static byte[][] downloadedPieces = null;  //Buffer to store downloadedPieces

	
	public static void main(String[] args) throws InterruptedException {
		URL url = null;

		HashMap tracker_info = null;
		TI = null;
		
//		TI = openTorrent("src/GivenTools/CS352_Exam_Solutions.mp4.torrent");
		TI = openTorrent(args[0]);
		url = getURL(TI);
		
		//Get the host and portno by using the TorrentInfo
//		hostName = url.getHost();
		portno = url.getPort();
		
 		piece_hashes = TI.piece_hashes;
		
		//connects to the tracker and retrieves the interval and peer list data
		tracker_info = connectTracker(TI, url, portno);

		//interval = ((Integer)tracker_info.get(KEY_INTERVAL)).intValue();
		buildPeerList(tracker_info);
		
//		String path = "src/GivenTools/newfile.mov";
		createFileStream(args[1]);
		
		//Initialize buffer size to number of pieces expected to download
		downloadedPieces = new byte[piece_hashes.length][];
		
		//prints out the info decoded from the tracker
		ToolKit.print(tracker_info);
		
		System.out.println("My generatedPeerID: " + generatedPeerID);
		System.out.println("Peer list size: " + getPeersListLength());
		
		//used to get the index of the peer with the lowest average RTT
		int index = 0;
		long min = Long.MAX_VALUE;
		
//Phase II leftover
//		//Look at list of peers and computes the average lowest RTT
//		for (Peer peer : peers) {
//			String host = peer.getPeerIP();
//			long x = 0;
//			long y = 0;
//			long sum = 0;
//			long avg = 0;
//			
//			for(int i = 0; i < 10; i++){
//				
//				try {
//					x = System.nanoTime();
//					InetAddress.getByName(host).isReachable(5000);
//					y = System.nanoTime() - x;
//				} catch (UnknownHostException e) {
//					e.printStackTrace();
//				} catch (IOException e) {
//					e.printStackTrace();
//				}
//				
//				sum += y;	
//			}
//			
//			
//			avg = sum/ (long)10;
//			
//			if(avg < min){
//				min = avg;
//				index = peers.indexOf(peer);
//				
//			}
//			
//			peer.printPeer();
//			System.out.println("RRT for peer: " + avg + " ns");
//		}
//		System.out.println("*************************************");
//		System.out.println("Remote peer we are downloading from: **********************");
//		peers.get(index).printPeer();
		//downloads file
		
		//The first time you begin the download,
		//you need to contact the tracker and let it know you are starting to download.
		try {
			contactTrackerWithStartedEvent();
		} catch (MalformedURLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		System.out.println("Download is starting \n------------ \nPlease wait patiently for download to finish\n");
		
		//Have two threads to listen for incoming connections and send requested pieces
		for	(int i = 0; i < 2; i++) {
			IncomingPeer listenIncomingPeer = new IncomingPeer(threadID);
			threadID++;
			Thread t = new Thread(listenIncomingPeer);
			t.start();
		}
		
		//Make a list of downloading threads to join(to block the main method)
		List<Thread> downloading_threads = new ArrayList<Thread>();
		
		for (Peer peer : peers) {
			Thread downloading = new Thread(peer);
			downloading_threads.add(downloading);
			downloading.start();
		}
		
		// Allow downloading threads to finish before continuing the main method
		for (Thread downloading : downloading_threads) {
			downloading.join();
		}
		
		System.out.println("Writing downloaded buffer to stream...");
		//Write all pieces to file
		for (byte[] piece : downloadedPieces) {
			try {
				file_stream.write(piece);
			} catch (IOException e) {
				System.err.println("Failed to write piece to stream: " + e);
			}
		}
		
		System.out.println("Finished writing buffer to stream.");
		System.out.println("Contacting tracker with completed event...");
	    //When the file is finished, you must contact the tracker and send it the completed event and properly close all TCP connections
		try {
			contactTrackerWithCompletedEvent();
		} catch (MalformedURLException e) {
			System.err.println("Could not contact tracker with completed event.");
		}
		
		System.out.println("Contacting tracker with stopped event...");
		//Before the client exits it should send the tracker the stop event
		try {
			contactTrackerWithStoppedEvent();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		
		long downloadTime = peers.get(index).getElapsedTime();
		
		System.out.println("----------------------------------------------");
		System.out.println("Total download time: " + NANOSECONDS.toMinutes(downloadTime) + " mins");
		
		try {
			file_stream.close();
		} catch (IOException e) {
			System.err.println("Failed to close file_stream: " + e);
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
			System.err.println("The torrent file was not found: " + e);
		} catch (final IOException e) {
			System.err.println("Failed to open torrent file: " + e);
		}
		
		try {
			TI = new TorrentInfo(bytes);
		} catch (BencodingException e) {
			System.err.println("Failed to get TorrentInfo: " + e);
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
	
	public static int getPeersListLength() {
		return peersListLength;
	}
	
	public static void createFileStream (String path) {
		File file = new File (path);
		
		try {
			file_stream = new FileOutputStream(file);
		} catch (FileNotFoundException e) {
			System.err.println("Failed to create a fileoutputstream: " + e);
		}
		
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
			System.err.println("Failed to create getRequest in connectTracker: " + e);
		}
		
		//Making the connection with the tracker 
		try {
			tracker_connect = (HttpURLConnection)tracker.openConnection();
		} catch (Exception e) { 
			System.err.println("Failed to connect to tracker: " + e);
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
			tracker_connect.disconnect();
			System.err.println("Failed to read bytes from tracker: " + e);
		}
		
		tracker_connect.disconnect();
		return decode;
	}
	
	private static void buildPeerList(HashMap info){
		ArrayList list = (ArrayList)info.get(KEY_PEERS);
		peers = new ArrayList<Peer>();
		CharSequence cs= "-RU";
		
		for (int i = 0; i < list.size(); i++) {
			HashMap peer_info = (HashMap)list.get(i);
			String id = null;
			//gets peer id, ip and port
			byte[] peer_id = ((ByteBuffer)peer_info.get(KEY_PEER_ID)).array();
			String ip = new String(((ByteBuffer)peer_info.get(KEY_PEER_IP)).array());
			int port = ((Integer)peer_info.get(KEY_PEER_PORT)).intValue();
			
			try {
				id = (new String(peer_id,"ASCII"));
				
				//creates new peer and adds it to the peer list
				// use only the peers with peer_id prefix -RU
				if (id.contains(cs)) {
					Peer p = new Peer(peer_id, ip, port, threadID);
					peers.add(p);
					threadID++; //Increment for a new unused threadID
				}
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		}
		
		peersListLength = peers.size();	
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
	
	//you need to contact the tracker and let it know you are STARTING the download
	private static void contactTrackerWithStartedEvent() throws MalformedURLException {
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
			System.err.println("Failed to contact tracker with completed event: " + e);
		}
		
		try {
			tracker_connect = (HttpURLConnection)tracker.openConnection();
			//System.out.println("Contacted tracker to verify download was successful");
			tracker_connect.disconnect();
		} catch (IOException e) {
			System.err.println("Failed to contact tracker with completed event: " + e);
		}
		return;
	}
	
	private static void contactTrackerWithStoppedEvent() throws MalformedURLException {
		URL url = TI.announce_url; 
		int portno = url.getPort();
		URL tracker = null;
		String getRequest = null;
		HttpURLConnection tracker_connect = null;
		String hash = null;
		
		try {
			hash = URLEncoder.encode(new String(TI.info_hash.array(), "ISO-8859-1"),"ISO-8859-1");
			getRequest = url +
					String.format("?info_hash=%s&peer_id=%S&port=%s&uploaded=0&downloaded=%s&left=0&event=stopped", //**"stopped"**
					hash, RUBTClient.getGeneratedPeerID(), portno, TI.file_length);
			tracker = new URL(getRequest);
		} catch (UnsupportedEncodingException e) {
			System.err.println("Failed to contact tracker with stopped event: " + e);
		}
		
		try {
			tracker_connect = (HttpURLConnection)tracker.openConnection();
			//System.out.println("Contact tracker that client connection is closing");
			tracker_connect.disconnect();
		} catch (IOException e) {
			System.err.println("Failed to contact tracker with stopped event: " + e);
		}
		return;
	}

}
