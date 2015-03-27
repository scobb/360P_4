package server;

import java.util.NoSuchElementException;
import java.util.Scanner;
/**
 * StdInHandler - Used to read from command line
 *
 */
public class StdInHandler implements Runnable{
	Server s;
	Scanner in;
	public StdInHandler(Server s, Scanner in){
		this.s = s;
		this.in = in;
		
	}

	@Override
	public void run() {
		// go until user enters an empty line.
		while (true) {
			try {
				s.addScheduledFailure(in.nextLine());
			} catch (NoSuchElementException e) {
				break;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
	}
	
}
