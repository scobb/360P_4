package message;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;

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
	
	abstract public void ping();

	public abstract void communicate(BufferedReader in, PrintWriter out) throws IOException;

	public abstract void handleTimeout();

	public void send() {
		Socket s = null;
		try {
			// talk to the server on the socket
			ping();
			s = new Socket();
			s.connect(new InetSocketAddress(to.getAddr(), to.getPort()),
					Server.TIMEOUT_MS);
			s.setSoTimeout(Server.TIMEOUT_MS);
			// we'll communicate through streams: scanner and printwriter
			BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
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
