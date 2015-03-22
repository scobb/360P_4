package Server;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

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
				PrintWriter out = new PrintWriter(socket.getOutputStream());
				// request -- send back an acknowledgement
				out.println("OK");
			} else {
				// other guy finished---update database
				server.processRequest(server.getClientRequests().remove().getReqString());
				
				// am i next?
				if (server.getClientRequests().peek().isValid()
						&& server.getClientRequests().peek().getServer() == server) {
					// time to process this request
					ClientRequest req = server.getClientRequests().remove();
					String result = server.processRequest(req.getReqString());
					
					// send response to appropriate client
					PrintWriter out = new PrintWriter(req.getSocket().getOutputStream());
					out.println(result);
					out.flush();
					out.close();
					req.getSocket().close();
					
					// request processed. Send the finished message.
					for (ServerRecord sr: server.getServerRecords()){
						if (!sr.equals(server)) {
							// send finished msg
							server.getThreadpool().submit(new FinishedMessage(sr));
						}
					}
					server.clientServed();
					if (server.getCurrentScheduledFailure() != null && server.getCurrentScheduledFailure().hasFailed(server.getNumServed())){
						server.fail();
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private void handleClientMessage(String msg) {
		ClientRequest cr = new ClientRequest(socket, server,
				server.getClock(), msg);
		server.getClientRequests().add(cr);

		// send requests to other servers - worth spinning off a thread?
		for (ServerRecord s : server.getServerRecords()) {
			server.getThreadpool()
					.submit(new AcknowledgementRequest(cr, s));
		}
	}
}
