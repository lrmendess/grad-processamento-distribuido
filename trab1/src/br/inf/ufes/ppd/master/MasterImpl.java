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
import java.util.UUID;

import br.inf.ufes.ppd.Guess;
import br.inf.ufes.ppd.Master;
import br.inf.ufes.ppd.Slave;
import br.inf.ufes.ppd.slave.NamedSlave;
import br.inf.ufes.ppd.slave.SlaveRunnable;
import br.inf.ufes.ppd.utils.DictionaryReader;
import br.inf.ufes.ppd.utils.Partition;
import br.inf.ufes.ppd.utils.Sequence;

public class MasterImpl implements Master {
	
//	Diretorio do dicionario
	private String dictionaryPath;
//	Lista de escravos mapeadas pelo ID do escravo
	private Map<UUID, NamedSlave> slaves;
//	Lista de particoes por ataque
	private Map<Integer, List<Partition>> attacksPartitions;
//	Mapa que indica qual particao um escravo esta responsavel em um ataque
	private Map<Integer, Map<UUID, Partition>> slavesPartitions;
//	Threads que estao atuando
	private Map<Integer, List<Thread>> workingSlaves;
	
	public MasterImpl(String dictionaryPath) {
		this.dictionaryPath = dictionaryPath;
		this.slaves = Collections.synchronizedMap(new HashMap<>());
		this.attacksPartitions = Collections.synchronizedMap(new HashMap<>());
		this.workingSlaves = Collections.synchronizedMap(new HashMap<>());
		this.slavesPartitions = Collections.synchronizedMap(new HashMap<>());
	}
	
	@Override
	public void addSlave(Slave slave, String slaveName, UUID slaveKey) throws RemoteException {
		synchronized (slaves) {
			NamedSlave namedSlave = new NamedSlave(slave, slaveName, slaveKey);
			
//			If statement de debug para poder acompanhar as inscricoes dos escravos
//			Deve ser removido futuramente
			if (slaves.containsKey(slaveKey)) {
				notification(slaveName, "Heartbeat Received");
			} else {
				notification(slaveName, "Registered");
				slaves.put(slaveKey, namedSlave);
			}
		}
	}

	@Override
	public void removeSlave(UUID slaveKey) throws RemoteException {
		synchronized (slaves) {
			NamedSlave removedSlave = slaves.remove(slaveKey);
	
			String slaveName = removedSlave.getName();
			notification(slaveName, "Removed");
		}
	}

	@Override
	public synchronized void foundGuess(UUID slaveKey, int attackNumber, long currentIndex, Guess currentGuess)
			throws RemoteException {
	}

	@Override
	public void checkpoint(UUID slaveKey, int attackNumber, long currentIndex) throws RemoteException {
		synchronized (slaves) {
//			Evita que Threads remanescentes insiram valores que nao existem mais
			if (slaves.containsKey(slaveKey) == false) {
				return;
			}
			
//			Debug
			NamedSlave namedSlave = slaves.get(slaveKey);
			notification(namedSlave.getName(), "Checkpoint received");
		}
		
		synchronized (slavesPartitions) {
			synchronized (attacksPartitions) {
				Map<UUID, Partition> attackSlavesPartitions = slavesPartitions.get(attackNumber);
				Partition partition = attackSlavesPartitions.get(slaveKey);
				
				if (currentIndex == partition.getMax()) {
//					Terminou a particao
//					Remover a particao em que o escravo de UUID "slaveKey" estava trabalhando
					attackSlavesPartitions.remove(slaveKey);
//					Remover a particao da lista de particoes do ataque
//					TODO tentar remover por index
					attacksPartitions.get(attackNumber).remove(partition);
				} else {
//					Ainda nao terminou a particao
//					Reajustar o indice da particao que o escravo ja passou
					if (currentIndex != -1) {
//						Por convencao, -1 indica que o escravo ainda nao testou nenhuma palavra
						int toInt = (int) currentIndex;
						partition.setMin(toInt);
					}
				}
			}
		}
	}

	@Override
	public Guess[] attack(byte[] cipherText, byte[] knownText) throws RemoteException {
//		Resgatar o numero do ataque e as particoes de dicionario que serao distribuidas
		Integer attackNumber = Sequence.nextValue();
		List<Partition> partitions = partitionDictionary();
		
		synchronized (attacksPartitions) {
//			Armazena as particoes referentes a um ataque
			attacksPartitions.put(attackNumber, partitions);
		}
		
		synchronized (slavesPartitions) {
//			Inicializa o mapa que indica qual escravo esta trabalhando numa particao especifica
			slavesPartitions.put(attackNumber, Collections.synchronizedMap(new HashMap<>()));
		}
		
		synchronized (workingSlaves) {
			workingSlaves.put(attackNumber, Collections.synchronizedList(new ArrayList<>()));
		}
		
		List<Thread> currentAttackSlavesThreads = workingSlaves.get(attackNumber);
		Map<UUID, Partition> currentAttackSlavesPartitions = slavesPartitions.get(attackNumber);
		Iterator<Partition> partitionsForSlaves = attacksPartitions.get(attackNumber).iterator();
		
//		Resgata o mapa de particoes que escravos estarao trabalhando nesse ataque
		synchronized (slaves) {
//			Distribui particoes para os escravos ligados ao mestre
			slaves.forEach((slaveKey, namedSlave) -> {
//				Armazena qual escravo esta responsavel por uma particao num determinado ataque
				Partition partition = partitionsForSlaves.next();
				currentAttackSlavesPartitions.put(slaveKey, partition);

//				Fornece os argumentos necessarios para a thread trabalhar uma particao e responder o mestre
				SlaveRunnable slaveRunnable = new SlaveRunnable(namedSlave);
//				Para slaveRunnable sera passado apenas uma copia da particao em que ele deve operar
				slaveRunnable.setPartition(new Partition(partition));
				slaveRunnable.setSubAttackParameters(cipherText, knownText, attackNumber, this);
				
				Thread slaveThread = new Thread(slaveRunnable);
				currentAttackSlavesThreads.add(slaveThread);
				slaveThread.start();
			});
		}
		
		synchronized (currentAttackSlavesThreads) {
			for (Thread thread : currentAttackSlavesThreads) {
				try {
					thread.join();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			
			currentAttackSlavesThreads.clear();
		}
		
		attacksPartitions.get(attackNumber).forEach(System.out::println);
		slavesPartitions.get(attackNumber).entrySet().forEach(System.out::println);
		
		return null;
	}
	
	private List<Partition> partitionDictionary() {
		DictionaryReader dictionary = null;
		
		try {
			dictionary = new DictionaryReader(dictionaryPath);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		List<Partition> partitions = new ArrayList<Partition>();
		
		int partitionLength = 0, partitionLeftovers = 0;
		synchronized (slaves) {
			partitionLength = dictionary.countAllLines() / slaves.size();
			partitionLeftovers = dictionary.countAllLines() % slaves.size();
		}
		
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
	private void notification(String slaveName, String msg) {
		System.out.println("Slave[name=" + slaveName + "]: " + msg);
	}
	
}
