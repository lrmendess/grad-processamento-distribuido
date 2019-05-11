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

	private Map<UUID, Pair<String, Slave>> slaves;
	private Set<UUID> busySlaves;
	
	public MasterImpl() {
		slaves = new HashMap<UUID, Pair<String, Slave>>();
		busySlaves = new HashSet<UUID>();
	}
	
	@Override
	public synchronized void addSlave(Slave slave, String slaveName, UUID slaveKey) throws RemoteException {
		Pair<String, Slave> slavePair = Pair.of(slaveName, slave);
		
//		If statement de debug para poder acompanhar as inscricoes dos escravos
//		Deve ser removido futuramente
		if (slaves.containsKey(slaveKey)) {
			notification(slaveKey, slaveName, "Received Heartbeat");
			
		} else {
			notification(slaveKey, slaveName, "Registered");
			slaves.put(slaveKey, slavePair);
		}

	}

	@Override
	public synchronized void removeSlave(UUID slaveKey) throws RemoteException {
		Pair<String, Slave> removedSlavePair = slaves.remove(slaveKey);
		busySlaves.remove(slaveKey);

		String slaveName = removedSlavePair.getLeft();
		notification(slaveKey, slaveName, "Removed");
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
	
	private void notification(UUID slaveId, String slaveName, String msg) {
		System.out.println("Slave[name=" + slaveName + "]: " + msg);
	}
	
}
