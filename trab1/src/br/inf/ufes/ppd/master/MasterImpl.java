package br.inf.ufes.ppd.master;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.stream.Collectors;

import br.inf.ufes.ppd.Guess;
import br.inf.ufes.ppd.Master;
import br.inf.ufes.ppd.Slave;
import br.inf.ufes.ppd.slave.NamedSlave;
import br.inf.ufes.ppd.utils.DictionaryReader;
import br.inf.ufes.ppd.utils.Partition;

public class MasterImpl implements Master {

//	Diretorio base
	private DictionaryReader dictionaryReader;
//	Lista de escravos mapeadas pelo ID do escravo
	private Map<UUID, NamedSlave> slaves;
//	Mapa que indica quais particoes um escravo esta responsavel em um ataque
	private Map<Integer, Attack> attacks;
//	Lista de tempo de comunicacao dos escravos
	private Map<UUID, Long> slavesTimer;

	/**
	 * Construtor do mestre, aqui tambem inicializamos uma thread para verificar quais escravos
	 * do sistema ainda estao vivos. Essa thread eh disparada a cada 5 segundos.
	 * 
	 * @param dictionaryPath
	 */
	public MasterImpl(String dictionaryPath) {
		this.dictionaryReader = new DictionaryReader(dictionaryPath);
		this.slaves = Collections.synchronizedMap(new HashMap<>());
		this.attacks = Collections.synchronizedMap(new HashMap<>());
		this.slavesTimer = Collections.synchronizedMap(new HashMap<>());
		
//		Thread para procurar por escravos caidos e redistribuir suas particoes
		Timer timer = new Timer();
		TimerTask timerTask = new TimerTask() {
			
			@Override
			public void run() {
				synchronized (slavesTimer) {
					Long currentTimeInMillis = System.currentTimeMillis();
					
//					Preenche uma lista com ids de escravos que nao respondem ha mais de 20s (checkpoint)
					Set<UUID> candidatesToBeRemovedCheckpoint = slavesTimer.entrySet()
							.stream()
							.filter(entry -> (currentTimeInMillis - entry.getValue()) > 22000)
							.map(Entry::getKey)
							.collect(Collectors.toSet());
					
					synchronized (slaves) {
//						Preenche uma lista com ids de escravos que nao respondem ha mais de 30s (hearbeat)
						List<UUID> candidatesToBeRemovedHeartbeat = slaves.entrySet()
								.stream()
								.filter(entry -> (currentTimeInMillis - entry.getValue().getHeartbeatTime()) > 32000)
								.map(Entry::getKey).collect(Collectors.toList());
						
//						Juncao dos escravos que irao ser removidos por nao cumprirem os requisitos de tempo
						Set<UUID> candidatesToBeRemoved = new HashSet<UUID>();
						candidatesToBeRemoved.addAll(candidatesToBeRemovedCheckpoint);
						candidatesToBeRemoved.addAll(candidatesToBeRemovedHeartbeat);
	
						for (UUID uuid : candidatesToBeRemoved) {
							try {
								removeSlave(uuid);
							} catch (RemoteException e) {
								System.err.println("ME DERRUBARAM AQUI OW!");
								e.printStackTrace();
							}
						}
					}
				}
			};
		};
		timer.schedule(timerTask, 0, 5000);
	}

	@Override
	public synchronized void addSlave(Slave slave, String slaveName, UUID slaveKey) throws RemoteException {
		synchronized (slaves) {
//			Caso a chave ja exista no mapa de escravos, indica que ele ja esta cadastrado, portanto
//			sua adicao significa que ele enviou um heartbeat
			boolean isPresent = slaves.containsKey(slaveKey);
			
			if (!isPresent) {
				NamedSlave newSlave = new NamedSlave(slave, slaveName, slaveKey);
				slaves.put(slaveKey, newSlave);
				System.out.println("Registered [" + slaveName + "]");
			} else {
				System.out.println("Heartbeat Received [" + slaveName + "]");
			}
			
			NamedSlave namedSlave = slaves.get(slaveKey);
			
			Long currentTimeInMillis = System.currentTimeMillis();
			namedSlave.setHeartbeatTime(currentTimeInMillis);
		}
	}

	@Override
	public synchronized void removeSlave(UUID slaveKey) throws RemoteException {
		synchronized (slavesTimer) {
//			Removemos da lista de tempo o uuid do escravo que nao esta mais em operacao
			boolean isPresentInTimer = slavesTimer.containsKey(slaveKey);
			
			if (isPresentInTimer) {
				slavesTimer.remove(slaveKey);
			}
		
			synchronized (slaves) {
//				Remove o escravo que nao esta mais ativo
				System.out.println("Removed [" + slaves.get(slaveKey).getName() + "]");
				boolean isPresentInSlaves = slaves.containsKey(slaveKey);
				
				if (isPresentInSlaves) {
					slaves.remove(slaveKey);
				}
			
//				Agora vamos reescalonar as particoes do escravo removido para outros escravos
				synchronized (attacks) {
//					Para cada ataque, vamos recuperar as particoes que estao pendentes
					attacks.forEach((attackNumber, attack) -> {
						byte[] cipherText = attack.getCipherText();
						byte[] knownText = attack.getKnownText();
						
//						Recuperamos todas as particoes do escravo caido
						Set<Partition> recoveredPartitions = attack.getPartitions(slaveKey);
						
//						Para cada particao recuperada, vamos quebra-la e distribuir para os escravos atacarem
						recoveredPartitions.forEach(partition ->  {
							List<Partition> redistributedPartition = partition.shatter(slaves.size());
							attack(cipherText, knownText, attackNumber, redistributedPartition);
						});
										
//						Removemos o escravo dos ataques que ele estava participando antes de cair
						attack.removeSlave(slaveKey);
					});
				}
			}
		}
	}

