package br.ufes.inf.ppd.master;

import java.rmi.RemoteException;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.jms.JMSProducer;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.sun.messaging.Queue;

import br.ufes.inf.ppd.Guess;
import br.ufes.inf.ppd.Master;
import br.ufes.inf.ppd.utils.DictionaryReader;
import br.ufes.inf.ppd.utils.Partition;

public class MasterImpl implements Master, MessageListener {

//	Diretorio base
	private Set<Partition> dictionaryPartitions;
//	Mapa que indica quais particoes um escravo esta responsavel em um ataque
	private Map<Integer, Attack> attacks;
//	Fila de sub-ataques a serem depositados pelo mestre
	private Queue subAttacksQueue;
//	Contexto utilizado para instanciar mensagens a serem enviadas pelo produtor
	private JMSContext context;
//	Produtor utilizado para enviar mensagens na fila de sub-ataques
	private JMSProducer producer;

	/**
	 * Construtor do mestre, aqui tambem inicializamos uma thread para verificar quais escravos
	 * do sistema ainda estao vivos. Essa thread eh disparada a cada 5 segundos.
	 * 
	 * @param dictionaryPath
	 * @param subAttacksQueue 
	 */

	public MasterImpl(String dictionaryPath, int numberOfPartitions, Queue subAttacksQueue, JMSContext context) {
		DictionaryReader dictionaryReader = new DictionaryReader(dictionaryPath);
		
		this.dictionaryPartitions = dictionaryReader.toPartitions(numberOfPartitions);
		this.attacks = Collections.synchronizedMap(new HashMap<Integer, Attack>());
		this.subAttacksQueue = subAttacksQueue;
		this.context = context;
		this.producer = context.createProducer();
	}
	
	@Override
	public Guess[] attack(byte[] cipherText, byte[] knownText) throws RemoteException {
//		Incializa uma classe de ataque com os dados de entrada do cliente
		Attack attack = new Attack(cipherText, knownText);
		
//		O numero de ataque eh gerado automaticamente pela classe Attack, logo basta recupera-lo
		Integer attackNumber = attack.getAttackNumber();

		System.out.println("Attack [" + attackNumber + "]: Started");
		
//		Adicionamos esse ataque ao mapa de ataques que estao sendo gerenciados pelo mestre
		Set<Partition> dictionaryPartitionsCopy = new HashSet<Partition>(dictionaryPartitions);
		synchronized (attacks) {
			attacks.put(attackNumber, attack);	
			attack.setPartitions(dictionaryPartitionsCopy);
		}
		
		try {
			for(Partition partition : dictionaryPartitionsCopy) {
				JSONObject obj = new JSONObject();
				obj.put("initialWordIndex", partition.getStart());
				obj.put("finalWordIndex", partition.getEnd());
				obj.put("knownText", new String(knownText));
				obj.put("cipherText", new String(Base64.getEncoder().encode(cipherText)));

				String jsonText = obj.toString();
		    
				TextMessage message = context.createTextMessage();
				message.setIntProperty("attackNumber", attackNumber);
				message.setText(jsonText);
				
				producer.send(subAttacksQueue, message);
			}
		} catch(JSONException e) {
			e.printStackTrace();				
		} catch(JMSException e) {
			e.printStackTrace();
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
			System.out.println("Attack [" + attackNumber + "]: Ended");
		}
		
//		Converte a lista de guess para um array de guess
		Guess[] returnedGuesses = attack.getGuesses()
				.stream()
				.toArray(Guess[]::new);
		
		return returnedGuesses;
	}

	@Override
	public void onMessage(Message message) {
		if (message instanceof TextMessage) {
			TextMessage textMessage = (TextMessage) message;
			
			try {
				int attackNumber = textMessage.getIntProperty("attackNumber");
				Attack attack = attacks.get(attackNumber);
				
				JSONObject obj = new JSONObject(textMessage.getText());
				
				JSONArray guesses = obj.getJSONArray("guesses");
				for (int i = 0; i < guesses.length(); i++) {
					Guess guess = new Guess(guesses.getJSONObject(i));
					attack.addGuess(guess);
				}
				
				int start = obj.getInt("initialWordIndex");
				int end = obj.getInt("finalWordIndex");
				Partition partition = new Partition(start, end);
				
				String slaveName = obj.getString("slaveName");
				
				System.out.println("Attack [" + attackNumber + "]: Partition <" + start + ", " + end
						+ "> has been terminated by " + slaveName);
				
				attack.removePartition(partition);
				attack.notifyAttack();
			} catch (JMSException e) {
				e.printStackTrace();
			}
		}
	}
	
}
