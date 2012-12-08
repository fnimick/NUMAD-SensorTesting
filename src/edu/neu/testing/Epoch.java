package edu.neu.testing;

class Epoch {
	long timestamp;
	double activity;
	Epoch(long l, double d) {
		timestamp = l;
		activity = d;
	}
	
	public String toString() {
		return timestamp + ": " + activity;
	}
}