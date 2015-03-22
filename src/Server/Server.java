package Server;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import Record.ClientRecord;
import Record.FailureRecord;
import Record.ServerRecord;

/**
 * Server - Implements a concurrent UDP and TCP server.
 * 
 * @author scobb
 *
 */
public class Server {

	// member variables
	private Map<String, String> bookMap;
	private ServerSocket tcpSocket;
	private int numServers;
	private int clock;
	private int serverId;
	private int numServed;
	private List<ServerRecord> serverRecords;
	private List<FailureRecord> scheduledFailures;
	private PriorityQueue<ClientRecord> clientRequests;
	private FailureRecord currentScheduledFailure;
	private ExecutorService threadpool;

	// constants
	private static final int INVALID = -1;
	private final String RESERVE = "reserve";
	private final String RETURN = "return";
	private final String FAIL = "fail ";
	private final String AVAILABLE = "available";
	private final String FREE = "free ";

	// empty constructor - instantiate the map that will track who has what
	// books
	public Server() {
		bookMap = new HashMap<String, String>();
		serverRecords = new ArrayList<ServerRecord>();
		scheduledFailures = new ArrayList<FailureRecord>();
		clientRequests = new PriorityQueue<ClientRecord>();
		currentScheduledFailure = null;
		numServed = 0;
		clock = 0;
	}

	// getters
	public int getNumServed() {
		return numServed;
	}

	public ExecutorService getThreadpool() {
		return threadpool;
	}

	public int getClock() {
		return clock;
	}

	public PriorityQueue<ClientRecord> getClientRequests() {
		return clientRequests;
	}

	public List<ServerRecord> getServerRecords() {
		return serverRecords;
	}

	public int getNumServers() {
		return numServers;
	}

	public int getServerId() {
		return serverId;
	}
	
	public ServerSocket getTcpSocket(){
		return tcpSocket;
	}
	
	public void clientServed(){
		++numServed;
	}

	public FailureRecord getCurrentScheduledFailure() {
		return currentScheduledFailure;
	}
	
	/**fail - clears state and sleeps for requisite period
	 * 
	 * 
	 */
	public void fail(){
		
	}

	/**
	 * parseConfig - handles the first line entered to server
	 * 
	 * @param config
	 *            String configuration, fmt: <numBooks> <updPort> <tcpPort>
	 */
	public void parseConfig(String config) {
		// split based on whitespace
		String[] configList = config.split("\\s+");

		serverId = Integer.parseInt(configList[0]);
		numServers = Integer.parseInt(configList[1]);

		// populate book map, 1-based indexing, with all books available
		int numBooks = Integer.parseInt(configList[2]);
		for (int i = 1; i <= numBooks; ++i) {
			bookMap.put("b" + i, AVAILABLE);
		}

	}

	/**
	 * startServer - starts a server on given port if server is on localhost
	 * 
	 * pre-condition: serverRecords, serverId populated
	 */
	public void startServer() {
		try {
			InetAddress addr = serverRecords.get(serverId).getAddr();
			if (addr.isAnyLocalAddress() || addr.isLoopbackAddress()) {
				System.out.println("Starting a server on port "
						+ serverRecords.get(serverId).getPort());
				tcpSocket = new ServerSocket(serverRecords.get(serverId)
						.getPort());
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * addScheduledFailure
	 * 
	 * @param s
	 *            config string for failure to be scheduled
	 */
	public void addScheduledFailure(String s) {
		String[] split = s.split("\\s+");
		int k = Integer.parseInt(split[0]);
		int delta = Integer.parseInt(split[1]);
		scheduledFailures.add(new FailureRecord(k, delta));
	}

	/**
	 * addServerRecord - adds a record for a given configuration string
	 * 
	 * @param s
	 *            - configuration string for a server
	 */
	public void addServerRecord(String s) {
		serverRecords.add(new ServerRecord(s));
	}

	/**
	 * listen - blocking method that listens for TCP input
	 * 
	 */
	public void listen() {
		threadpool = Executors.newCachedThreadPool();
		Socket s;
		try {
			while ((s = tcpSocket.accept()) != null) {
				// when we get a connection, sping off a thread to handle it
				threadpool.submit(new TCPHandler(s, this));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * processRequest - method that updates bookMap based on a client's request.
	 * Synchronized to protect bookMap from multiple concurrent tinkerers.
	 * 
	 * @param request
	 *            String from client, format: <clientid> <bookid> <directive>
	 * @return String that holds response to client
	 */
	public String processRequest(String request) {
		// TODO - send message
		// TODO - add HEADER to formatted messages for both clients and servers
		// parse request into component parts
		String[] requestSplit = request.split("\\s+");
		String client = requestSplit[0].trim();
		String book = requestSplit[1].trim();
		String directive = requestSplit[2].trim();
		String status = bookMap.get(book);
		String prefix = "";

		// craft our response and update bookMap based on request
		if (status == null) {
			// book not listed - nothing to do except return a failing message
			prefix = FAIL;
		} else if (directive.equals(RETURN)) {
			// did the client have the book?
			if (!status.equals(client)) {
				// no, they didn't.
				prefix = FAIL;
			} else {
				// yes, they did. Make the book available and send a free
				// response.
				bookMap.put(book, AVAILABLE);
				prefix = FREE;
			}

		} else {
			// is the book available?
			if (!status.equals(AVAILABLE)) {
				// no, it's not.
				prefix = FAIL;
			} else {
				// yes, it is. Assign it to the client who reserved it.
				bookMap.put(book, client);
				// prefix already blank, which is correct
			}
		}

		// return the string in proper format
		return prefix + client + " " + book;
	}

	public static void main(String[] args) {
		// create server object
		Server s = new Server();

		// we'll listen to stdin.
		Scanner sc = new Scanner(System.in);

		// first line will be configuration
		s.parseConfig(sc.nextLine());

		// add records of all servers
		for (int i = 0; i < s.numServers; ++i) {
			s.addServerRecord(sc.nextLine());
		}

		// get records of scheduled crash
		while (sc.hasNext()) {
			s.addScheduledFailure(sc.nextLine());
		}
		s.currentScheduledFailure = s.scheduledFailures.remove(0);

		// start a server
		s.startServer();

		// after that, we'll poll for TCP communications
		s.listen();
	}
}
