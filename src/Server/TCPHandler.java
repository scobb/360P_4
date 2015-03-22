package server;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

import record.ClientRecord;
import record.ServerRecord;
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
			Scanner in = new Scanner(socket.getInputStream());
			String msg = in.nextLine();
			// determine if this message came from client or server
			if (msg.split(" ")[0] == "SERVER") {
				this.handleServerMessage(msg);
			} else {
				this.handleClientMessage(msg);
			}
			String resp = server.processRequest(msg);
			socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void handleServerMessage(String msg) {
		// if from server, is it an ack, req, or finished?
		String[] splitMsg = msg.split(" ");
		String directive = splitMsg[1];
		try {
			if (directive == "R") {
				// request -- send back an acknowledgement
				PrintWriter out = new PrintWriter(socket.getOutputStream());
				
				out.println("OK");
			} else {
				// remote server finished serving---update database
				server.updateFromRemoteComplete();
				
				// if it's my turn, serve now
				server.serveIfReady();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private void handleClientMessage(String msg) {
		ClientRecord cr = new ClientRecord(socket, server,
				server.getClock(), msg);
		server.getClientRequests().add(cr);

		// send requests to other servers - worth spinning off a thread?
		for (ServerRecord s : server.getServerRecords()) {
			server.getThreadpool()
					.submit(new RequestMessage(server, cr, s));
		}
	}
}
