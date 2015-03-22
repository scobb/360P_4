package Message;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

import Record.ServerRecord;
import Server.Server;

public abstract class Message implements Runnable {
	protected Server from;
	protected ServerRecord to;

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

			// construct message
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
