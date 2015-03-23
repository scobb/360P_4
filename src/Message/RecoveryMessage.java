package message;

import java.io.PrintWriter;
import java.util.Scanner;

import record.ServerRecord;
import server.Server;

// FROM healthy server TO recently-recovered server
public class RecoveryMessage extends Message{

	public RecoveryMessage(Server from, ServerRecord to) {
		super(from, to);
	}

	@Override
	public void communicate(Scanner in, PrintWriter out) {
		// send recover request
		out.println(Server.SERVER + " " + Server.RECOVER);
		// TODO accept info on books
		
		// TODO accept info on clients 
		
	}

	@Override
	public void handleTimeout() {
		// TODO increment "from"'s heard from?
		
	}

}
