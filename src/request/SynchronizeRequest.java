package request;

import message.FinishedMessage;
import message.SynchronizeMessage;
import record.ServerRecord;
import server.Server;

public class SynchronizeRequest extends Request {
	boolean fulfilled = false;
	public SynchronizeRequest(Server server, ServerRecord sr, int clock,
			int numServers) {
		super(server, sr, clock, numServers);
	}

	public String toString() {
		return this.encode();
	}	

	@Override
	public void fulfill() {
		if (server.getId() != sr.getId()) {
			// send Synchronize message
			System.out.println("Trying to send a message from server "
					+ server.getId() + " to server " + sr.getId());
			SynchronizeMessage m = new SynchronizeMessage(server, sr);
			m.send();
	
			// this server is alive again.
			fulfilled = true;
			sr.setOnline(true);
		} else {
			System.out.println("Finishing this request for me.");
			System.out.println("My queue: " + server.getRequests());
			server.getRequests().remove();
			System.out.println("My queue: " + server.getRequests());
			System.out.println("finishing this req for everyone else...");
			server.broadcastMessage(new FinishedMessage(server, null));
		}
	}

	@Override
	public void fulfillSilently(Server server) {

	}

	@Override
	public String encode() {
		int id = -1;
		if (server != null) {
			id = sr.getId();
		}
		return "S|" + id + "|" + clock + "|" + numServers;
	}

	@Override
	public boolean isValid() {
		return !fulfilled;
	}

}
