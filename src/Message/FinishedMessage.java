package message;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
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

}
