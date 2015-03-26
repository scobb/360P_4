package request;

import static org.junit.Assert.*;

import org.junit.Test;

import record.ServerRecord;
import server.Server;

public class RequestFactoryTest {
	@Test
	public void ClientRequestTest(){
		Server s = new Server();
		ClientRequest r = new ClientRequest(null, null, new ServerRecord(null, 1), 1, "zomg", 1);
		String encodedR = r.encode();
		
		Request newR = RequestFactory.decode(encodedR, s);
		assertEquals(r, newR);
	}
	
	@Test
	public void SynchronizeRequestTest() {
		Server s = new Server();
		SynchronizeRequest r = new SynchronizeRequest(s, new ServerRecord(null, 1), 1, 2);
		String encodedR = r.encode();
		
		Request newR = RequestFactory.decode(encodedR, s);
		assertEquals(r, newR);
	}

}
