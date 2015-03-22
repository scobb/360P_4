package message;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

import record.ServerRecord;
import server.Server;

public abstract class Message implements Runnable {
	protected Server from;
	protected ServerRecord to;
	
	public Message(Server from, ServerRecord to){
		this.from = from;
		this.to = to;
	}

	public abstract void communicate(Scanner in, PrintWriter out);

	@Override
	public void run() {
		Socket s = null;
		try {
			// talk to the server on the socket
			s = new Socket(to.getAddr(), to.getPort());

			// we'll communicate through streams: scanner and printwriter
			Scanner in = new Scanner(s.getInputStream());
			PrintWriter out = new PrintWriter(s.getOutputStream(), true);

			// implemenetation-specific communication
			communicate(in, out);
			
			// clean up
			s.close();
			in.close();
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
