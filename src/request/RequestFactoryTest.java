package request;

import static org.junit.Assert.*;

import org.junit.Test;

import record.ServerRecord;

public class RequestFactoryTest {
	@Test
	public void ClientRequestTest(){
		ClientRequest r = new ClientRequest(null, null, new ServerRecord(null, 1), 1, "zomg", 1);
		String encodedR = r.encode();
		
		Request newR = RequestFactory.decode(encodedR);
		assertEquals(r, newR);
	}
	
	@Test
	public void SynchronizeRequestTest() {
		SynchronizeRequest r = new SynchronizeRequest(null, new ServerRecord(null, 1), 1, 2);
		String encodedR = r.encode();
		
		Request newR = RequestFactory.decode(encodedR);
		assertEquals(r, newR);
	}
	
	@Test
	public void RecoveryRequestTest() {
		RecoveryRequest r = new RecoveryRequest(null, new ServerRecord(null, 1), 1, 2);
		String encodedR = r.encode();
		
		Request newR = RequestFactory.decode(encodedR);
		assertEquals(r, newR);
	}
}
