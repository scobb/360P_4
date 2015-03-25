package record;

import java.net.InetAddress;
import java.net.UnknownHostException;

import server.Server;

public class ServerRecord {
	private InetAddress addr;
	private int port;
	private int clock;
	private int id;
	private boolean online;

	// getters
	public int getClock() {
		return clock;
	}

	public int getId() {
		return id;
	}

	public void setOnline(boolean online) {
		this.online = online;
	}

	public boolean isOnline() {
		return online;
	}

	public InetAddress getAddr() {
		return addr;
	}

	public int getPort() {
		return port;
	}

	// constructor
	public ServerRecord(String configStr, int id) {
		if (configStr != null) {
			String[] splitAddress = configStr.split(":");
			try {
				addr = InetAddress.getByName(splitAddress[0].trim());
			} catch (UnknownHostException e) {
				e.printStackTrace();
			}
			port = Integer.parseInt(splitAddress[1]);
		} else {
			addr = null;
			port = 0;
		}
		clock = 0;
		online = true;
		this.id = id;
	}

	public boolean equals(Server other) {
		return this.addr.equals(other.getAddr())
				&& this.port == other.getPort();
	}

	public boolean equals(ServerRecord other) {
		return this.addr.equals(other.addr) && this.port == other.port;
	}
}
