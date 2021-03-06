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

	/**
	 * 
	 * @param code
	 *            string to decode
	 * @param s
	 *            decoding server
	 * @return Request of appropriate type
	 * @throws MalformedParametersException
	 */
	public static Request decode(String code, Server s)
			throws MalformedParametersException {
		String[] fields = code.split("\\|");
		switch (fields[0]) {
		case "C": {
			return new ClientRequest(null, null, new ServerRecord(null,
					Integer.parseInt(fields[1])), Integer.parseInt(fields[2]),
					fields[3], Integer.parseInt(fields[4]));

		}
		case "S": {
			return new SynchronizeRequest(s, s.getServerRecords().get(
					Integer.parseInt(fields[1])), Integer.parseInt(fields[2]),
					Integer.parseInt(fields[3]));
		}
		}
		throw new MalformedParametersException("Could not decode " + code);
	}
}
