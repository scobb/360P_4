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
			SynchronizeMessage m = new SynchronizeMessage(server, sr);
			m.send();
	
			// this server is alive again.
			fulfilled = true;
			sr.setOnline(true);
		} else {
			// finish for me
			server.getRequests().remove();
			
			// finish for everyone else
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
