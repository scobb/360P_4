package message;

import java.io.PrintWriter;
import java.util.Scanner;

import record.ServerRecord;
import server.Server;

// FROM recently-recovered server TO healthy server
public class RecoveryMessage extends Message{

	public RecoveryMessage(Server from, ServerRecord to) {
		super(from, to);
	}

	@Override
	public void communicate(Scanner in, PrintWriter out) {
		// send recover request
		out.println(Server.SERVER + " " + Server.RECOVER);
		out.flush();
		
	}

	@Override
	public void handleTimeout() {
		// TODO increment "from"'s heard from?
		
	}

}
