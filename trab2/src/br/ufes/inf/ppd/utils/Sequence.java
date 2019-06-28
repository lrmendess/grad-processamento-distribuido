package br.ufes.inf.ppd.utils;

import java.util.concurrent.atomic.AtomicInteger;

public class Sequence {

	private static final AtomicInteger counter = new AtomicInteger();

	/**
	 * Gera inteiros unicos a cada chamada
	 * 
	 * @return
	 */
	public static int nextValue() {
		return counter.getAndIncrement();
	}

}
