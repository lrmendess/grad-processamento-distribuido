package br.ufes.inf.ppd.master;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import br.ufes.inf.ppd.Guess;
import br.ufes.inf.ppd.utils.Partition;
import br.ufes.inf.ppd.utils.Sequence;

public class Attack implements Runnable {

	private final int attackNumber;
	
	private Set<Partition> partitions;
	private List<Guess> guesses;
	
	public Attack(byte[] cipherText, byte[] knownText) {
		this.attackNumber = Sequence.nextValue();
		this.guesses = new ArrayList<Guess>();
	}
	
	public void setPartitions(Set<Partition> partitions) {
		this.partitions = partitions;
	}
	
	public synchronized void removePartition(Partition partition) {
		partitions.remove(partition);
	}
	
	public boolean hasPartitions() {
		return !partitions.isEmpty();
	}

	public int getAttackNumber() {
		return attackNumber;
	}

	public synchronized void addGuess(Guess guess) {
		guesses.add(guess);
	}

	public List<Guess> getGuesses() {
		return guesses;
	}
	
	/**
	 * Enquanto nossa lista de particoes nao estiver vazia, continuaremos segurando o ataque
	 * para o mestre nao finalizar e retornar nulo para o cliente
	 */
	@Override
	public void run() {
		try {
			while (!partitions.isEmpty()) {
				synchronized (this) {
					this.wait();	
				}
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Avisa ao ataque para ele verificar se ja pode finalizar
	 */
	public void notifyAttack() {
		synchronized (this) {
			this.notify();	
		}
	}
	
}
