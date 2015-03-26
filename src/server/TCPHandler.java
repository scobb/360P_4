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

		// we'll only accept the synchronize message prior to recovering.
		if (!server.hasRecovered() && !directive.equals(Server.SYNCHRONIZE)
				&& !directive.equals(Server.SYNCHRONIZE)) {
			return;
		}
		try {
			if (directive.equals(Server.REQUEST)) {
				// request -- send back an acknowledgement
				System.out.println("It was a request.");
				Request r = RequestFactory.decode(msg.split("%")[1]);
				if (!server.getRequests().contains(r)) {
					server.getRequests().add(r);
				}
				PrintWriter out = new PrintWriter(socket.getOutputStream());
				out.println("OK");
				out.flush();

			} else if (directive.equals(Server.RECOVER)) {
				System.out.println("It was a recover.");
				ServerRecord sender = server.getServerRecords().get(
						Integer.parseInt(splitMsg[3]) - 1);

				// schedule synchronize request
				SynchronizeRequest sr = new SynchronizeRequest(server, sender,
						clock, server.getNumServers());
				server.getRequests().add(sr);
				server.broadcastMessage(new RequestMessage(server, sr, null));

			} else if (directive.equals(Server.SYNCHRONIZE)) {
				// get book data
				String[] msgData = msg.split(":");

				String bookDataString = msgData[1];
				System.out.println("Got book data: " + bookDataString);

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
					System.out.println("Got requestData: " + requestDataString);
					System.out.println("Before updating requests: "
							+ server.getRequests());
					String[] requestData = requestDataString.split("_");
					for (int i = 0; i < requestData.length; ++i) {
						Request r = RequestFactory.decode(requestData[i]);
						if (!server.getRequests().contains(r)) {
							server.getRequests().add(r);
						}
					}
					System.out.println("Done updating requests: "
							+ server.getRequests());
					// System.out.println("server.getRequests().size(): " +
					// server.getRequests().size());

				} else {
					System.out.println("No client data.");
				}

				// increment the number of recoveries received to know whether
				// we're healthy enough to serve clients
				server.recoveryReceived();
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
		if (server.hasRecovered()) {
			System.out.println("Server is healthy!");
			ClientRequest cr = new ClientRequest(socket, server, null,
					server.getClock(), msg, server.getNumServers());
			server.getRequests().add(cr);

			// send requests to other servers
			server.broadcastMessage(new RequestMessage(server, cr, null));
		}
		// Otherwise, we'll let the message time out and go to another server
	}
}
