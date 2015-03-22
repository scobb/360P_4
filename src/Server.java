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

/**
 * Server - Implements a concurrent UDP and TCP server.
 * 
 * @author scobb
 *
 */
public class Server {

	/**
	 * ServerRecord - inner class to keep track of other servers
	 * 
	 * @author scobb
	 *
	 */
	public class ServerRecord {
		private InetAddress addr;
		private int port;
		private int clock;

		public int getClock() {
			return clock;
		}

		public void setClock(int clock) {
			this.clock = clock;
		}

		public ServerRecord(String configStr) {
			String[] splitAddress = configStr.split(":");
			try {
				addr = InetAddress.getByName(splitAddress[0]);
			} catch (UnknownHostException e) {
				e.printStackTrace();
			}
			port = Integer.parseInt(splitAddress[1]);
			clock = 0;
		}
	}

	public class ClientRequest {
		private Socket s;
		private String reqString;
		int server;
		int clock;
		int acksReceived;

		public ClientRequest(Socket s, int server, int clock, String reqString) {
			this.s = s;
			this.reqString = reqString;
			this.server = server;
			this.clock = clock;
			acksReceived = 0;
		}

		public void ackReceived() {
			++acksReceived;
		}

		public boolean isValid() {
			return acksReceived >= Server.this.numServers;
		}

		public int compareTo(ClientRequest other) {
			if (this.clock < other.clock
					|| (this.clock == other.clock && this.server < other.server)) {
				return -1;
			}
			return 1;
		}

		public void fulfill() {

		}
	}

	public class ScheduledFailure {
		private int k;
		private int delta;

		public ScheduledFailure(int k, int delta) {
			this.k = k;
			this.delta = delta;
		}

		public boolean hasFailed(int numServed) {
			return numServed >= this.k;
		}
	}

	/**
	 * TCPHandler Runnable to handle incoming messages
	 * 
	 * @author scobb
	 *
	 */
	public class TCPHandler implements Runnable {
		// member vars
		Socket socket;

		// constructor: listener will give us a socket to work with.
		public TCPHandler(Socket s) {
			this.socket = s;
		}

		@Override
		public void run() {
			try {
				Scanner in = new Scanner(socket.getInputStream());
				String msg = in.nextLine();
				// determine if this message came from client or server
				if (msg.split(" ")[0] == "SERVER") {
					this.handleServerMessage(msg);
				} else {
					this.handleClientMessage(msg);
				}
				String resp = Server.this.processRequest(msg);
				socket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		private void handleServerMessage(String msg) {
			// if from server, is it an ack, req, or finished?
			String[] splitMsg = msg.split(" ");
			String directive = splitMsg[1];
			try {
				PrintWriter out = new PrintWriter(socket.getOutputStream());
				if (directive == "R") {
					// request -- send back an acknowledgement
					out.println("OK");
				} else {
					// other guy finished--am i at top of q, and have I received
					// all
					// acks?
				}
			} catch (IOException e) {
				e.printStackTrace();
			}

		}

		private void handleClientMessage(String msg) {
			ClientRequest cr = new ClientRequest(socket, Server.this.serverId,
					Server.this.clock, msg);
			Server.this.clientRequests.add(cr);

			// send requests to other servers - worth spinning off a thread?
			for (ServerRecord s : Server.this.serverRecords) {
				Server.this.threadpool
						.submit(new AcknowledgementRequest(cr, s));
			}
		}
	}

	public class AcknowledgementRequest implements Runnable {
		ClientRequest cr;
		ServerRecord sr;

		public AcknowledgementRequest(ClientRequest cr, ServerRecord sr) {
			this.cr = cr;
			this.sr = sr;
		}

		@Override
		public void run() {
			Socket s = null;
			try {
				// talk to the server on the socket
				s = new Socket(sr.addr, sr.port);

				// we'll communicate through streams: scanner and printwriter
				Scanner in = new Scanner(s.getInputStream());
				PrintWriter out = new PrintWriter(s.getOutputStream(), true);

				// construct message
				out.println("SERVER R");

				// wait for acknowledgement -- TODO, timeout here?
				in.nextLine();

				// clean up -- not sure if these are redundant. Stream closes if
				// any is called.
				s.close();
				in.close();
				out.close();
			} catch (IOException e) {
				e.printStackTrace();
			}

		}
	}

	// member variables
	private Map<String, String> bookMap;
	private ServerSocket tcpSocket;
	private int numServers;
	private int clock;
	private int serverId;
	private int numServed;
	private List<ServerRecord> serverRecords;
	private List<ScheduledFailure> scheduledFailures;
	private PriorityQueue<ClientRequest> clientRequests;
	private ScheduledFailure currentScheduledFailure;
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
		scheduledFailures = new ArrayList<ScheduledFailure>();
		clientRequests = new PriorityQueue<ClientRequest>();
		currentScheduledFailure = null;
		numServed = 0;
		clock = 0;
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
			InetAddress addr = serverRecords.get(serverId).addr;
			if (addr.isAnyLocalAddress() || addr.isLoopbackAddress()) {
				System.out.println("Starting a server on port "
						+ serverRecords.get(serverId).port);
				tcpSocket = new ServerSocket(serverRecords.get(serverId).port);
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
		scheduledFailures.add(new ScheduledFailure(k, delta));
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
				threadpool.submit(new TCPHandler(s));
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
		// TODO - remove synchronize
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
