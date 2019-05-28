package br.inf.ufes.ppd.master;

import java.rmi.RemoteException;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
//					Preenche uma lista com ids de escravos que nao respondem ha mais de 20s (+alpha)
					List<UUID> candidateToBeRemoved = slavesTimer.entrySet()
							.stream()
							.filter(entry -> (System.currentTimeMillis() - entry.getValue()) > 21000)
							.map(Entry::getKey)
							.collect(Collectors.toList());
					
//					Removemos da lista de tempo o uuid dos escravos que nao estao mais em operacao
					candidateToBeRemoved.stream().forEach(uuid -> slavesTimer.remove(uuid));
					
//					Se o escravo nao responde a mais de 20 segundos e esta ocupado em algum ataque, ele caiu
					List<UUID> removedSlaves = candidateToBeRemoved
							.stream()
							.filter(uuid -> slaveIsBusy(uuid))
							.collect(Collectors.toList());
					
					synchronized (slaves) {
//						Remocao da lista de escravos
						removedSlaves.forEach(uuid -> slaves.remove(uuid));
//						Agora vamos reescalonar as particoes dos escravos removidos para outros escravos
						synchronized (attacks) {
//							Para cada ataque, vamos recuperar as particoes que estao pendentes
							attacks.forEach((attackNumber, attack) -> {
								byte[] cipherText = attack.getCipherText();
								byte[] knownText = attack.getKnownText();
								
//								Recuperamos todas as particoes dos escravos caidos
								List<Partition> recoveredPartitions = attack.slavesPartitions(removedSlaves);
								
//								Para cada particao recuperada, vamos quebra-la e distribuir para os escravos atacarem
								recoveredPartitions.forEach(partition ->  {
									List<Partition> redistributedPartition = partition.shatter(slaves.size());
									attack(cipherText, knownText, attackNumber, redistributedPartition);
								});
												
//								Removemos os escravos dos ataques que eles estavam participando antes de cair
								attack.removeSlaves(removedSlaves);
							});
						}
					}
				}
			};
		};
		timer.schedule(timerTask, 5000, 5000);
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

//		Impressao do chute recebido contendo nome do escravo, numero do ataque e indice lido
		System.out.println("Callback received [" + slaves.get(slaveKey).getName() + ", " + attackNumber + ", "
				+ currentIndex + "]: " + dictionaryReader.readLine((int) currentIndex));
	}

	@Override
	public void checkpoint(UUID slaveKey, int attackNumber, long currentIndex) throws RemoteException {
//		Atualizacao do indica da particao em que um escravo esta processando
		attacks.get(attackNumber).updatePartition(slaveKey, (int) currentIndex);
		
//		Caso o escravo que enviou o checkpoint esteja na lista de tempo de resposta de escravos,
//		iremos alterar o valor de sua resposta para o tempo mais atual
		synchronized (slavesTimer) {
//			Se depois da atualizacao da particao o escravo nao estiver em nenhum ataque, significa que ele
//			terminou todas as suas tarefas e pode ser removido do timer
			boolean busySlave = slaveIsBusy(slaveKey);
			
			if (busySlave) {
				slavesTimer.put(slaveKey, System.currentTimeMillis());
			} else {
				slavesTimer.remove(slaveKey);
			}
		}

//		Impressao do checkpoint recebido contendo nome do escravo, numero do ataque e indice lido
		System.out.println("Checkpoint received [" + slaves.get(slaveKey).getName() + ", " + attackNumber + ", "
				+ currentIndex + "]: " + dictionaryReader.readLine((int) currentIndex));
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
//				Escravo com problemas, reescalonar sua tarefa??
//				Sua ausencia so sera notada no momento que o timer passar por ele
			}
			
//			Um escravo que ate entao nao tinha comecado nenhum ataque, sera registrado no temporizados
//			de checkpoint dos escravos, mas caso ele ja estava no sistema, nao faremos nada.
			synchronized (slavesTimer) {
				if (!slavesTimer.containsKey(slaveKey)) {
					slavesTimer.put(slaveKey, Calendar.getInstance().getTimeInMillis());
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
				.anyMatch(entry -> {
					return entry.getValue().hasSlave(slaveKey);
				});
		
		return busySlave;
	}
	
}
