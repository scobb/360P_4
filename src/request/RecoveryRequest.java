package request;

import message.RecoveryMessage;
import record.ServerRecord;
import server.Server;

public class RecoveryRequest extends Request{
	public RecoveryRequest(Server server, ServerRecord sr, int clock, int numServers){
		super(server, sr, clock, numServers);
	}
	@Override
	public void fulfill() {
		if (server != null){
			server.broadcastMessage(new RecoveryMessage(this.server, null));
			while (!this.server.hasRecovered());
			System.out.println("Recovered.");
		}
		
		// everyone else will do nothing, and wait for this guy to finish.
	}
	@Override
	public boolean isMine(){
		return true;
	}
	@Override
	public void fail(){}
	@Override
	public void fulfillSilently(Server server) {
		fulfill();
	}
	@Override
	public boolean isValid(){
		if (server != null){
			return true;
		}
		return super.isValid();
	}
}
