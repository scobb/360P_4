package request;

import java.net.Socket;

import record.ServerRecord;
import server.Server;

public abstract class Request {
	protected int clock;
	protected int acksReceived;
	protected Server server;
	protected ServerRecord sr;
	
	public Request(Server server, ServerRecord sr, int clock) {
		this.server = server;
		this.sr = sr;
		this.clock = clock;
		acksReceived = 0;
	}
	public boolean isMine(){
		return server != null;
	}
	
	public Server getServer() {
		return server;
	}
	public ServerRecord getServerRecord() {
		return sr;
	}
	private int getId(){
		if (server != null){
			return server.getServerId();
		}
		return sr.getId();
	}
	public abstract void fulfill();
	public abstract void fulfillSilently();
	public int compareTo(Request other) {
		if (this.clock < other.clock
				|| (this.clock == other.clock && this.server.getServerId() < other.server.getServerId())) {
			return -1;
		}
		return 1;
	}

	public void ackReceived() {
		++acksReceived;
	}

	public boolean isValid() {
		return acksReceived >= this.server.getNumServers() - 1;
	}
}
