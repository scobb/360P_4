package request;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;

import message.FinishedMessage;
import record.ServerRecord;
import server.Server;
/**
 * ClientRequest - the primary request. From a client, requesting service
 *
 */
public class ClientRequest extends Request{
	private Socket s;
	private String reqString;

	public ClientRequest(Socket s, Server server, ServerRecord sr, int clock,
			String reqString, int numServers) {
		super(server, sr, clock, numServers);
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
			out.println(result);
			out.flush();
			out.close();
			s.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		// update number of clients served
		server.clientServed();
		
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
		}
	}

	@Override
	public void fulfillSilently(Server server) {
		server.processRequest(reqString);
	}

	@Override
	public String encode() {
		int id = -1;
		if (sr == null) {
			id = server.getId();
		} else {
			id = sr.getId();
		}
		return "C|" + id + "|" + clock + "|" + reqString + "|" + numServers;
	}

	public String toString(){
		return encode();
	}
}