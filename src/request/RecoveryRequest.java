package request;

import message.RecoveryMessage;
import message.SynchronizeMessage;
import record.ServerRecord;
import server.Server;

public class RecoveryRequest extends Request{
	public RecoveryRequest(Server server, ServerRecord sr, int clock, int numServers){
		super(server, sr, clock, numServers);
	}
	@Override
	public void fulfill() {
		server.broadcastMessage(new RecoveryMessage(this.server, null));
		System.out.println("Recovering...");
		
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
	@Override
	public String encode() {
		// TODO Auto-generated method stub
		return "R|" + sr.getClock() + "|" +  clock + "|" + numServers;
	}
	
	public String toString() {
		if (sr != null ) {
			return encode();
		}
		return "R|" + clock;
	}
}
