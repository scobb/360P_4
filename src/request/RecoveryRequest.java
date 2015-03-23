package request;

import message.RecoveryMessage;
import record.ServerRecord;
import server.Server;

public class RecoveryRequest extends Request{
	public RecoveryRequest(Server server, ServerRecord sr, int clock){
		super(server, sr, clock);
	}
	@Override
	public void fulfill() {
		for (ServerRecord sr : this.server.getServerRecords()){
			if (!this.server.equals(sr)){
				this.server.getThreadpool().submit(new RecoveryMessage(this.server, sr));
			}
		}
		while (!this.server.isRecovered());
	}
	@Override
	public boolean isValid() {
		return true;
	}
	@Override
	public void fulfillSilently() {
		fulfill();
	}
}
