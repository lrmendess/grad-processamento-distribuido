package br.inf.ufes.ppd.master;

import java.rmi.RemoteException;
import java.util.ArrayList;
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
		
//		Thread para procurar por escravos caidos e redistribuir suas particoes, atribuimos um valor alpha
//		de 2 segundos para cada verificacao na intencao de nao punir um escravo por atrasos tao facilmente
		Timer timer = new Timer();
		TimerTask timerTask = new TimerTask() {
			
			@Override
			public void run() {
				synchronized (slavesTimer) {
					Long currentTimeInMillis = System.nanoTime() / 1_000_000;
					
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
								.map(Entry::getKey)
								.collect(Collectors.toList());
						
//						Juncao dos escravos que irao ser removidos por nao cumprirem os requisitos de tempo
						Set<UUID> candidatesToBeRemoved = new HashSet<UUID>();
						candidatesToBeRemoved.addAll(candidatesToBeRemovedCheckpoint);
						candidatesToBeRemoved.addAll(candidatesToBeRemovedHeartbeat);
	
//						Remove os escravos candidatos a serem removidos para que nao prejudique a redivisao
//						das tarefas caso muitos escravos tenham caido ate que a thread timer tenha detectado
						candidatesToBeRemoved.forEach(uuid -> {
							System.out.println("Removed [" + slaves.get(uuid).getName() + "]");
							slaves.remove(uuid);
						});
						
						candidatesToBeRemoved.forEach(uuid -> {
							try {
								removeSlave(uuid);
							} catch (RemoteException e) {
								System.err.println("I died");
								e.printStackTrace();
							}
						});
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
			
			Long currentTimeInMillis = System.nanoTime() / 1_000_000;
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
				boolean isPresentInSlaves = slaves.containsKey(slaveKey);
//				Remove um escravo que foi finalizado atraves de um SIGINT
				if (isPresentInSlaves) {
					System.out.println("Removed [" + slaves.get(slaveKey).getName() + "]");
					slaves.remove(slaveKey);
				}
			
//				Agora vamos reescalonar as particoes do escravo removido para outros escravos
				synchronized (attacks) {
//					Para cada ataque, vamos recuperar as particoes que estao pendentes
					attacks.forEach((attackNumber, attack) -> {
//						Se o ataque foi forcado a terminar, nao ha nada que possa ser feito
						if (attack.wasForcedToTerminate()) {
							return;
						}
						
						byte[] cipherText = attack.getCipherText();
						byte[] knownText = attack.getKnownText();
						
//						Recuperamos todas as particoes do escravo caido
						Set<Partition> recoveredPartitions = attack.getPartitions(slaveKey);
						
//						Se as particoes recuperadas retornarem nulo, significa que esse ataque foi forcado
//						a terminar, logo todo o seu conteudo foi excluido
						if (recoveredPartitions == null) {
							return;
						}
						
//						Para cada particao recuperada, vamos quebra-la e distribuir para os escravos atacarem
						for (Partition partition : recoveredPartitions) {
							List<Partition> redistributedPartition = partition.shatter(slaves.size());
							attack(cipherText, knownText, attackNumber, redistributedPartition);
						}
										
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
		synchronized (attacks) {
			attacks.get(attackNumber).addGuess(guess);
		}
		
//		Impressao do chute recebido contendo nome do escravo, numero do ataque, indice lido e a palavra
//		do indice lido
		System.out.println("Guess received [Name=" + slaves.get(slaveKey).getName() + ", AttackNumber="
				+ attackNumber + ", CurrentIndex=" + currentIndex + "]: "
				+ dictionaryReader.readLine((int) currentIndex));
	}

	@Override
	public void checkpoint(UUID slaveKey, int attackNumber, long currentIndex) throws RemoteException {
		Long currentTimeInMillis = System.nanoTime() / 1_000_000;
//		Caso o escravo que enviou o checkpoint esteja na lista de tempo de resposta de escravos,
//		iremos alterar o valor de sua resposta para o tempo mais atual
		synchronized (slavesTimer) {
//			Atualizacao do indica da particao em que um escravo esta processando
			Attack attack = attacks.get(attackNumber);
			attack.updatePartition(slaveKey, (int) currentIndex);
			attack.notifyAttack();

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

//			Impressao do checkpoint recebido contendo nome do escravo, numero do ataque, indice lido, e o tempo
//			passo desde o ultimo checkpoint
			System.out.println("Checkpoint received [Name=" + slaves.get(slaveKey).getName() + ", AttackNumber="
					+ attackNumber + ", CurrentIndex=" + currentIndex + "]: " + responseIntervalInMillis + "ms");
		}
	}

	@Override
	public Guess[] attack(byte[] cipherText, byte[] knownText) throws RemoteException {
//		Incializa uma classe de ataque com os dados de entrada do cliente
		Attack attack = new Attack(cipherText, knownText);
		
//		O numero de ataque eh gerado automaticamente pela classe Attack, logo basta recupera-lo
		Integer attackNumber = attack.getAttackNumber();

		System.out.println("The attack [" + attackNumber + "] started");
		
//		Adicionamos esse ataque ao mapa de ataques que estao sendo gerenciados pelo mestre
		synchronized (attacks) {
			attacks.put(attackNumber, attack);				
		}
		
		synchronized (slaves) {
//			Sem escravos, sem ataque
			if (slaves.size() == 0) {
				System.out.println("The attack [" + attackNumber + "] ended with error");
				return failure();
			}
			
			List<Partition> partitions  = new DictionaryReader(dictionaryReader).toPartitions(slaves.size());
			
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
//			Se o ataque foi forcado a terminar depois de ter comecado (slaves.size() == 0),
//			adicionamos uma mensagem de erro
			if (attack.wasForcedToTerminate()) {
				attack.guesses().add(failure()[0]);
				System.out.println("The attack [" + attackNumber + "] ended with error");
			} else {
				System.out.println("The attack [" + attackNumber + "] ended without error");
			}
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
		synchronized (slavesTimer) {
			synchronized (slaves) {
				Attack attack = attacks.get(attackNumber);
		
//				Se nao ha mais escravos, o sistema nao pode atender mais ninguem e, caso as particoes estejam
//				vazias, significa que nao foi possivel dividi-las entre os escravos
				if (slaves.size() == 0 || partitions.isEmpty()) {
					attack.forcedTermination();
					return;
				}
		
//				Iteracao pelas particoes a serem distribuidas para os escravos
				Iterator<Partition> partitionsForSlaves = partitions.iterator();
		
				List<Entry<UUID, NamedSlave>> slavesList = new ArrayList<>(slaves.entrySet());

//				Vamos varrer a lista de escravos e entregar uma (ou mais) particoes para cada escravo
//				Com a finalidade de evitar eventuais particoes pendentes, fazemos uma iteracao circular
//				pela lista de escravos ate que as particoes terminem.
				int i = 0;
				while (partitionsForSlaves.hasNext()) {
//					Idem ao primeiro comentario do metodo
					if (slaves.size() == 0 || partitions.isEmpty()) {
						attack.forcedTermination();
						return;
					}
					
					Entry<UUID, NamedSlave> entry = slavesList.get(i++ % slaves.size());

					UUID slaveKey = entry.getKey();
					Slave slave = entry.getValue().getSlave();

					Partition partition = partitionsForSlaves.next();
//					Inserimos na classe Attack que o escravo X esta responsavel pela particao P
					attack.addPartition(slaveKey, partition);

					try {
//						Tendo o um escravo e sua particao, o sub-ataque eh iniciado
						slave.startSubAttack(cipherText, knownText, partition.getStart(), partition.getEnd(),
								attackNumber, this);

//						Um escravo que ate entao nao tinha comecado nenhum ataque, sera registrado no temporizados
//						de checkpoint dos escravos, mas caso ele ja estava no sistema, nao faremos nada.
						boolean isPresent = slavesTimer.containsKey(slaveKey);

						if (!isPresent) {
							slavesTimer.put(slaveKey, System.nanoTime() / 1_000_000);
						}
						
					} catch (RemoteException e) {
//						Escravo com problemas, reescalonamento forcado!
						try {
							removeSlave(slaveKey);
						} catch (RemoteException e1) {
							System.err.println("I died");
							e1.printStackTrace();
						}
					}
				}
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
				.anyMatch(entry -> entry.getValue().hasSlave(slaveKey));
		
		return busySlave;
	}
	
	/**
	 * Guess exclusivo para erros
	 * 
	 * @return
	 */
	private Guess[] failure() {
		String errorMessage = "Unfortunately no more slaves are available. Sit down and cry!";
		
		Guess guess = new Guess();
		guess.setKey("NoSlavesAvailableError");
		guess.setMessage(errorMessage.getBytes());
		
		return new Guess[] { guess };
	}
	
}
