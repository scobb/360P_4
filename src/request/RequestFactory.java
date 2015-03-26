package request;

import java.lang.reflect.MalformedParametersException;

import record.ServerRecord;
import server.Server;

public class RequestFactory {
	/**
	 * RequestFactory - for use in creating requests that will only be performed
	 * locally-- that is, they will not communicate with a client or another
	 * server.
	 * 
	 */
	private RequestFactory() {
	}

	public static Request decode(String code)
			throws MalformedParametersException {
		System.out.println("Decoding : " + code);
		String[] fields = code.split("\\|");
		Server s = new Server();
		s.setId(Integer.parseInt(fields[1]));
		switch (fields[0]) {
		case "C": {
			System.out.println("Client request case.");
			for (int i = 1; i < fields.length; ++i) {
				System.out.println("fields[" + i + "]: " + fields[i]);
			}
			return new ClientRequest(null, s, new ServerRecord(null,
					Integer.parseInt(fields[1])), Integer.parseInt(fields[2]),
					fields[3], Integer.parseInt(fields[4]));

		}
		case "S": {
			// TODO - need to populate Server, not ServerRecord
			System.out.println("Synchronize request case.");
			return new SynchronizeRequest(null, new ServerRecord(null,
					Integer.parseInt(fields[1])), Integer.parseInt(fields[2]),
					Integer.parseInt(fields[3]));
		}
		}
		throw new MalformedParametersException("Could not decode " + code);
	}
}
