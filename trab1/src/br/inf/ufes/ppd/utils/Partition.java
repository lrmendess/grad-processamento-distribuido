package br.inf.ufes.ppd.utils;

public class Partition {
	
	private int min;
	private int max;
	
	public Partition(int min, int max) {
		this.min = min;
		this.max = max;
	}

	public int getMin() {
		return min;
	}

	public void setMin(int min) {
		this.min = min;
	}

	public int getMax() {
		return max;
	}

	public void setMax(int max) {
		this.max = max;
	}
	
	@Override
	public String toString() {
		return "Partition[min=" + min + ", max=" + max + "]";
	}
	
}
