package br.inf.ufes.ppd.master;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import br.inf.ufes.ppd.Guess;
import br.inf.ufes.ppd.Master;
import br.inf.ufes.ppd.Slave;
import br.inf.ufes.ppd.slave.NamedSlave;
import br.inf.ufes.ppd.utils.DictionaryReader;
import br.inf.ufes.ppd.utils.Partition;

public class MasterImpl implements Master {

//	Diretorio do dicionario
	private DictionaryReader dictionaryReader;
//	Lista de escravos mapeadas pelo ID do escravo
	private Map<UUID, NamedSlave> slaves;
//	Mapa que indica quais particoes um escravo esta responsavel em um ataque
	private Map<Integer, Attack> attacks;
//	Lista de tempo de comunicacao dos escravos
	private Map<UUID, Long> slavesTimer;

	public MasterImpl(String dictionaryPath) {
		this.dictionaryReader = new DictionaryReader(dictionaryPath);
		this.slaves = Collections.synchronizedMap(new HashMap<>());
		this.attacks = Collections.synchronizedMap(new HashMap<>());
		this.slavesTimer = Collections.synchronizedMap(new HashMap<>());
		
//		So Deus pra entender isso aqui, to tentando...
//		TODO otimizar essa metodo inteiro... Esta extremamente custoso!
		new Thread() {
			
			@Override
			public void run() {
				while (true) {
					try {
						Thread.sleep(5000);
						synchronized (slavesTimer) {
//							Preenche uma lista com escravos que nao respondem ha mais de 20 + 1 (alpha) segundos
							List<UUID> candidateTobeRemoved = new ArrayList<>();
							slavesTimer.forEach((uuid, time) -> {
								Long currentTimeMillis = Calendar.getInstance().getTimeInMillis();
								
								if ((currentTimeMillis - time) > 21000) {
									candidateTobeRemoved.add(uuid);
								}
							});
							
//							Filtra os escravos que nao respondem a mais de 20 segundos pq ja terminaram seu servico
//							normalmente
							List<UUID> removedSlaves = new ArrayList<>();
							synchronized (attacks) {
								candidateTobeRemoved.forEach(uuid -> {
									boolean busySlave = attacks.entrySet()
											.stream()
											.anyMatch(entry -> {
												return entry.getValue().hasSlave(uuid);
											});
									
//									Se o escravo nao responde a mais de 20 segundos e esta ocupado em algum ataque
//									significa que ele caiu e esta pendente por ai...
									if (busySlave) {
										removedSlaves.add(uuid);
										slavesTimer.remove(uuid);
//									Mas se ele nao esta ocupado, quer dizer que terminou seu servico
									} else {
										slavesTimer.remove(uuid);
									}
								});
							}
							
							if (!removedSlaves.isEmpty()) {
//								Agora vamos reescalonar as particoes dos escravos removidos para outros escravos
								synchronized (slaves) {
									removedSlaves.forEach(uuid -> {
										slaves.remove(uuid);
									});

									synchronized (attacks) {
										attacks.forEach((attackNumber, attack) -> {
											List<Partition> recoveredPartitions = attack.slavesPartitions(removedSlaves);
											System.out.println("Recovered partitions: " + recoveredPartitions);
											recoveredPartitions.forEach(partition -> {
												System.out.println("Available slaves: " + slaves.size());
												System.out.println("Distributed partition " + partition.toPartitions(slaves.size()));
												attack(attack.getCipherText(), attack.getKnownText(), attackNumber,
														partition.toPartitions(slaves.size()));
											});
											attack.removeSlaves(removedSlaves);
										});
									}
								}
							}
						}
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			};
			
		}.start();
	}

	@Override
	public void addSlave(Slave slave, String slaveName, UUID slaveKey) throws RemoteException {
		synchronized (slaves) {
//			Criacao e um escravo do tipo NamedSlave para evitar de usar Pair<A, B>
			NamedSlave namedSlave = new NamedSlave(slave, slaveName, slaveKey);

//			Caso a chave ja existe no mapa de escravos, indica que ele ja esta cadastrado, portanto
//			sua adicao significa que ele enviou um heartbeat
			if (slaves.containsKey(slaveKey)) {
				System.out.println("Heartbeat Received [" + slaveName + "]");
			} else {
//				Caso contrario, sera adicionado como mu novo escravo
				System.out.println("Registered [" + slaveName + "]");
				slaves.put(slaveKey, namedSlave);
			}
		}
	}

//	FIXME onde isso sera utilizado?
	@Override
	public void removeSlave(UUID slaveKey) throws RemoteException {
		synchronized (slaves) {
//			Remocao do escravo da lista de escravos do mestre
			slaves.remove(slaveKey);
		}
	}

	@Override
	public void foundGuess(UUID slaveKey, int attackNumber, long currentIndex, Guess guess)
			throws RemoteException {
//		Para um determinado ataque, apenas iremos adicinar um chute a sua lista de chutes
		attacks.get(attackNumber).addGuess(guess);
	}

	@Override
	public void checkpoint(UUID slaveKey, int attackNumber, long currentIndex) throws RemoteException {
//		Caso o escravo que enviou o checkpoint esteja na lista de tempo de resposta de escravos,
//		iremos alterar o valor de sua resposta para o tempo mais atual
		synchronized (slavesTimer) {
			if (!slavesTimer.containsKey(slaveKey)) {
				return;
			}
			
			Long currentTimeInMillis = Calendar.getInstance().getTimeInMillis();
			slavesTimer.put(slaveKey, currentTimeInMillis);
		}
		
//		Atualizacao do indica da particao em que um escravo esta processando
		attacks.get(attackNumber).updatePartition(slaveKey, (int) currentIndex);

		System.out.println("Checkpoint received [" + slaves.get(slaveKey).getName() + ", " + currentIndex + "]: "
				+ dictionaryReader.readLine((int) currentIndex));
	}

	@Override
	public Guess[] attack(byte[] cipherText, byte[] knownText) throws RemoteException {
//		Incializa uma classe de ataque com os dados de entrada do cliente
		Attack attack = new Attack(cipherText, knownText);
//		O numero de ataque eh gerado automaticamente pela classe Attack, logo basta recupera-lo
		Integer attackNumber = attack.getAttackNumber();
//		Adicionamos esse ataque ao mapa de ataques que estao sendo gerenciados pelo mestre
		attacks.put(attackNumber, attack);

		synchronized (slaves) {
//			Particionamento do dicionario de acordo com o numero de escravos ligados ao mestre
			List<Partition> partitions = new DictionaryReader(dictionaryReader).toPartitions(slaves.size());
//			Inicia um ataque
			attack(cipherText, knownText, attackNumber, partitions);
		}

//		Inicializacao da thread que serve para segurar o mestre ate que o ataque termine
		Thread attackThread = new Thread(attack);
		attackThread.start();
		
		try {
//			Aguarda a thread de ataque terminar..
			attackThread.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
//		INICIO DEBUG
		System.out.println("PARTICOES DE ATAQUE");
		attacks.forEach((n, a) -> {
			a.printPartitions();
		});
		
		System.out.println("\nESCRAVOS");
		slaves.forEach((n, a) -> {
			System.out.println("ID: " + n + ", Name: " + a.getName());
		});
		
		System.out.println("\nTIMER DE ESCRAVOS");
		slavesTimer.forEach((n, a) -> {
			System.out.println("ID: " + n + ", Time: " + a);
		});
//		FIM DEBUG
		
//		Terminado um ataque, podemos remover ele do mapa de ataques
		synchronized (attacks) {
			attacks.remove(attackNumber);	
		}
		
//		Converte a lista de guess para um array de guess
		Guess[] returnedGuesses = attack.guesses()
				.stream()
				.toArray(Guess[]::new);
		
		return returnedGuesses;
	}
	
	private void attack(byte[] cipherText, byte[] knownText, int attackNumber, List<Partition> partitions) {
//		Iteracao pelas particoes a serem distribuidas para os escravos
		Iterator<Partition> partitionsForSlaves = partitions.iterator();
//		Recuperacao do ataque de interesse
		Attack attack = attacks.get(attackNumber);

//		Garantimos que o numero de particoes eh igual ou menos ao numero de escravos
//		Logo podemos varrer a lista de escravos e entregar uma (ou nenhuma) particao para cada
		for (Entry<UUID, NamedSlave> entry : slaves.entrySet()) {
//			Se nao houver mais particoes, cortamos a distribuicao
			if (!partitionsForSlaves.hasNext()) {
				break;
			}
			
			UUID slaveKey = entry.getKey();
			Slave slave = entry.getValue().getSlave();
			
			Partition partition = partitionsForSlaves.next();
//			Inserimos na classe Attack que o escravo X esta responsavel pela particao P
			attack.addPartition(slaveKey, partition);

			try {
//				Tendo o um escravo e sua particao, o sub-ataque eh iniciado
				slave.startSubAttack(cipherText, knownText, partition.getStart(), partition.getEnd(), attackNumber,
						this);
			} catch (RemoteException e) {
//				Escravo com problemas, reescalonar sua tarefa?
//				Sua ausencia so sera notada no momento que o timer passar por ele
			}
			
//			Como esse escravo comecou um ataque, ele sera adicionado na lista de tempo de comunicacao dos escravos
			synchronized (slavesTimer) {
				slavesTimer.put(slaveKey, Calendar.getInstance().getTimeInMillis());
			}
		}
	}

}
