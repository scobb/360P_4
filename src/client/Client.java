package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;

import record.ServerRecord;
import server.Server;

public class Client {
	// constants
	private static final String SLEEP = "sleep";

	// members
	private String id;
	private InetAddress add;
	private List<ServerRecord> servers;
	private int numServers;

	// no-arg constructor
	public Client() {
		this.id = null;
		this.add = null;
		this.servers = new ArrayList<ServerRecord>();
	}

	/**
	 * parseConfig
	 * 
	 * @param conf
	 *            String to be parsed into a command and handled
	 */
	public void parseConfig(String conf) {
		// split based on whitespace
		String[] confSplit = conf.split("\\s+");

		// add c to the client number to be able to easily construct requests
		this.id = confSplit[0];
		this.numServers = Integer.parseInt(confSplit[1].trim());

	}

	/**
	 * processCommand
	 * 
	 * @param cmd
	 *            command string to be executed
	 */
	public void processCommand(String cmd) {
		// split based on whitespace
		String[] cmdSplit = cmd.split("\\s+");

		// did we receive a sleep instruction?
		if (cmdSplit[0].equals(SLEEP)) {
			// if so, put the thread to sleep for the requested time
			//System.out.println("Sleeping...");
			try {
				Thread.sleep(Integer.parseInt(cmdSplit[1]));
			} catch (NumberFormatException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		} else {
			// any other command will have the same basic format
			String book = cmdSplit[0];
			String directive = cmdSplit[1];

			//System.out.println("Book: " + book + " Directive: " + directive);
			String request = id + " " + book + " " + directive;

			for (ServerRecord sr : servers) {
				// process request
				try {
					// try to process
					processTcp(request, sr.getPort());

					// if we get here, we can break the loop
					break;
				} catch (IOException e) {
//					System.out
//							.println("Server connection failed... proceeding to next.");
					// try the next server
					continue;
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

	}

	/**
	 * processTcp
	 * 
	 * @param send
	 *            String request to send
	 * @param port
	 *            Port server will be listening on
	 */
	public void processTcp(String send, int port) throws IOException {
		//System.out.println("In processTcp");
		Socket s = null;
		// talk to the server on the socket
		s = new Socket();
		s.connect(new InetSocketAddress(this.add, port), Server.TIMEOUT_MS);

		// we'll communicate through streams: scanner and printwriter
		BufferedReader in = new BufferedReader(new InputStreamReader(
				s.getInputStream()));
		PrintWriter out = new PrintWriter(s.getOutputStream(), true);

		//System.out.println("Sending request.");

		// routing msg
		out.println("CLIENT");

		// send request
		out.println(send);
		//System.out.println("Printing Response.");
		s.setSoTimeout(Server.TIMEOUT_MS);
		// print response to stdout
		String next = in.readLine();
		while (next.trim().equals("WAIT")) {
			// adding a bit of buffer for possible server timeouts.
			//System.out.println("Waiting...");
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			next = in.readLine();
		}
		System.out.println(next);
		//System.out.println("Printed.");

		// clean up -- not sure if these are redundant. Stream closes if any
		// is called.
		s.close();
		in.close();
		out.close();
	}

	public static void main(String[] args) {
		// build an empty client
		Client c = new Client();

		// we'll be taking commands from standard in
		Scanner sc = new Scanner(System.in);

		// configure using the first line of input
		c.parseConfig(sc.nextLine());

		// add server records
		for (int i = 0; i < c.numServers; ++i) {
			c.servers.add(new ServerRecord(sc.nextLine(), i));
		}

		System.out.println("Client initialized.");

		// go until user enters an empty line.
		while (true) {
			try {
				c.processCommand(sc.nextLine());
			} catch (NoSuchElementException e) {
				break;
			}
		}
	}
}
