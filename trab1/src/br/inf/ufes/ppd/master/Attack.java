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
	
	private final byte[] cipherText;
	private final byte[] knownText;
	
	private Map<UUID, Set<Partition>> slavesPartitions;
	private List<Guess> guesses;
	
	private boolean forcedTermination;
	
	public Attack(byte[] cipherText, byte[] knownText) {
		this.attackNumber = Sequence.nextValue();
		this.cipherText = cipherText;
		this.knownText = knownText;
		this.slavesPartitions = Collections.synchronizedMap(new HashMap<>());
		this.guesses = Collections.synchronizedList(new ArrayList<>());
		this.forcedTermination = false;
	}

	public int getAttackNumber() {
		return attackNumber;
	}

	public byte[] getCipherText() {
		return cipherText;
	}

	public byte[] getKnownText() {
		return knownText;
	}
	
	/**
	 * Dado um identificador de escravo e uma particao, iremos adicionar esse escravo na lista
	 * de escravo desse ataque junto da particao a qual ele foi designado a trabalhar, mas caso
	 * este ja esteja atuando nesse ataque, iremos acrescentar a particao em seu conjunto de particoes
	 * desse ataque.
	 * 
	 * @param slaveKey
	 * @param partition
	 */
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
	
	/**
	 * Dado um identificador de escravo e o ultimo indice que ele notificou ao mestre, iremos procurar
	 * pela particao a qual este index faz parte e iremos atualizar seu valor inicial, mas caso o ultimo
	 * index lido pelo escravo seja o fim da particao, iremos remove-la do seu conjunto de particoes e, se
	 * o escravo em questao nao tiver mais nenhuma particao para operar, removemos ele desse ataque.
	 * 
	 * @param slaveKey
	 * @param currentIndex
	 */
	public void updatePartition(UUID slaveKey, int currentIndex) {
		synchronized (slavesPartitions) {
//			Caso o escravo nao esteja na nossa lista, ou seu index esta em -1 (sinal de que ele nao leu nada
//			do dicionario ate o momento), apenas ignoramos e retornamos nada
			if (slavesPartitions.containsKey(slaveKey) == false || currentIndex < 0) {
				return;
			}
			
//			Recuperaca da particao do escravo em que o indice dado como parametro esta dentro
//			do raio da particao ou o escravo finalizou o trabalho (o certo eh ter apenas uma)
			Optional<Partition> existingPartition = slavesPartitions.get(slaveKey)
				.stream()
				.filter(partition -> {
//					Se estiver dentro do raio, iremos atualizar a particao
					if (partition.isBetweenTheRange(currentIndex)) {
						partition.setStart(currentIndex);
						return false;
//					Se for o fim da particao, iremos obte-la para remocao
					} else {
						return partition.getEnd() == currentIndex;
					}
				})
				.findAny();
			
//			Se uma particao foi obtida, significa que ela chegou ao fim e deve ser removida
			if (existingPartition.isPresent()) {
				Partition partition = existingPartition.get();
				Set<Partition> slavePartitions = slavesPartitions.get(slaveKey);
				slavePartitions.remove(partition);
//				Caso o conjunto de particoes do escravo esteja vazio, significa que seu papel foi cumprido
//				nesse ataque, logo pode ser removido e liberado
				if (slavePartitions.isEmpty()) {
					slavesPartitions.remove(slaveKey);
				}
			}
		}
	}
	
	/**
	 * Remove um escravo do mapa, consequentemente suas particoes vao junto
	 *
	 * @param slaveKey
	 */
	public void removeSlave(UUID slaveKey) {
		synchronized (slavesPartitions) {
			slavesPartitions.remove(slaveKey);
		}
	}
	
	/**
	 * Verifica se um escravo esta trabalhando nesse ataque
	 * 
	 * @param slaveKey
	 * @return
	 */
	public boolean hasSlave(UUID slaveKey) {
		return slavesPartitions.containsKey(slaveKey);
	}
	
	/**
	 * Retorna o conjunto de particoes de um dado escravo
	 * 
	 * @param slaveKey
	 * @return
	 */
	public Set<Partition> getPartitions(UUID slaveKey) {
		return slavesPartitions.get(slaveKey);
	}
	
	/**
	 * Adiciona um chute ao ataque
	 * 
	 * @param guess
	 */
	public synchronized void addGuess(Guess guess) {
		guesses.add(guess);
	}
	
	/**
	 * Retorna a lista de chutes do ataque
	 * 
	 * @return
	 */
	public List<Guess> guesses() {
		return guesses;
	}
	
	public void printPartitions() {
		slavesPartitions.forEach((uuid, partitions) -> {
			System.out.println(uuid + ": " + partitions);
		});
	}
	
	/**
	 * Finaliza o ataque forcadamente
	 */
	public void forcedTermination() {
		slavesPartitions.clear();
		forcedTermination = true;
	}
	
	public boolean wasForcedToTerminate() {
		return forcedTermination;
	}
	
	/**
	 * Enquanto nossa lista de particoes nao estiver vazia, continuaremos segurando o ataque
	 * para o mestre nao finalizar e retornar nulo para o cliente
	 */
	@Override
	public void run() {
		while (!emptyPartitions() && !forcedTermination) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Verifica se nao existem escravos trabalhando nesse ataque
	 * 
	 * @return
	 */
	private boolean emptyPartitions() {
		synchronized (slavesPartitions) {
			return slavesPartitions.isEmpty();
		}
	}
	
}