	@Override
	public void foundGuess(UUID slaveKey, int attackNumber, long currentIndex, Guess guess)
			throws RemoteException {
//		Para um determinado ataque, apenas iremos adicinar um chute a sua lista de chutes
		attacks.get(attackNumber).addGuess(guess);

//		Impressao do chute recebido contendo nome do escravo, numero do ataque e indice lido
		System.out.println("Callback received [" + slaves.get(slaveKey).getName() + ", " + attackNumber + ", "
				+ currentIndex + "]: " + dictionaryReader.readLine((int) currentIndex));
	}

	@Override
	public void checkpoint(UUID slaveKey, int attackNumber, long currentIndex) throws RemoteException {
		Long currentTimeInMillis = System.currentTimeMillis();
		
//		Caso o escravo que enviou o checkpoint esteja na lista de tempo de resposta de escravos,
//		iremos alterar o valor de sua resposta para o tempo mais atual
		synchronized (slavesTimer) {
//			Atualizacao do indica da particao em que um escravo esta processando
			attacks.get(attackNumber).updatePartition(slaveKey, (int) currentIndex);
			
//			Evita que algum atrasadinho (+20s) atualize seu tempo, ele sera removido pela timer thread
			boolean isPresent = slavesTimer.containsKey(slaveKey);
			
			if (!isPresent) {
				return;
			}
			
			Long oldTimeInMillis = slavesTimer.get(slaveKey);
			
//			Se depois da atualizacao da particao o escravo nao estiver em nenhum ataque, significa que ele
//			terminou todas as suas tarefas e pode ser removido do timer
			boolean busySlave = slaveIsBusy(slaveKey);
			
			if (busySlave) {
				slavesTimer.put(slaveKey, currentTimeInMillis);
			} else {
				slavesTimer.remove(slaveKey);
			}

			Long responseIntervalInMillis = currentTimeInMillis - oldTimeInMillis;

//			Impressao do checkpoint recebido contendo nome do escravo, numero do ataque e indice lido
			System.out.println("Checkpoint received [" + slaves.get(slaveKey).getName() + ", " + attackNumber + ", "
					+ currentIndex + "]: " + dictionaryReader.readLine((int) currentIndex) + ", "
					+ responseIntervalInMillis + "ms");
		}
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
			if (slaves.size() == 0) {
				synchronized (attacks) {
					attacks.remove(attackNumber);
				}
				return null;
			}
			
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
	
	/**
	 * Funcao para distribuir uma lista de particoes para os escravos registrados no mestre.
	 * Espera-se que a quantidade de particoes seja igual a quantidade de escravos no sistema.
	 * 
	 * @param cipherText
	 * @param knownText
	 * @param attackNumber
	 * @param partitions
	 */
	private void attack(byte[] cipherText, byte[] knownText, int attackNumber, List<Partition> partitions) {
//		Iteracao pelas particoes a serem distribuidas para os escravos
		Iterator<Partition> partitionsForSlaves = partitions.iterator();
//		Recuperacao do ataque de interesse
		Attack attack = attacks.get(attackNumber);
		
		List<Entry<UUID, NamedSlave>> slavesList = new ArrayList<>(slaves.entrySet());

//		Vamos varrer a lista de escravos e entregar uma (ou mais) particoes para cada escravo
//		Com a finalidade de evitar eventuais particoes pendentes, fazemos uma iteracao circular
//		pela lista de escravos ate que as particoes terminem.
		int i = 0;
		while (partitionsForSlaves.hasNext()) {
			if (slaves.size() == 0) {
				synchronized (attacks) {
					attacks.remove(attackNumber);
				}
				return;
			}
			
			Entry<UUID, NamedSlave> entry = slavesList.get(i++ % slaves.size());
			
			UUID slaveKey = entry.getKey();
			Slave slave = entry.getValue().getSlave();
			
			Partition partition = partitionsForSlaves.next();
//			Inserimos na classe Attack que o escravo X esta responsavel pela particao P
			attack.addPartition(slaveKey, partition);

			try {
//				Tendo o um escravo e sua particao, o sub-ataque eh iniciado
				slave.startSubAttack(cipherText, knownText, partition.getStart(), partition.getEnd(), attackNumber,
						this);

//				Um escravo que ate entao nao tinha comecado nenhum ataque, sera registrado no temporizados
//				de checkpoint dos escravos, mas caso ele ja estava no sistema, nao faremos nada.
				synchronized (slavesTimer) {
					boolean isPresent = slavesTimer.containsKey(slaveKey);
					
					if (!isPresent) {
						slavesTimer.put(slaveKey, Calendar.getInstance().getTimeInMillis());
					}
				}
			} catch (RemoteException e) {
//				Escravo com problemas, reescalonamento forcado!
//				Pode entrar num loop infinito caso nao tenham mais escravos
				try {
					removeSlave(slaveKey);
				} catch (RemoteException e1) {
					System.err.println("Dei uma leve falecida");
					e1.printStackTrace();
				}
				attack(cipherText, knownText, attackNumber, partitions);
			}
		}
	}
	
	/**
	 * Verifica se um dado escravo (apenas id) esta ocupado em pelo menos um ataque.
	 * Este metodo nao garante que um escravo esteja vivo, mas sim se ele esta alocado em algum ataques
	 * 
	 * @param slaveKey
	 * @return
	 */
	private boolean slaveIsBusy(UUID slaveKey) {
		boolean busySlave = attacks.entrySet()
				.stream()
				.anyMatch(entry -> {
					return entry.getValue().hasSlave(slaveKey);
				});
		
		return busySlave;
	}
	
}
