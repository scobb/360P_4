package message;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;

import record.ServerRecord;
import server.Server;

// message FROM healthy server TO recovering server
public class SynchronizeMessage extends Message{

	public SynchronizeMessage(Server from, ServerRecord to) {
		super(from, to);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void ping() {}

	@Override
	public void communicate(BufferedReader in, PrintWriter out)
			throws IOException {
		// server route
		out.println(Server.SERVER);
		
		// send encoded header
		out.println(Server.SERVER + " " + Server.SYNCHRONIZE + " " + from.getClock() + " " + from.getServerId());
		
		// TODO - send encoded books
		out.println("omg books");
		
		// TODO - send encoded client list
		out.println("omg clientz");
		
	}

	@Override
	public void handleTimeout() {}

}
