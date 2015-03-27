package request;

import java.net.Socket;

import record.ServerRecord;
import server.Server;

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
//			System.out.println(this.getId() + " == " + otherR.getId() + "?");
			return this.clock == otherR.clock && this.getId() == otherR.getId();
		}
		return super.equals(other);
	}

	public boolean isMine() {
		System.out.println("isMine(): " + (server != null));
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
//			System.out.println("server.getId(): " + server.getId());
			return server.getId();
		}
//		System.out.println("sr.getId(): " + sr.getId());
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
		System.out.println("isValid(): " + (acksReceived >= numServers - 1));
		return acksReceived >= numServers - 1;
	}

	public int getClock() {
		// TODO Auto-generated method stub
		return clock;
	}

	abstract public String encode();
}
