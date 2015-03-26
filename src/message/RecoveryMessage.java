package message;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Scanner;

import record.ServerRecord;
import server.Server;

// FROM recently-recovered server TO healthy server
public class RecoveryMessage extends Message{

	public RecoveryMessage(Server from, ServerRecord to) {
		super(from, to);
	}
	
	public void ping(){}

	@Override
	public void communicate(BufferedReader in, PrintWriter out) throws IOException {
		// send recover request
		out.println(Server.SERVER);
		out.println(Server.SERVER + " " + Server.RECOVER + " " + from.getClock() + " " + from.getId());
		out.flush();
		
		// get new clock from server
		from.setClock(Integer.parseInt(in.readLine()));
		
	}

	@Override
	public void handleTimeout() {
		
	}

}
