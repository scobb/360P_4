package message;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Scanner;

import record.ServerRecord;
import server.Server;

public class FinishedMessage extends Message {
	public FinishedMessage(Server from, ServerRecord to) {
		super(from, to);
	}
	public void ping(){}

	@Override
	public void communicate(BufferedReader in, PrintWriter out) throws IOException {
		out.println("SERVER");
		out.println("SERVER F " + from.getClock() + " " + from.getId());
	}

	@Override
	public void handleTimeout() {
		// nothing to do
	}

}
