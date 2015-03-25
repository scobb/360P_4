package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.util.Scanner;

import record.ServerRecord;
import request.ClientRequest;
import request.RecoveryRequest;
import message.FinishedMessage;
import message.RequestMessage;

public class TCPHandler implements Runnable {
	// member vars
	Socket socket;
	Server server;

	// constructor: listener will give us a socket to work with.
	public TCPHandler(Socket s, Server server) {
		this.socket = s;
		this.server = server;
	}

	@Override
	public void run() {
		try {
			BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			String msg = in.readLine();
			// determine if this message came from client or server
			System.out.println(msg.split("\\s+")[0]);
			if (msg.split("\\s+")[0].trim().equals("SERVER")) {
				this.handleServerMessage(msg);
				socket.close();
			} else {
				this.handleClientMessage(msg);
			}
			// String resp = server.processRequest(msg);
			// socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void handleServerMessage(String msg) {
		System.out.println("Handling a server message: " + msg);
		// if from server, is it an ack, req, or finished?
		String[] splitMsg = msg.split("\\s+");
		String directive = splitMsg[1].trim();
		System.out.println("Directive: " + directive);
		int clock = Integer.parseInt(splitMsg[2]);
		server.setClock(clock + 1);
		try {
			if (directive.equals(Server.REQUEST)) {
				// request -- send back an acknowledgement
				System.out.println("It was a request.");

				ServerRecord sender = server.getServerRecords().get(Integer.parseInt(splitMsg[3]));
				server.getRequests().add(new ClientRequest(null, null, sender, clock, msg.split(":")[1], server.getNumServers()));
				PrintWriter out = new PrintWriter(socket.getOutputStream());
				out.println("OK");
				out.flush();

			} else if (directive.equals(Server.RECOVER)) {
				System.out.println("It was a recover.");
				ServerRecord sender = server.getServerRecords().get(Integer.parseInt(splitMsg[3]));
				
				// sender is back online
				sender.setOnline(true);
				
				// schedule recover response - TODO - this clock is invalid, will that break things?
				RecoveryRequest rr = new RecoveryRequest(null, sender, clock, server.getNumServers());
				server.getRequests().add(rr);

				server.broadcastMessage(new RequestMessage(server, rr, null));

			} else {
				System.out.println("It was something else.");
				// remote server finished serving---update database to stay in
				// line
				server.updateFromRemoteComplete();

				// if it's my turn, serve now
				server.serveIfReady();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public void handleClientMessage(String msg) {
		System.out.println("Handling client msg: " + msg);
		if (server.hasRecovered()){
			System.out.println("Server is healthy!");
			ClientRequest cr = new ClientRequest(socket, server, null, server.getClock(), msg, server.getNumServers());
			server.getRequests().add(cr);
	
			// send requests to other servers
			server.broadcastMessage(new RequestMessage(server, cr, null));
		} else {
			(new ClientRequest(socket, server, null, server.getClock(), msg, server.getNumServers())).fail();
			System.out.println("Server is broken :(");
			// clock not valid yet. Should not broadcast.
			server.scheduleClientRequest(new ClientRequest(socket, server, null, server.getClock(), msg, server.getNumServers()));
			
		}
	}
}
