package message;
import java.io.PrintWriter;
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

	@Override
	public void handleTimeout() {
		// if that server has crashed, we can count them as acknowledging.
		cr.ackReceived();
	}

}
