package message;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Scanner;

import record.ServerRecord;
import server.Server;

public abstract class Message {
	protected Server from;
	protected ServerRecord to;

	public void ackReceived(){
		
	}

	public Message(Server from, ServerRecord to) {
		this.from = from;
		this.to = to;
	}
	
	public void setTo(ServerRecord to){
		this.to = to;
	}

	public abstract void communicate(Scanner in, PrintWriter out);

	public abstract void handleTimeout();

	public void send() {
		Socket s = null;
		try {
			// talk to the server on the socket
			s = new Socket();
			s.connect(new InetSocketAddress(to.getAddr(), to.getPort()),
					Server.TIMEOUT_MS);
			// we'll communicate through streams: scanner and printwriter
			Scanner in = new Scanner(s.getInputStream());
			PrintWriter out = new PrintWriter(s.getOutputStream(), true);

			// implementation-specific communication
			communicate(in, out);

			// clean up
			s.close();
			in.close();
			out.close();
		} catch (SocketTimeoutException e) {
			// connection timed out.
			handleTimeout();
		} catch (IOException e) {
			// something else went wrong.
			e.printStackTrace();
		}
	}

}
