package message;

import java.io.PrintWriter;
import java.util.Scanner;

import record.ServerRecord;
import server.Server;

public class FinishedMessage extends Message {
	public FinishedMessage(Server from, ServerRecord to) {
		super(from, to);
	}

	@Override
	public void communicate(Scanner in, PrintWriter out) {
		out.println("SERVER F " + from.getClock());
	}

	@Override
	public void handleTimeout() {
		// nothing to do
	}

}
