package Message;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

import Record.ClientRecord;
import Record.ServerRecord;
import Server.Server;


public class RequestMessage extends Message{
	ClientRecord cr;

	public RequestMessage(Server from, ClientRecord cr, ServerRecord to) {
		this.from = from;
		this.cr = cr;
		this.to = to;
	}

	@Override
	public void communicate(Scanner in, PrintWriter out) {// construct message
		out.println("SERVER R " + from.getClock());

		// wait for acknowledgement -- TODO, timeout here?
		in.nextLine();

		// add acknowledgement
		cr.ackReceived();
		
	}

}