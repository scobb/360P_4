package message;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

import record.ClientRecord;
import record.ServerRecord;
import server.Server;


public class RequestMessage extends Message{
	ClientRecord cr;

	public RequestMessage(Server from, ClientRecord cr, ServerRecord to) {
		super(from, to);
		this.cr = cr;
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
