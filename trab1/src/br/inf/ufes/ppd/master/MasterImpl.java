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

public class MasterImpl implements Master {
	
//	Diretorio do dicionario
	private String dictionaryPath;
//	Lista de escravos mapeadas pelo ID do escravo
	private Map<UUID, NamedSlave> slaves;
//	Mapa que indica quais particoes um escravo esta responsavel em um ataque
	private Map<Integer, Attack> attacks;
//	Threads que estao atuando
	private Map<Integer, List<Thread>> slaveThreads;
	
	public MasterImpl(String dictionaryPath) {
		this.dictionaryPath = dictionaryPath;
		this.slaves = Collections.synchronizedMap(new HashMap<>());
		this.slaveThreads = Collections.synchronizedMap(new HashMap<>());
		this.attacks = Collections.synchronizedMap(new HashMap<>());
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
//		Remove o escravo da lista de escravos
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
		attacks.get(attackNumber).updatePartition(slaveKey, (int) currentIndex);
		
//		Notificacao para debug
		NamedSlave namedSlave = slaves.get(slaveKey);
		notification(namedSlave.getName(), "Checkpoint received");
	}

	@Override
	public Guess[] attack(byte[] cipherText, byte[] knownText) throws RemoteException {
		Attack attack = new Attack();
		Integer attackNumber = attack.getAttackNumber();
		attacks.put(attackNumber, attack);
		
		synchronized (slaveThreads) {
			slaveThreads.put(attackNumber, Collections.synchronizedList(new ArrayList<Thread>()));
		}
		
		List<Thread> currentAttackSlavesThreads = slaveThreads.get(attackNumber);
		
//		Resgata o mapa de particoes que escravos estarao trabalhando nesse ataque
		synchronized (slaves) {
			List<Partition> partitions = partitionDictionary();
			Iterator<Partition> partitionsForSlaves = partitions.iterator();
			
//			Distribui particoes para os escravos ligados ao mestre
			slaves.forEach((slaveKey, namedSlave) -> {
//				Armazena qual escravo esta responsavel por uma particao num determinado ataque
				Partition partition = partitionsForSlaves.next();
				attack.addPartition(slaveKey, partition);

//				Fornece os argumentos necessarios para a thread trabalhar uma particao e responder o mestre
				SlaveRunnable slaveRunnable = new SlaveRunnable(namedSlave);
//				Para slaveRunnable sera passado apenas uma copia da particao em que ele deve operar
				slaveRunnable.setPartition(partition);
				slaveRunnable.setSubAttackParameters(cipherText, knownText, attackNumber, this);
				
				Thread slaveThread = new Thread(slaveRunnable);
				currentAttackSlavesThreads.add(slaveThread);
			});
		}
		
//		Inicia as threads dos escravos
		currentAttackSlavesThreads.forEach(Thread::start);
		
//		Precisamos iterar por indexacao pois a iteracao nao pode falhar durante adicoes de novas threads
		currentAttackSlavesThreads.forEach(thread -> {
			try {
				thread.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		});
//		Remove todas as threads referentes a um ataque
		currentAttackSlavesThreads.clear();
		
		synchronized (slaveThreads) {
			slaveThreads.remove(attackNumber);
		}
		
		attack.printSlavePartitions();
		
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
