package Server;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;
import Server.ClientRequest;


public class AcknowledgementRequest implements Runnable{
	ClientRequest cr;
	ServerRecord sr;

	public AcknowledgementRequest(ClientRequest cr, ServerRecord sr) {
		this.cr = cr;
		this.sr = sr;
	}

	@Override
	public void run() {
		Socket s = null;
		try {
			// talk to the server on the socket
			s = new Socket(sr.getAddr(), sr.getPort());

			// we'll communicate through streams: scanner and printwriter
			Scanner in = new Scanner(s.getInputStream());
			PrintWriter out = new PrintWriter(s.getOutputStream(), true);

			// construct message
			out.println("SERVER R");

			// wait for acknowledgement -- TODO, timeout here?
			in.nextLine();

			// add acknowledgement
			cr.ackReceived();

			// clean up -- not sure if these are redundant. Stream closes if
			// any is called.
			s.close();
			in.close();
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
