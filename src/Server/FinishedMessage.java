package Server;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class FinishedMessage implements Runnable{
	ServerRecord sr;
	public FinishedMessage(ServerRecord sr){
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

			// send finished message
			out.println("SERVER F");
			out.flush();
			out.close();
			in.close();
			s.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
