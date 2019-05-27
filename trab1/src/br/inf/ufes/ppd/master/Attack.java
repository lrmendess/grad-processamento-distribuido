package br.inf.ufes.ppd.master;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import br.inf.ufes.ppd.Guess;
import br.inf.ufes.ppd.utils.Partition;
import br.inf.ufes.ppd.utils.Sequence;

public class Attack implements Runnable {

	private final int attackNumber;
	
	private byte[] cipherText;
	private byte[] knownText;
	
	private Map<UUID, Set<Partition>> slavesPartitions;
	private List<Guess> guesses;
	
	public Attack(byte[] cipherText, byte[] knownText) {
		this.attackNumber = Sequence.nextValue();
		this.cipherText = cipherText;
		this.knownText = knownText;
		this.slavesPartitions = Collections.synchronizedMap(new HashMap<>());
		this.guesses = Collections.synchronizedList(new ArrayList<>());
	}

	public int getAttackNumber() {
		return attackNumber;
	}
	
	public void addPartition(UUID slaveKey, Partition partition) {
		synchronized (slavesPartitions) {
//			Caso o escravo ja esteja cadastrado, iremos apenas acrescentar uma particao em seu conjunto
//			de particoes NESSE ataque
			if (slavesPartitions.containsKey(slaveKey)) {
				Set<Partition> partitions = slavesPartitions.get(slaveKey);
				partitions.add(partition);
			} else {
//				Se nao estiver cadastrado, iremos criar um conjunto de particoes para ele e adiciona-lo
				Set<Partition> partitions = new HashSet<Partition>();
				partitions.add(partition);
				slavesPartitions.put(slaveKey, partitions);
			}
		}
	}
	
	public void updatePartition(UUID slaveKey, int currentIndex) {
		synchronized (slavesPartitions) {
//			Caso o escravo nao esteja na nossa lista, ou seu index esta em -1 (sinal de que ele nao leu nada
//			do dicionario ate o momento), apenas ignoramos e retornamos nada
			if (slavesPartitions.containsKey(slaveKey) == false || currentIndex < 0) {
				return;
			}
			
//			Recuperaca da particao do escravo em que o indice dado como parametro esta dentro
//			do raio da particao (o certo eh ter apenas uma)
			Optional<Partition> existingPartition = slavesPartitions.get(slaveKey)
				.stream()
				.filter(partition -> partition.isBetweenTheRange(currentIndex))
				.findAny();
			
			if (existingPartition.isPresent()) {
				Partition partition = existingPartition.get();
//				Se o meu indice atual for o ultimo indice do array, significa que indice + 1 = tamanho do array,
//				logo essa particao foi finalizada e ela pode ser removida do conjunto de particoes do escravo
				if (partition.getEnd() == (currentIndex + 1)) {
					Set<Partition> slavePartitions = slavesPartitions.get(slaveKey);
					slavePartitions.remove(partition);
//					Caso o conjunto de particoes do escravo esteja vazio, significa que seu papel foi cumprido
//					nesse ataque, logo pode ser removido e liberado
					if (slavePartitions.isEmpty()) {
						slavesPartitions.remove(slaveKey);
					}
				} else {
//					Com a particao nao terminada, iremos apenas atualizar seu indice de inicio
					partition.setStart(currentIndex);
				}
			}
		}
	}
	
//	Remove um escravo do mapa, consequentemente suas particoes vao junto
	public void removeSlave(UUID slaveKey) {
		synchronized (slavesPartitions) {
			slavesPartitions.remove(slaveKey);
		}
	}
	
	public boolean hasSlave(UUID slaveKey) {
		return slavesPartitions.containsKey(slaveKey);
	}
	
//	Retorna o conjunto de particoes de um dado escravo
	public Set<Partition> getPartitions(UUID slaveKey) {
		return slavesPartitions.get(slaveKey);
	}
	
//	Retorna todas as particoes dos escravos contidos na lista de entrada
	public List<Partition> slavesPartitions(List<UUID> slaves) {
		List<Partition> returnedPartitions = new ArrayList<>();
		
		synchronized (slavesPartitions) {
			slaves.forEach(uuid -> {
				returnedPartitions.addAll(slavesPartitions.get(uuid));
			});
		}
		
		return returnedPartitions;
	}
	
//	Remove todos os escravos da lista do mapa de escravos
	public void removeSlaves(List<UUID> slaves) {
		synchronized (slavesPartitions) {
			slaves.forEach(uuid -> {
				slavesPartitions.remove(uuid);
			});
		}
	}
	
	public void addGuess(Guess guess) {
		synchronized (guesses) {
			guesses.add(guess);
		}
	}
	
	public List<Guess> guesses() {
		return guesses;
	}
	
	public void printPartitions() {
		slavesPartitions.forEach((uuid, partitions) -> {
			System.out.println(uuid + ": " + partitions);
		});
	}
	
	@Override
	public void run() {
//		Enquanto houverem particoes a serem processadas, nosso ataque nao terminou
		while (!emptyPartitions()) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	private boolean emptyPartitions() {
		synchronized (slavesPartitions) {
			return slavesPartitions.isEmpty();
		}
	}

	public byte[] getCipherText() {
		return cipherText;
	}

	public byte[] getKnownText() {
		return knownText;
	}
	
}
