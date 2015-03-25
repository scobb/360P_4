package message;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;

import record.ServerRecord;
import request.Request;
import request.SynchronizeRequest;
import server.Server;

// message FROM healthy server TO recovering server
public class SynchronizeMessage extends Message {

	public SynchronizeMessage(Server from, ServerRecord to) {
		super(from, to);
	}

	@Override
	public void ping() {
	}

	@Override
	public void communicate(BufferedReader in, PrintWriter out)
			throws IOException {
		// server route
		out.println(Server.SERVER);
		
		// encode book data
		String bookStr = "";
		String prefix = "";
		System.out.println("----------");
		for (int i = 0; i < from.getBookMap().size(); ++i){
			bookStr += (prefix + from.getBookMap().get("b" + (i + 1)));
			prefix = "_";
		}
		
		// encode request data - add the request that spawned this message first thing.
		String requestStr = (new SynchronizeRequest(from, to, 0, 0)).encode();
		prefix = "_";
		for (Request r : from.getRequests()){
			requestStr += (prefix + r.encode());
		}

		// send encoded recovery with books and clients.
		out.println(Server.SERVER + " " + Server.SYNCHRONIZE + " "
				+ from.getClock() + " " + from.getServerId() + " :" + bookStr + ":" + requestStr);
		System.out.println("Finished with teh Synch request....");

	}

	@Override
	public void handleTimeout() {
	}

}
