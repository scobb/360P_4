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
		System.out.println("pinging the request.");
		cr.ping();
	}

	@Override
	public void communicate(BufferedReader in, PrintWriter out) throws IOException, IOException {
		System.out.println("Communicating...");
		System.out.println("Message: SERVER R " + from.getClock() + " " + from.getServerId() + " :" + cr.getMsg());
		// let the client know we haven't forgotten them.
		// construct message
		out.println("SERVER");
		out.println("SERVER R " + from.getClock() + " " + from.getServerId() + " :" + cr.getMsg());

		
		// wait for acknowledgement
		in.readLine();
		
		System.out.println("Got the acknowledgement");

		// add acknowledgement
		ackReceived();
		
		// check if it's our turn.
		this.from.serveIfReady();
		
	}

	@Override
	public void handleTimeout() {
		// mark this server as offline until we get a RecoveryMessage from them
		System.out.println("Request timed out... marking server offline.");
		to.setOnline(false);
		
		// TODO - remove all CRs that belong to that server.
		
		// if that server has crashed, we can count them as acknowledging.
		cr.ackReceived();
	}

}
