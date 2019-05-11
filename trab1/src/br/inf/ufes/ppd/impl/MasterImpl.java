package br.inf.ufes.ppd.impl;

import java.rmi.RemoteException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import br.inf.ufes.ppd.Guess;
import br.inf.ufes.ppd.Master;
import br.inf.ufes.ppd.Slave;
import br.inf.ufes.ppd.utils.Pair;

public class MasterImpl implements Master {

	private Map<UUID, Pair<Slave, String>> slaves;
	
	public MasterImpl() {
		slaves = Collections.synchronizedMap(new HashMap<UUID, Pair<Slave, String>>());
	}
	
	@Override
	public void addSlave(Slave slave, String slaveName, UUID slaveKey) throws RemoteException {
		Pair<Slave, String> pair = Pair.of(slave, slaveName);
		slaves.put(slaveKey, pair);
		
		System.out.println("Registered Slave: " + slaveName);
	}

	@Override
	public void removeSlave(UUID slaveKey) throws RemoteException {
		slaves.remove(slaveKey);
		
		System.out.println("Removed Slave: " + slaves.get(slaveKey).getRight());
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
		// TODO Auto-generated method stub
		return null;
	}

}
