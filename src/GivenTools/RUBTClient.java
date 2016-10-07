/**
 * 
 */
package GivenTools;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;

/**
 * @author chai1
 *
 */
public class RUBTClient {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String path = "C:\\Users\\chai1\\Desktop\\newfile.txt";
		URL url = null;
		String hostname = null;
		int portno = -1;
		//Open the torrent file and retrieve the TorrentInfo
		TorrentInfo TI = openTorrent(args[0]);
		url = getURL(TI);
		//Get the host and portno by using the TorrentInfo
		hostname = url.getHost();
		portno = url.getPort();
		System.out.println(hostname);
		System.out.println(portno);
		//Open connection...
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
