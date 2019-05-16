package br.inf.ufes.ppd.master;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.stream.Stream;

import br.inf.ufes.ppd.Guess;
import br.inf.ufes.ppd.Master;
import br.inf.ufes.ppd.Slave;
import br.inf.ufes.ppd.slave.SlaveWorker;
import br.inf.ufes.ppd.utils.DictionaryReader;
import br.inf.ufes.ppd.utils.Partition;
import br.inf.ufes.ppd.utils.Sequence;

public class MasterImpl implements Master {
	
	private static final long ALPHA = 500;
	private DictionaryReader dictionary;
//	Map<ID do Escravo, Escravo>
	private Map<UUID, SlaveWorker> slaves;
	private List<Thread> workingSlavesThreads;
//	Map<Numero do ataque, Particao de leitura no dicionario>
	private Map<Integer, List<Partition>> attacksAndPartitions;
//	Map<Numero do ataque, Colecao de Chutes>
	private Map<Integer, List<Guess>> attacksAndGuesses;
//	Map<ID do Escravo, Tempo da ultima resposta>
	private Map<UUID, Long> slavesTimer;
	
	public MasterImpl(String dictionaryPath) {
		try {
			dictionary = new DictionaryReader(dictionaryPath);
			slaves = Collections.synchronizedMap(new HashMap<UUID, SlaveWorker>());
			attacksAndPartitions = Collections.synchronizedMap(new HashMap<Integer, List<Partition>>());
			attacksAndGuesses = Collections.synchronizedMap(new HashMap<Integer, List<Guess>>());
			slavesTimer = Collections.synchronizedMap(new HashMap<UUID, Long>());
			workingSlavesThreads = Collections.synchronizedList(new ArrayList<Thread>());
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void addSlave(Slave slave, String slaveName, UUID slaveKey) throws RemoteException {
		synchronized (slaves) {
			SlaveWorker slaveWorker = new SlaveWorker(slave, slaveName, slaveKey);
			
//			If statement de debug para poder acompanhar as inscricoes dos escravos
//			Deve ser removido futuramente
			if (slaves.containsKey(slaveKey)) {
				notification(slaveName, "Heartbeat Received");
			} else {
				notification(slaveName, "Registered");
				slaves.put(slaveKey, slaveWorker);
			}
		}
	}

	@Override
	public void removeSlave(UUID slaveKey) throws RemoteException {
		synchronized (slaves) {
			SlaveWorker removedSlave = slaves.remove(slaveKey);
	
			String slaveName = removedSlave.getName();
			notification(slaveName, "Removed");
		}
	}

	@Override
	public synchronized void foundGuess(UUID slaveKey, int attackNumber, long currentIndex, Guess currentGuess)
			throws RemoteException {
//		Esse metodo ainda esta meio nebuloso
//		if (attacksAndGuesses.containsKey(attackNumber)) {
//			attacksAndGuesses.put(attackNumber, new ArrayList<Guess>());
//		}
//		List<Guess> attackGuesses = attacksAndGuesses.get(attackNumber);
//		
//		attackGuesses.add(currentGuess);
//		notification(slaves.get(slaveKey).getName(), "Guess received");
	}

	@Override
	public void checkpoint(UUID slaveKey, int attackNumber, long currentIndex) throws RemoteException {
//		Evita que Threads remanescentes insiram valores que nao existem mais
		if (slaves.containsKey(slaveKey) == false) {
			return;
		}
		
//		Debug
		notification(slaves.get(slaveKey).getName(), "Checkpoint received");
		
		SlaveWorker slaveWorker = slaves.get(slaveKey);
		Partition partition = slaveWorker.getPartition();
		
		if (currentIndex == partition.getMax()) {
//			Terminou a particao
//			Remover a tarefa do escravo
			slaveWorker.setPartition(null);
//			Remover a particao da lista de particoes do seu ataque
			attacksAndPartitions.get(attackNumber).remove(partition);
			
		} else {
//			Ainda nao terminou a particao
//			Reajustar o indice da particao que o escravo ja passou
			if (currentIndex != -1) {
//				Por convencao, -1 indica que o escravo ainda nao testou nenhuma palavra
				partition.setMin((int) currentIndex);
			}
		}
	}

	@Override
	public Guess[] attack(byte[] cipherText, byte[] knownText) throws RemoteException {
//		Adiciona um numero de ataque e as particoes a serem exploradas
		int attackNumber = Sequence.nextValue();
		List<Partition> partitions = partitionDictionary();
		attacksAndPartitions.put(attackNumber, partitionDictionary());
		
		Iterator<Partition> partitionsForSlaves = attacksAndPartitions.get(attackNumber).iterator();
		
		for (Entry<UUID, SlaveWorker> entry : slaves.entrySet()) {
			SlaveWorker sw = entry.getValue();
			sw.setPartition(partitionsForSlaves.next());
			sw.setSubAttackParameters(cipherText, knownText, attackNumber, this);
			Thread t = new Thread(sw);
			workingSlavesThreads.add(t);
			t.start();
		}
		
		for (Thread t : workingSlavesThreads) {
			try {
				t.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
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
		
		dictionary.rewind();
		
		return partitions;
	}
	
//	Funcao de debug (deve se removida futuramente)
	private void notification(String slaveName, String msg) {
		System.out.println("Slave[name=" + slaveName + "]: " + msg);
	}
	
}
