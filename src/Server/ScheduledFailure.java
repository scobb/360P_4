package Server;

public class ScheduledFailure {
	private int k;
	private int delta;

	public ScheduledFailure(int k, int delta) {
		this.k = k;
		this.delta = delta;
	}

	public boolean hasFailed(int numServed) {
		return numServed >= this.k;
	}

}
