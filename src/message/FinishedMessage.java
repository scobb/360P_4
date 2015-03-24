package message;

import java.io.PrintWriter;
import java.util.Scanner;

import record.ServerRecord;
import server.Server;

public class FinishedMessage extends Message {
	public FinishedMessage(Server from, ServerRecord to) {
		super(from, to);
		System.out.println("===========================");
		System.out.println("Creating a finishedMessage:");
		System.out.println("from: " + from.getAddr() + ":" + from.getPort());
		System.out.println("to: " + to.getAddr() + ":" + to.getPort());
		System.out.println("===========================");
	}

	@Override
	public void communicate(Scanner in, PrintWriter out) {
		out.println("SERVER F " + from.getClock() + " " + from.getServerId());
	}

	@Override
	public void handleTimeout() {
		// nothing to do
	}

}
