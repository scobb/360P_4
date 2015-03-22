package Server;
import java.net.InetAddress;
import java.net.UnknownHostException;


public class ServerRecord {
	private InetAddress addr;
	private int port;
	private int clock;

	public int getClock() {
		return clock;
	}
	public InetAddress getAddr() {
		return addr;
	}

	public int getPort() {
		return port;
	}


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

}
