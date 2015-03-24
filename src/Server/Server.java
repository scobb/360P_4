package server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import message.FinishedMessage;
import message.Message;
import message.RequestMessage;
import record.FailureRecord;
import record.ServerRecord;
import request.ClientRequest;
import request.RecoveryRequest;
import request.Request;

/**
 * Server - Implements a distributed TCP server.
 * 
 * @author scobb
 *
 */
public class Server {

	// member variables
	private int numServers;
	private int clock;
	private int serverId;
	private int numServed;
	private int numRecoveriesReceived;
	private ServerSocket tcpSocket;
	private ExecutorService threadpool;
	private InetAddress addr;
	private int port;
	
	// permanent state - not lost on crash
	private boolean crashed;
	private List<ServerRecord> serverRecords;
	private List<FailureRecord> scheduledFailures;
	private FailureRecord currentScheduledFailure;

	// volatile state - lost on crash
	private Map<String, String> bookMap;
	private PriorityQueue<Request> requests;
	private List<ClientRequest> scheduledClientRequests;

	// constants
	private final String RESERVE = "reserve";
	private final String RETURN = "return";
	private final String FAIL = "fail ";
	private final String AVAILABLE = "available";
	private final String FREE = "free ";
	public static final String SERVER = "SERVER";
	public static final String CLIENT = "CLIENT";
	public static final String REQUEST = "R";
	public static final String FINISHED = "F";
	public static final String RECOVER = "V";
	public static final int TIMEOUT_MS = 100;

	// empty constructor - instantiate the map that will track who has what
	// books
	public Server() {
		bookMap = new HashMap<String, String>();
		serverRecords = new ArrayList<ServerRecord>();
		scheduledFailures = new ArrayList<FailureRecord>();
		requests = new PriorityQueue<Request>();
		scheduledClientRequests = new ArrayList<ClientRequest>();
		currentScheduledFailure = null;
		numServed = 0;
		clock = 0;
		crashed = false;
		numRecoveriesReceived = 0;
	}

	// incrementors
	public void recoveryReceived() {
		++numRecoveriesReceived;
	}

	public void clientServed() {
		++numServed;
	}

	// getters
	public boolean hasRecovered() {
		if (crashed && numRecoveriesReceived >= numServers - 1) {
			crashed = false;
			numRecoveriesReceived = 0;
		}
		return !crashed;
	}
	
	public int getPort(){
		return port;
	}
	
	public InetAddress getAddr(){
		return addr;
	}

	public int getNumServed() {
		return numServed;
	}

	public ExecutorService getThreadpool() {
		return threadpool;
	}

	public int getClock() {
		return clock;
	}

	public void setClock(int clock) {
		this.clock = Math.max(this.clock, clock + 1);
	}

	public PriorityQueue<Request> getRequests() {
		return requests;
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

	public ServerSocket getTcpSocket() {
		return tcpSocket;
	}

	public FailureRecord getCurrentScheduledFailure() {
		return currentScheduledFailure;
	}

	public void scheduleClientRequest(ClientRequest cr) {
		scheduledClientRequests.add(cr);
	}

	public void broadcastMessage(Message m) {
		System.out.println("Broadcasting...");
		for (ServerRecord s : serverRecords) {
			if (!s.equals(this) && s.isOnline()) {
				System.out.println("Broadcasting to " + s);
				m.setTo(s);
				threadpool.submit(m);
			} else if (!s.isOnline()) {
				m.ackReceived();
			}
		}
	}

	public void broadcastScheduledRequests() {
		for (ClientRequest cr : scheduledClientRequests) {
			// update the request to have a valid clock.
			cr.setClock(clock++);

			// add to local queue
			requests.add(cr);

			// send request to other servers
			broadcastMessage(new RequestMessage(this, cr, null));
		}
	}

	/**
	 * fail - clears state and sleeps for requisite period
	 * 
	 * 
	 */
	public synchronized void fail() {
		System.out.println("FAILING.");
		// update state
		crashed = true;

		// reset state
		numServed = 0;
		numRecoveriesReceived = 0;

		// clear state - requests become empty.
		requests.clear();
		scheduledClientRequests.clear();

		// also, make all books available again
		for (String bookKey : bookMap.keySet()) {
			bookMap.put(bookKey, AVAILABLE);
		}

		// sleep
		try {
			Thread.sleep(currentScheduledFailure.getDelta());
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		// update failure list
		updateCurrentScheduledFailure();

		// schedule a recoveryRecord
		requests.add(new RecoveryRequest(this, null, this.clock));
	}

	public void updateCurrentScheduledFailure() {
		if (scheduledFailures.size() > 0) {
			currentScheduledFailure = scheduledFailures.remove(0);
		} else {
			currentScheduledFailure = null;
		}
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
			InetAddress addr = serverRecords.get(serverId - 1).getAddr();
			if (addr.isAnyLocalAddress() || addr.isLoopbackAddress()) {
				System.out.println("Starting a server on port "
						+ serverRecords.get(serverId - 1).getPort());
				tcpSocket = new ServerSocket(serverRecords.get(serverId - 1)
						.getPort());
				this.addr = addr;
				this.port = serverRecords.get(serverId - 1).getPort();
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
		int k = Integer.parseInt(split[1]);
		int delta = Integer.parseInt(split[2]);
		scheduledFailures.add(new FailureRecord(k, delta));
	}

	/**
	 * addServerRecord - adds a record for a given configuration string
	 * 
	 * @param s
	 *            - configuration string for a server
	 */
	public void addServerRecord(String s, int id) {
		serverRecords.add(new ServerRecord(s, id));
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
				// when we get a connection, spin off a thread to handle it if
				// we're online
				if (!crashed) {
					++clock;
					threadpool.submit(new TCPHandler(s, this));
				}
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
	public synchronized String processRequest(String request) {
		System.out.println("Processing request: " + request);
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

	/**
	 * updateFromRemoteComplete - to be called when a remote server has
	 * completed a transaction
	 * 
	 */
	public void updateFromRemoteComplete() {
		// remove next from queue and process, but don't output
		requests.remove().fulfillSilently();
	}

	/**
	 * serveIfReady - if next request on the Q is mine and it has been
	 * acknowledged, process it
	 * 
	 */
	public void serveIfReady() {
		System.out.println("Serving if ready...");
		// while loop means we can handle multiple in a row if we're up.
		while (getRequests().peek().isValid() && getRequests().peek().isMine()) {
			// time to process this request
			Request req = requests.remove();
			System.out.println("Got a valid request. Fulfilling.");

			// fulfill the request
			req.fulfill();

			// broadcast that we're done.
			broadcastMessage(new FinishedMessage(this, null));
		}
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
			s.addServerRecord(sc.nextLine(), i);
		}

		// get records of scheduled crash
		while (sc.hasNext()) {
			s.addScheduledFailure(sc.nextLine());
		}
		s.updateCurrentScheduledFailure();

		// start a server
		s.startServer();

		// after that, we'll poll for TCP communications
		s.listen();
	}
}
