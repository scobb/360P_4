package request;

import message.SynchronizeMessage;
import record.ServerRecord;
import server.Server;

public class SynchronizeRequest extends Request{

	public SynchronizeRequest(Server server, ServerRecord sr, int clock,
			int numServers) {
		super(server, sr, clock, numServers);
	}

	@Override
	public void fail() {}

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
		fulfill();
	}

	@Override
	public String encode() {
		return "S|" + clock + "|" + numServers;
	}

}
