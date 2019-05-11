package br.inf.ufes.ppd.impl;

import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import br.inf.ufes.ppd.Guess;
import br.inf.ufes.ppd.Master;
import br.inf.ufes.ppd.Slave;
import br.inf.ufes.ppd.utils.Pair;

public class MasterImpl implements Master {

	private Map<UUID, Pair<Slave, String>> slaves;
	private Set<UUID> busySlaves;
	
	public MasterImpl() {
		slaves = new HashMap<UUID, Pair<Slave, String>>();
		busySlaves = new HashSet<UUID>();
	}
	
	@Override
	public synchronized void addSlave(Slave slave, String slaveName, UUID slaveKey) throws RemoteException {
		Pair<Slave, String> pair = Pair.of(slave, slaveName);
		slaves.put(slaveKey, pair);
		
		System.out.println("Registered Slave: " + slaveName);
	}

	@Override
	public synchronized void removeSlave(UUID slaveKey) throws RemoteException {
		Pair<Slave, String> removedSlavePair = slaves.remove(slaveKey);
		busySlaves.remove(slaveKey);
		
		System.out.println("Removed Slave: " + removedSlavePair.getRight());
	}

	@Override
	public void foundGuess(UUID slaveKey, int attackNumber, long currentIndex, Guess currentGuess)
			throws RemoteException {
		// TODO Auto-generated method stub

	}

	@Override
	public void checkpoint(UUID slaveKey, int attackNumber, long currentIndex) throws RemoteException {
		// TODO Auto-generated method stub

	}

	@Override
	public Guess[] attack(byte[] cipherText, byte[] knownText) throws RemoteException {
//		Solucao temporaria para caso nao exista escravos cadastrados
		while (slaves.isEmpty());
		
		return null;
	}

}
