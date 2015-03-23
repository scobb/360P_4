package message;
import java.io.PrintWriter;
import java.util.Scanner;

import record.ServerRecord;
import request.ClientRequest;
import request.Request;
import server.Server;


public class RequestMessage extends Message{
	Request cr;

	public RequestMessage(Server from, Request cr, ServerRecord to) {
		super(from, to);
		this.cr = cr;
	}

	@Override
	public void communicate(Scanner in, PrintWriter out) {
		// construct message
		out.println("SERVER R " + from.getClock());

		// wait for acknowledgement -- TODO, timeout here?
		in.nextLine();

		// add acknowledgement
		cr.ackReceived();
		
		// check if it's our turn.
		this.from.serveIfReady();
		
	}

	@Override
	public void handleTimeout() {
		// mark this server as offline until we get a RecoveryMessage from them
		to.setOnline(false);
		
		// if that server has crashed, we can count them as acknowledging.
		cr.ackReceived();
	}

}
