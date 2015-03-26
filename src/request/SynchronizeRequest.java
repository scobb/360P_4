package request;

import message.FinishedMessage;
import message.SynchronizeMessage;
import record.ServerRecord;
import server.Server;

public class SynchronizeRequest extends Request {

	public SynchronizeRequest(Server server, ServerRecord sr, int clock,
			int numServers) {
		super(server, sr, clock, numServers);
	}

	@Override
	public void fulfill() {
		if (server != null) {
			// send Synchronize message
			SynchronizeMessage m = new SynchronizeMessage(server, sr);
			m.send();

			// this server is alive again.
			sr.setOnline(true);
		}
	}

	@Override
	public void fulfillSilently(Server server) {
	}

	@Override
	public String encode() {
		int id = -1;
		if (server != null) {
			id = server.getId();
		}
		return "S|" + id + "|" + clock + "|" + numServers;
	}

}
