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
	private Map<UUID, Set<Partition>> slavesPartitions;
	private List<Guess> guesses;
	
	public Attack() {
		this.attackNumber = Sequence.nextValue();
		this.slavesPartitions = Collections.synchronizedMap(new HashMap<>());
		this.guesses = Collections.synchronizedList(new ArrayList<>());
	}

	public int getAttackNumber() {
		return attackNumber;
	}
	
	public void addPartition(UUID slaveKey, Partition partition) {
		synchronized (slavesPartitions) {
			if (slavesPartitions.containsKey(slaveKey)) {
				Set<Partition> partitions = slavesPartitions.get(slaveKey);
				partitions.add(partition);
			} else {
				Set<Partition> partitions = new HashSet<Partition>();
				partitions.add(partition);
				slavesPartitions.put(slaveKey, partitions);
			}
		}
	}
	
	public void updatePartition(UUID slaveKey, int currentIndex) {
		synchronized (slavesPartitions) {
			if (slavesPartitions.containsKey(slaveKey) == false || currentIndex < 0) {
				return;
			}
			
			Optional<Partition> existingPartition = slavesPartitions.get(slaveKey)
				.stream()
				.filter(partition -> partition.isBetweenTheRange(currentIndex))
				.findAny();
			
			if (existingPartition.isPresent()) {
				Partition partition = existingPartition.get();
//				Se essa condicao for satisfeita, quer dizer que a particao inteira foi lida
				if (partition.getMax() == (currentIndex + 1)) {
					Set<Partition> slavePartitions = slavesPartitions.get(slaveKey);
					slavePartitions.remove(partition);
					
					if (slavePartitions.isEmpty()) {
						slavesPartitions.remove(slaveKey);
					}
				} else {
					partition.setMin(currentIndex);
				}
			}
		}
	}
	
	public void removeSlave(UUID slaveKey) {
		synchronized (slavesPartitions) {
			slavesPartitions.remove(slaveKey);
		}
	}
	
	public Set<Partition> getPartitions(UUID slaveKey) {
		return slavesPartitions.get(slaveKey);
	}
	
	public void addGuess(Guess guess) {
		synchronized (guesses) {
			guesses.add(guess);
		}
	}
	
	public List<Guess> guesses() {
		return guesses;
	}
	
	public boolean emptyPartitions() {
		synchronized (slavesPartitions) {
			return slavesPartitions.isEmpty();
		}
	}
	
//	Funcao para debugacao do ataque
	public void printSlavePartitions() {
		synchronized (slavesPartitions) {
			slavesPartitions.entrySet().forEach(System.out::println);
		}
	}

	@Override
	public void run() {
		while (!emptyPartitions()) {
			try {
				Thread.sleep(1000);
//				printSlavePartitions();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
}
