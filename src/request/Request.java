package request;

import java.net.Socket;

import record.ServerRecord;
import server.Server;
/**
 * Request - defines the interface for our events to be completed
 *
 */
public abstract class Request implements Comparable<Request> {
	protected int clock;
	protected int acksReceived;
	protected int numServers;
	protected Server server;
	protected ServerRecord sr;

	public void ping() {
	}

	public Request(Server server, ServerRecord sr, int clock, int numServers) {
		this.server = server;
		this.sr = sr;
		this.clock = clock;
		this.numServers = numServers;
		acksReceived = 0;
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof Request) {
			Request otherR = (Request) other;
			return this.clock == otherR.clock && this.getId() == otherR.getId();
		}
		return super.equals(other);
	}

	public boolean isMine() {
		return server != null;
	}

	public void setClock(int clock) {
		this.clock = clock;
	}

	public Server getServer() {
		return server;
	}

	public ServerRecord getServerRecord() {
		return sr;
	}

	private int getId() {
		if (server != null) {
			return server.getId();
		}
		return sr.getId();
	}

	public String getMsg() {
		return " ";
	}

	public abstract void fulfill();

	public abstract void fulfillSilently(Server server);

	public int compareTo(Request other) {
		if (this.clock < other.clock
				|| (this.clock == other.clock && getId() < other.getId())) {
			return -1;
		}
		return 1;
	}

	public void ackReceived() {
		++acksReceived;
	}

	public boolean isValid() {
		return acksReceived >= numServers - 1;
	}

	public int getClock() {
		return clock;
	}

	abstract public String encode();
}
