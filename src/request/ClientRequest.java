package request;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;

import message.FinishedMessage;
import record.ServerRecord;
import server.Server;

public class ClientRequest extends Request{
	private Socket s;
	private String reqString;

	public ClientRequest(Socket s, Server server, ServerRecord sr, int clock,
			String reqString) {
		super(server, sr, clock);
		this.s = s;
		this.reqString = reqString;
	}


	@Override
	public void fulfill() {

		// time to process this request
		String result = server.processRequest(reqString);

		// send response to appropriate client
		PrintWriter out;
		try {
			out = new PrintWriter(s.getOutputStream());
			System.out.println("Writing to client: " + result );
			out.println(result);
			out.flush();
			out.close();
			s.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		// request processed. Send the finished message.
		server.broadcastMessage(new FinishedMessage(server, null));
//		for (ServerRecord sr : server.getServerRecords()) {
//			if (!sr.equals(this)) {
//				System.out.println("Sending a finished msg");
//				// send finished msg to each server that isn't me
//				server.getThreadpool().submit(new FinishedMessage(server, sr));
//			}
//		}

		// update number of clients served
		server.clientServed();

		// is it time to fail?
		if (server.getCurrentScheduledFailure() != null
				&& server.getCurrentScheduledFailure().hasFailed(server.getNumServed())) {
			server.fail();
		}
		
	}

	@Override
	public void fulfillSilently() {
		server.processRequest(reqString);
	}
}