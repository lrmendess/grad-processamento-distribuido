package br.inf.ufes.ppd.utils;

public class Pair<L, R> {

	private L left;
	private R right;

	public static <L, R> Pair<L, R> of(L left, R right) {
		Pair<L, R> newPair = new Pair<L, R>();

		newPair.left = left;
		newPair.right = right;

		return newPair;
	}

	public L getLeft() {
		return left;
	}

	public R getRight() {
		return right;
	}

	/* Generated Methods */

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((left == null) ? 0 : left.hashCode());
		result = prime * result + ((right == null) ? 0 : right.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Pair<?, ?> other = (Pair<?, ?>) obj;
		if (left == null) {
			if (other.left != null)
				return false;
		} else if (!left.equals(other.left))
			return false;
		if (right == null) {
			if (other.right != null)
				return false;
		} else if (!right.equals(other.right))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "Pair [left=" + left + ", right=" + right + "]";
	}

}
