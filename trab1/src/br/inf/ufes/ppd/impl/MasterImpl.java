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
import br.inf.ufes.ppd.utils.Partition;

public class MasterImpl implements Master {
	
//	Dictionary
	private DictionaryReader dictionary;
//	Map<ID do Escravo, Pair<Nome do Escravo, Referencia do Escravo>>
	private Map<UUID, Pair<String, Slave>> slaves;
//	Numero de ataques
	private int attacks;
//	Map<Numero do ataque, Particao de leitura no dicionario>
	private Map<Integer, List<Partition>> attacksAndPartitions;

	public MasterImpl(String dictionaryPath) {
		try {
			dictionary = new DictionaryReader(dictionaryPath);
			slaves = Collections.synchronizedMap(new HashMap<UUID, Pair<String, Slave>>());
			
			attacks = 0;
			attacksAndPartitions = Collections.synchronizedMap(new HashMap<Integer, List<Partition>>());
			
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
//		Cria uma lista de particoes do dicionario para um ataque
		attacksAndPartitions.put(attacks++, partitionDictionary());
		
		return null;
	}
	
	private List<Partition> partitionDictionary() {
		List<Partition> partitions = new ArrayList<Partition>();
		
		int partitionLength = dictionary.countAllLines() / slaves.size();
		int partitionLeftovers = dictionary.countAllLines() % slaves.size();
		
		while (dictionary.ready()) {
			int min = dictionary.getLineNumber();

			if (partitionLeftovers > 0) {
				dictionary.seek(partitionLength + 1);
				partitionLeftovers--;
			} else {
				dictionary.seek(partitionLength);
			}
			
			int max = dictionary.getLineNumber();
			
			partitions.add(new Partition(min, max));
		}
		
		return partitions;
	}
	
//	Funcao de debug (deve se removida futuramente)
	private void notification(UUID slaveId, String slaveName, String msg) {
		System.out.println("Slave[name=" + slaveName + "]: " + msg);
	}
	
}
