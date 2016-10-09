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
import java.net.Socket;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


/**
 * @author chai1
 *
 */
public class RUBTClient {

	/**
	 * @param args
	 */
	private static int interval;
	private static List<Peer> peers;
	
	public static void main(String[] args) {
		String path = "src/GivenTools/newfile.txt";
		URL url = null;
		String hostname = null;
		int portno = -1;
		HashMap tracker_info = null;
		
		//Open the torrent file and retrieve the TorrentInfo
		TorrentInfo TI = openTorrent(args[0]);
		url = getURL(TI);
		//Get the host and portno by using the TorrentInfo
		hostname = url.getHost();
		portno = url.getPort();
		System.out.println(hostname);
		System.out.println(portno);
		//Open connection...
		
		//connects to the tracker and retrieves the interval and peer list data
		tracker_info = connectTracker(TI, url, hostname, portno);
		interval = ((Integer)tracker_info.get("interval".getBytes())).intValue();
		buildPeerList(tracker_info);
		
		
		
	}
	
	//Gets TorrentInfo from torrent file
	private static TorrentInfo openTorrent (String args0) {
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
			System.exit(1);
		} catch (final IOException e) {
			System.exit(1);
		}
		
		try {
			TI = new TorrentInfo(bytes);
		} catch (BencodingException e) {
			e.printStackTrace();
		}
		return TI;
	}
	
	private static URL getURL (TorrentInfo TI) {
		URL	url = TI.announce_url;
		return url;
	}
	
	private static HashMap connectTracker(TorrentInfo TI, URL url, String hostname, int portno){
		URL tracker = null;
		Socket sock = null;
		String getRequest = null;
		HttpURLConnection tracker_connect = null;
		HashMap decode = null;
		
		//opens socket to communicate with tracker
		try{
			sock = new Socket (hostname,portno);
		}catch (Exception e){
			System.out.println(e);
		}
		
		//creating tracker connection url
		try{
			getRequest = url +
					String.format("?info_hash=%s&peer_id=ABCDEFGHIJKLMNOPQRST&port=%s&uploaded=0&downloaded=0&left=%s", 
					TI.info_hash,portno,TI.file_length);
			tracker = new URL(getRequest);
		}catch(Exception e){
			System.out.println(e);
		}
		
		//Making the connection with the tracker 
		try {
			tracker_connect = (HttpURLConnection)tracker.openConnection();
		}catch (Exception e){ 
			System.out.println(e);
		}
		
		//try reading info from tracker
		try{
			BufferedInputStream tracker_response = new BufferedInputStream(tracker_connect.getInputStream());
			ByteArrayOutputStream write_bytes = new ByteArrayOutputStream();
			byte[] bytes = new byte[1];
			
			while(tracker_response.read(bytes) != -1)
				write_bytes.write(bytes);
			
			byte[] response = write_bytes.toByteArray();
			decode = (HashMap)Bencoder2.decode(response);	
		}catch (Exception e){
			System.out.println(e);
		}
		
		//close socket
		try{
			sock.close();
		}catch(Exception e){
			e.printStackTrace();
		}
		
		return decode;
		
	}
	
	private static void buildPeerList(HashMap info){
		ArrayList list = (ArrayList)info.get("peers".getBytes());
		
		for(int i = 0; i < list.size(); i++){
			HashMap peer_info = (HashMap)list.get(i);
			
			//gets peer id, ip and port
			String peer_id = new String(((ByteBuffer)peer_info.get("peer id".getBytes())).array());
			String ip = new String(((ByteBuffer)peer_info.get("ip".getBytes())).array());
			int port = ((Integer)peer_info.get("port".getBytes())).intValue();
			
			//creates new peer and adds him to the peer list
			//need to implement check according to assignment
			// use only the peers at IP address with peer_id prefix RUBT11
			Peer p = new Peer(peer_id, ip, port);
			peers.add(p);
		}
	}
	
	private static void WriteToFile (String path) {
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
