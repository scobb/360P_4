package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import record.ServerRecord;
import request.ClientRequest;
import request.Request;
import request.RequestFactory;
import request.SynchronizeRequest;
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
			BufferedReader in = new BufferedReader(new InputStreamReader(
					socket.getInputStream()));
			String msg = in.readLine();
			// determine if this message came from client or server
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
		// if from server, is it an ack, req, or finished?
		String[] splitMsg = msg.split("\\s+");
		String directive = splitMsg[1].trim();
		int clock = Integer.parseInt(splitMsg[2]);
		server.setClock(clock);

		// we'll only accept the synchronize message prior to recovering.
		if (!server.hasRecovered() && !directive.equals(Server.SYNCHRONIZE)) {
			return;
		}
		try {
			if (directive.equals(Server.REQUEST)) {
				// request -- send back an acknowledgement
				Request r = RequestFactory.decode(msg.split("%")[1], server);
				if (!server.getRequests().contains(r)) {
					server.getRequests().add(r);
				}
				PrintWriter out = new PrintWriter(socket.getOutputStream());
				out.println("OK");
				out.flush();
				server.serveIfReady();

			} else if (directive.equals(Server.RECOVER)) {
				ServerRecord sender = server.getServerRecords().get(
						Integer.parseInt(splitMsg[3]));
				PrintWriter out = new PrintWriter(socket.getOutputStream());
				out.println(server.getClock());
				out.flush();
				out.close();

			} else if (directive.equals(Server.SYNCHRONIZE)) {
				// get book data
				String[] msgData = msg.split(":");

				String bookDataString = msgData[1];

				// populate our book map from that data -- should be the same
				// from all other servers
				String[] bookData = bookDataString.split("_");
				for (int i = 0; i < bookData.length; ++i) {
					server.getBookMap().put("b" + (i + 1), bookData[i]);
				}

				if (msgData.length > 2) {
					// TODO - be friendly.. Need to test that this works with
					// multiple servers.
					String requestDataString = msgData[2];
					server.getRequests().clear();
					String[] requestData = requestDataString.split("_");
					for (int i = 0; i < requestData.length; ++i) {
						Request r = RequestFactory.decode(requestData[i], server);
						server.getRequests().add(r);
					}

				} 
				// increment the number of recoveries received to know whether
				// we're healthy enough to serve clients
				server.recoveryReceived();
				server.serveIfReady();
				

			} else {
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
		if (server.hasRecovered()) {
			ClientRequest cr = new ClientRequest(socket, server, null,
					server.getClock(), msg, server.getNumServers());
			server.getRequests().add(cr);

			// send requests to other servers
			server.broadcastMessage(new RequestMessage(server, cr, null));
		} 
		// Otherwise, we'll let the message time out and go to another server
	}
}
