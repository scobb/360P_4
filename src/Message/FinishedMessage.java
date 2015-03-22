package Message;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

import Record.ServerRecord;
import Server.Server;

public class FinishedMessage extends Message {
	public FinishedMessage(Server from, ServerRecord to){
		this.from = from;
		this.to = to;
	}
	@Override
	public void communicate(Scanner in, PrintWriter out) {
		out.println("SERVER F " + from.getClock());
	}

}
