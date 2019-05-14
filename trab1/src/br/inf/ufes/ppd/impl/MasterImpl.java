package br.inf.ufes.ppd.impl;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import br.inf.ufes.ppd.Guess;
import br.inf.ufes.ppd.Master;
import br.inf.ufes.ppd.Slave;
import br.inf.ufes.ppd.utils.DictionaryReader;
import br.inf.ufes.ppd.utils.Pair;

public class MasterImpl implements Master {
	
//	Dictionary
	private DictionaryReader dictionary;
//	List<Particao do dicionario>
	private List<Pair<Integer, Integer>> partitions;
//	Map<ID do Escravo, Pair<Nome do Escravo, Referencia do Escravo>>
	private Map<UUID, Pair<String, Slave>> slaves;

	public MasterImpl(String dictionaryPath) {
		try {
			dictionary = new DictionaryReader(dictionaryPath);
			partitions = Collections.synchronizedList(new ArrayList<Pair<Integer, Integer>>());
			slaves = Collections.synchronizedMap(new HashMap<UUID, Pair<String, Slave>>());
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Override
	public synchronized void addSlave(Slave slave, String slaveName, UUID slaveKey) throws RemoteException {
		synchronized (slaves) {
			Pair<String, Slave> slavePair = Pair.of(slaveName, slave);
			
//			If statement de debug para poder acompanhar as inscricoes dos escravos
//			Deve ser removido futuramente
			if (slaves.containsKey(slaveKey)) {
				notification(slaveKey, slaveName, "Heartbeat Received");
				
			} else {
				notification(slaveKey, slaveName, "Registered");
				slaves.put(slaveKey, slavePair);
			}
		}
	}

	@Override
	public synchronized void removeSlave(UUID slaveKey) throws RemoteException {
		synchronized (slaves) {
			Pair<String, Slave> removedSlavePair = slaves.remove(slaveKey);
	
			String slaveName = removedSlavePair.getLeft();
			notification(slaveKey, slaveName, "Removed");
		}
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
		int dictionaryLength = dictionary.countAllLines();
		int numberOfSlaves = slaves.size();
		int partitionLength = dictionaryLength / numberOfSlaves;
		
		/* Fefo's algorithm */ 
		for (int i = 0; i < (numberOfSlaves - 1); i++) {
			Pair<Integer, Integer> pair = Pair.of(i * partitionLength, i * partitionLength + partitionLength);
			partitions.add(pair);
		}
		
		Pair<Integer, Integer> pair = Pair.of((numberOfSlaves - 1) * partitionLength, dictionaryLength);
		partitions.add(pair);
		
//		while (dictionary.ready()) {
//			int startIndex = dictionary.getLineNumber();
//			dictionary.readLines(partitionLength);
//			int endIndex = dictionary.getLineNumber();
//			
//			partitions.add(Pair.of(startIndex, endIndex));
//		}
		
		partitions.forEach(System.out::println);
		
		return null;
	}
	
//	Funcao de debug (deve se removida futuramente)
	private void notification(UUID slaveId, String slaveName, String msg) {
		System.out.println("Slave[name=" + slaveName + "]: " + msg);
	}
	
}
