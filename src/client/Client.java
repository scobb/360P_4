package client;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.NoSuchElementException;
import java.util.Scanner;

import server.Server;

public class Client {
	// constants
	private static final String SLEEP = "sleep";

	// members
	private String id;
	private InetAddress add;

	// no-arg constructor
	public Client() {
		this.id = null;
		this.add = null;
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
		this.id = "c" + confSplit[0];

		// build address using ip submitted
		try {
			this.add = InetAddress.getByName(confSplit[1]);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
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
			int port = Integer.parseInt(cmdSplit[2]);

			String request = id + " " + book + " " + directive;
			// process request
			processTcp(request, port);
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
	public void processTcp(String send, int port) {
		Socket s = null;
		try {
			// talk to the server on the socket
			s = new Socket();
			s.connect(new InetSocketAddress(this.add, port), Server.TIMEOUT_MS);

			// we'll communicate through streams: scanner and printwriter
			Scanner in = new Scanner(s.getInputStream());
			PrintWriter out = new PrintWriter(s.getOutputStream(), true);

			// send request
			out.println(send);

			// print response to stdout
			System.out.println(in.nextLine());

			// clean up -- not sure if these are redundant. Stream closes if any
			// is called.
			s.close();
			in.close();
			out.close();
		} catch (SocketTimeoutException e) {
			handleTimeout();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void handleTimeout() {
		// TODO - go to next server on the list
	}

	public static void main(String[] args) {
		// build an empty client
		Client c = new Client();

		// we'll be taking commands from standard in
		Scanner sc = new Scanner(System.in);

		// configure using the first line of input
		c.parseConfig(sc.nextLine());
		
		// TODO - add server records

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
