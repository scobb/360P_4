package record;

import java.net.InetAddress;
import java.net.UnknownHostException;

import server.Server;

public class ServerRecord {
	private InetAddress addr;
	private int port;
	private int clock;

	// getters
	public int getClock() {
		return clock;
	}

	public InetAddress getAddr() {
		return addr;
	}

	public int getPort() {
		return port;
	}

	// constructor
	public ServerRecord(String configStr) {
		String[] splitAddress = configStr.split(":");
		try {
			addr = InetAddress.getByName(splitAddress[0]);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		port = Integer.parseInt(splitAddress[1]);
		clock = 0;
	}

	public boolean equals(Server other) {
		return this.addr == other.getTcpSocket().getInetAddress()
				&& this.port == other.getTcpSocket().getLocalPort();
	}

}
