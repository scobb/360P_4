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
	
	public void fail(){
		System.out.println("failing in client request.");
		try {
			PrintWriter out = new PrintWriter(s.getOutputStream());
			out.println("FAIL");
			out.flush();
			s.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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

		// update number of clients served
		server.clientServed();

		// is it time to fail?
		System.out.println("numServed: " + server.getNumServed());
		if (server.getCurrentScheduledFailure() != null
				&& server.getCurrentScheduledFailure().hasFailed(server.getNumServed())) {
			server.fail();
		}
		
	}
	public String getMsg(){
		return reqString;
	}
	@Override

	public void ping(){
		PrintWriter out;
		try {
			out = new PrintWriter(s.getOutputStream());
			out.println("WAIT");
			out.flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void fulfillSilently(Server server) {
		server.processRequest(reqString);
	}

}