package record;

public class FailureRecord {
	private int k;
	private int delta;

	public FailureRecord(int k, int delta) {
		this.k = k;
		this.delta = delta;
	}

	public boolean hasFailed(int numServed) {
		return numServed >= this.k;
	}

}
