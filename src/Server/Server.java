package server;

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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import record.ClientRecord;
import record.FailureRecord;
import record.ServerRecord;
import message.FinishedMessage;

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
	private ServerSocket tcpSocket;
	private ExecutorService threadpool;

	// permanent state - not lost on crash
	private boolean crashed;
	private List<ServerRecord> serverRecords;
	private List<FailureRecord> scheduledFailures;
	private FailureRecord currentScheduledFailure;

	// volatile state - lost on crash
	private Map<String, String> bookMap;
	private PriorityQueue<ClientRecord> clientRequests;

	// constants
	private final String RESERVE = "reserve";
	private final String RETURN = "return";
	private final String FAIL = "fail ";
	private final String AVAILABLE = "available";
	private final String FREE = "free ";
	public static final int TIMEOUT_MS = 100;

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
		crashed = false;
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

	public ServerSocket getTcpSocket() {
		return tcpSocket;
	}

	public void clientServed() {
		++numServed;
	}

	public FailureRecord getCurrentScheduledFailure() {
		return currentScheduledFailure;
	}

	/**
	 * fail - clears state and sleeps for requisite period
	 * 
	 * 
	 */
	public synchronized void fail() {
		// update state
		crashed = true;

		// clear state - client requests become empty.
		clientRequests.clear();

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
		// TODO - send message to other servers to send me a list of client
		// records, books
		// should receive ALL of these before resuming service (excluding
		// timeouts from crashed)

		// no longer crashed.
		crashed = false;
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
				// when we get a connection, spin off a thread to handle it if
				// we're online
				if (!crashed) {
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
		processRequest(getClientRequests().remove().getReqString());
	}

	/**
	 * serveIfReady - if next request on the Q is mine and it has been
	 * acknowledged, process it
	 * 
	 */
	public void serveIfReady() {
		if (getClientRequests().peek().isValid()
				&& getClientRequests().peek().getServer() == this) {
			// time to process this request
			ClientRecord req = getClientRequests().remove();
			String result = processRequest(req.getReqString());

			// send response to appropriate client
			PrintWriter out;
			try {
				out = new PrintWriter(req.getSocket().getOutputStream());
				out.println(result);
				out.flush();
				out.close();
				req.getSocket().close();
			} catch (IOException e) {
				e.printStackTrace();
			}

			// request processed. Send the finished message.
			for (ServerRecord sr : getServerRecords()) {
				if (!sr.equals(this)) {
					// send finished msg to each server that isn't me
					getThreadpool().submit(new FinishedMessage(this, sr));
				}
			}

			// update number of clients served
			clientServed();

			// is it time to fail?
			if (getCurrentScheduledFailure() != null
					&& getCurrentScheduledFailure().hasFailed(getNumServed())) {
				fail();
			}
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
			s.addServerRecord(sc.nextLine());
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
