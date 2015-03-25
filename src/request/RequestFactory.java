package request;

import java.lang.reflect.MalformedParametersException;

public class RequestFactory {
	/**RequestFactory - for use in creating requests that will only be performed locally--
	 * that is, they will not communicate with a client or another server.
	 * 
	 */
	private RequestFactory() {
	}

	public static Request decode(String code) throws MalformedParametersException {
		String[] fields = code.split("|");
		switch (fields[0]) {
		case "C": {
			return new ClientRequest(null, null, null,
					Integer.parseInt(fields[1]), fields[2],
					Integer.parseInt(fields[3]));

		}
		case "R": {
			return new RecoveryRequest(null, null, Integer.parseInt(fields[1]),
					Integer.parseInt(fields[2]));

		}
		case "S": {
			return new SynchronizeRequest(null, null, Integer.parseInt(fields[1]),
					Integer.parseInt(fields[2]));
		}
		}
		throw new MalformedParametersException("Could not decode " + code);
	}
}
