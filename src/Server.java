import java.io.IOException;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**Server - Implements a concurrent UDP and TCP server.
 * 
 * @author scobb
 *
 */
public class Server {
	/**TCPHandler - inner class that will handle TCP requests so TCPListener can continue listening
	 * 
	 * @author scobb
	 *
	 */
	public class TCPHandler implements Runnable {
		// member vars
		Socket socket;
		
		// constructor: listener will give us a socket to work with.
		public TCPHandler(Socket s){
			this.socket = s;
		}
		@Override
		public void run() {
			try {
				Scanner in = new Scanner(socket.getInputStream());
				PrintWriter out = new PrintWriter(socket.getOutputStream());
				String req = in.nextLine();
				String resp = Server.this.processRequest(req);
				out.println(resp);
				out.flush();
		        out.close();
		        socket.close();
			} catch (IOException e) {
				e.printStackTrace();
			} 
		}
	}
	/**TCPListener - class that will listen on the assigned port for TCP requests
	 * 
	 * @author scobb
	 *
	 */
	public class TCPListener implements Runnable{
		// base constructor - nothing to do
		public TCPListener(){};
		@Override
		public void run() {
			// use outer class's tcpSocket to listen
			Socket s;
			try {
				while ( (s = tcpSocket.accept()) != null) {
					// when we get a connection, sping off a thread to handle it
					threadpool.submit(new TCPHandler(s));
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		}
		
	}
	/**UDPListener - Will listen for UDP requests
	 * 
	 * @author scobb
	 *
	 */
	public class UDPListener implements Runnable{
		// member vars
		int port;
		Server myServer;
		int len = 1024;
		
		// empty constructor
		public UDPListener(){
		}

		@Override
		public void run() {
			// listen on the outer class's UDP socket for a connection.
			try {
				while (true){
					// allocate a buffer and object for client
					byte[] buf  = new byte[len];
					DatagramPacket datapacket = new DatagramPacket(buf, buf.length);
					
					// wait for client to transmit something
					udpSocket.receive(datapacket);
					
					// read client's info into string
					String req = new String (datapacket.getData());
					
					// process client's request
					String resp = Server.this.processRequest(req);
					
					// convert string response into a byte array
					byte[] respBytes = resp.getBytes();
					
					// build and send response packet
					DatagramPacket returnpacket = new DatagramPacket (
							respBytes,
							respBytes.length,
							datapacket.getAddress () ,
							datapacket.getPort());
					udpSocket.send(returnpacket);
				}
				
			} catch (Exception exc){
				System.out.println("Exception: " + exc.getMessage());
			}
				
		}
	}

	// member variables
	private Map<String, String> bookMap; 
	private ServerSocket tcpSocket;
	private DatagramSocket udpSocket;
	ExecutorService threadpool;
	
	// constants
	private static final int INVALID = -1;
	private final String RESERVE = "reserve";
	private final String RETURN = "return";
	private final String FAIL = "fail ";
	private final String AVAILABLE = "available";
	private final String FREE = "free ";
	
	// empty constructor - instantiate the map that will track who has what books
	public Server(){
		bookMap = new HashMap<String, String>();
	}
	
	/**parseConfig - handles the first line entered to server
	 * 
	 * @param config String configuration, fmt: <numBooks> <updPort> <tcpPort>
	 */
	public void parseConfig(String config) {
		// split based on whitespace
		String[] configList = config.split("\\s+");
		
		// populate book map, 1-based indexing, with all books available
		int numBooks = Integer.parseInt(configList[0]);
		for (int i = 1; i <= numBooks; ++i){
			bookMap.put("b" + i,  AVAILABLE);
		}
		
		// set up udp and tcp sockets for listeners to use
		int udpPort = Integer.parseInt(configList[1]);
		int tcpPort = Integer.parseInt(configList[2]);
		try {
			udpSocket = new DatagramSocket(udpPort);
			//System.out.println("Listening for UDP on : " + udpPort);
			tcpSocket = new ServerSocket(tcpPort);
			//System.out.println("Listening for TCP on : " + tcpPort);
		} catch (SocketException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	/**listen - blocking method that listens for UDP and TCP input
	 * 
	 */
	public void listen(){
		threadpool = Executors.newCachedThreadPool();
		UDPListener udp = new UDPListener();
		threadpool.submit(udp);
		TCPListener tcp = new TCPListener();
		threadpool.submit(tcp);
		while (true);
	}
	/**processRequest - method that updates bookMap based on a client's request.
	 * Synchronized to protect bookMap from multiple concurrent tinkerers.
	 * 
	 * @param request String from client, format: <clientid> <bookid> <directive>
	 * @return String that holds response to client
	 */
	public synchronized String processRequest(String request){
		// parse request into component parts
		String[] requestSplit = request.split("\\s+");
		String client = requestSplit[0].trim();
		String book = requestSplit[1].trim();
		String directive = requestSplit[2].trim();
		String status = bookMap.get(book);
		String prefix = "";
		
		// craft our response and update bookMap based on request
		if (status == null){
			// book not listed - nothing to do except return a failing message
			prefix = FAIL;
		}
		else if (directive.equals(RETURN)){
			// did the client have the book?
			if (!status.equals(client)){
				// no, they didn't.
				prefix = FAIL;
			} else {
				// yes, they did. Make the book available and send a free response.
				bookMap.put(book,  AVAILABLE);
				prefix = FREE;
			}
			
		} else {
			// is the book available?
			if (!status.equals(AVAILABLE)){
				// no, it's not.
				prefix = FAIL;
			} else {
				// yes, it is. Assign it to the client who reserved it.
				bookMap.put(book,  client);
				// prefix already blank, which is correct
			}
		}
		
		// return the string in proper format
		return prefix + client + " " + book;
	}

	public static void main(String[] args){
		// create server object
		Server s = new Server();
		
		// we'll listen to stdin. 
		Scanner sc = new Scanner(System.in);
		
		// first line will be configuration
		s.parseConfig(sc.nextLine());
		
		// after that, we'll poll for TCP and UDP communications
		s.listen();
	}
}
