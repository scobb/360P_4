import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.NoSuchElementException;
import java.util.Scanner;

public class Client {
	// constants
	private static final String TCP = "T";
	private static final String UDP = "U";
	private static final String SLEEP = "sleep";
	private static final int len = 1024;

	// members
	private String id;
	private InetAddress add;
	private byte[] rbuffer = new byte[len];

	// no-arg constructor
	public Client() {
		this.id = null;
		this.add = null;
	}

	/** parseConfig
	 * 
	 * @param conf String to be parsed into a command and handled
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

	/**processCommand
	 * 
	 * @param cmd command string to be executed
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
			String protocol = cmdSplit[3];
			
			String request = id + " " + book + " " + directive;
			// dependent on protocol, process in two different manners
			if (protocol.equals(UDP)) {
				processUdp(request, port);
			} else {
				processTcp(request, port);
			}
		}

	}

	/**
	 * processTcp
	 * @param send String request to send
	 * @param port Port server will be listening on
	 */
	public void processTcp(String send, int port) {
		Socket s = null;
		try {
			// talk to the server on the socket
			s = new Socket(this.add, port);
			
			// we'll communicate through streams: scanner and printwriter
			Scanner in = new Scanner(s.getInputStream());
			PrintWriter out = new PrintWriter(s.getOutputStream(), true);
			
			// send request
			out.println(send);
			
			// print response to stdout
			System.out.println(in.nextLine());
			
			// clean up -- not sure if these are redundant. Stream closes if any is called.
			s.close();
			in.close();
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	/**processUdp
	 * 
	 * @param send String we'll send
	 * @param port Integer port the server will be listening on
	 */
	public void processUdp(String send, int port) {
		DatagramPacket sPacket, rPacket;
		DatagramSocket datasocket;
		try {
			// declare new instance of socket
			datasocket = new DatagramSocket();
			
			// convert string to byte array
			byte[] buffer = send.getBytes();
			
			// build and send datagram packet
			sPacket = new DatagramPacket(buffer, buffer.length, add, port);
			datasocket.send(sPacket);
			
			// wait for response
			rPacket = new DatagramPacket(rbuffer, rbuffer.length);
			datasocket.receive(rPacket);
			
			// parse response into a string
			String retstring = new String(rPacket.getData(), 0,
					rPacket.getLength());
			
			// print string to stdout
			System.out.println(retstring);
		} catch (SocketException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		// build an empty client
		Client c = new Client();
		
		// we'll be taking commands from standard in
		Scanner sc = new Scanner(System.in);
		
		// configure using the first line of input
		c.parseConfig(sc.nextLine());
		
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
