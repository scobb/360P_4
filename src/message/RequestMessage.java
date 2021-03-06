package message;
import java.io.BufferedReader;
import java.io.IOException;
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
	
	public void ackReceived(){
		cr.ackReceived();
	}
	public void ping(){
		cr.ping();
	}

	@Override
	public void communicate(BufferedReader in, PrintWriter out) throws IOException, IOException {
		// let the client know we haven't forgotten them.
		// construct message
		out.println("SERVER");
		out.println("SERVER R " + from.getClock() + " %" + cr.encode());

		
		// wait for acknowledgement
		in.readLine();

		// add acknowledgement
		ackReceived();
		
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
