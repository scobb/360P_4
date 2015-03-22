package Server;

import java.net.Socket;

public class ClientRequest {
	private Socket s;
	private String reqString;
	private Server server;
	int clock;
	int acksReceived;

	public ClientRequest(Socket s, Server server, int clock,
			String reqString) {
		this.s = s;
		this.reqString = reqString;
		this.server = server;
		this.clock = clock;
		acksReceived = 0;
	}

	public String getReqString() {
		return reqString;
	}
	
	public Socket getSocket(){
		return s;
	}

	public Server getServer() {
		return server;
	}

	public void ackReceived() {
		++acksReceived;
	}

	public boolean isValid() {
		return acksReceived >= this.server.getNumServers();
	}

	public int compareTo(ClientRequest other) {
		if (this.clock < other.clock
				|| (this.clock == other.clock && this.server.getServerId() < other.server.getServerId())) {
			return -1;
		}
		return 1;
	}
}