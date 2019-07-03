package br.ufes.inf.ppd.slave;

import java.util.Base64;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.JMSProducer;
import javax.jms.Message;
import javax.jms.TextMessage;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.sun.messaging.Queue;

import br.ufes.inf.ppd.Guess;
import br.ufes.inf.ppd.Slave;
import br.ufes.inf.ppd.utils.DictionaryReader;

public class SlaveImpl implements Slave {

//	Nome dado ao escravo quando instanciado
	private String name;
//	Dicionario original
	private DictionaryReader dictionary;
//	Fila de chutes para enviar informacoes ao mestre
	private Queue guessQueue;
//	Contexto utilizado para instanciar mensagens a serem enviadas pelo produtor
	private JMSContext context;
//	Produtor utilizado para enviar mensagens na fila de sub-ataques
	private JMSProducer producer;

	public SlaveImpl(String name, String dictionaryPath, Queue guessQueue, JMSContext context) {
		this.name = name;
		this.dictionary = new DictionaryReader(dictionaryPath);
		this.guessQueue = guessQueue;
		this.context = context;
		this.producer = context.createProducer();
	}

	/**
	 * A partir de uma copia do dicionario, o escravo ira varregar as palavras
	 * chaves dados indices inicial e final buscando pela chave que consiga
	 * descriptografar o cipherText
	 */
	@Override
	public void startSubAttack(byte[] cipherText, byte[] knownText, long initialWordIndex, long finalWordIndex,
			int attackNumber) {
		JSONArray guesses = new JSONArray();

//		Limita o range do dicionario que o escravo ira trabalhar
		dictionary.setRange((int) initialWordIndex, (int) finalWordIndex);
		dictionary.rewind();

		System.out.println("Attack [" + attackNumber + "]: Received Partition: <" + dictionary.getStart() + ", "
				+ dictionary.getEnd() + ">");

//		Enquanto houver alguma coisa para ler no dicionario...
		while (dictionary.ready()) {
			String key = dictionary.readLine();

			SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(), "Blowfish");

			try {
				Cipher cipher = Cipher.getInstance("Blowfish");
				cipher.init(Cipher.DECRYPT_MODE, secretKeySpec);

//				Caso nao caia no catch BadPaddingException, essa chave eh uma chave candidata
				byte[] decrypted = cipher.doFinal(cipherText);

				Guess guess = new Guess(key, decrypted);

				String decryptedStr = new String(decrypted);
				String knownTextStr = new String(knownText);

//				Caso a mensagem descriptografada contenha a palavra conhecida, pode-se dizer que esta chave
//				eh uma chave candidata, portanto sera enviada para o mestre como um chute.
				if (decryptedStr.contains(knownTextStr)) {
					guesses.put(guess.toJson());
				}
			} catch (BadPaddingException e) {
//				Chave errada, ignorar
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		System.out.println("Attack ["+ attackNumber + "]: Partition <" + dictionary.getStart() + ", "
				+ dictionary.getEnd() + "> has been terminated");
		
		try {
			JSONObject obj = new JSONObject();
			obj.put("slaveName", name);
			obj.put("initialWordIndex", dictionary.getStart());
			obj.put("finalWordIndex", dictionary.getEnd());
			obj.put("guesses", guesses);
	
			String jsonText = obj.toString();
			
			TextMessage message = context.createTextMessage();
			message.setIntProperty("attackNumber", attackNumber);
			message.setText(jsonText);
			
			producer.send(guessQueue, message);
		} catch (JMSException e) {
			e.printStackTrace();
		}
	}

	public void sentMessage(Message message) {
		if (message instanceof TextMessage) {
			TextMessage textMessage = (TextMessage) message;
			
			try {
				JSONObject obj = new JSONObject(textMessage.getText());
				
				int initialWordIndex = obj.getInt("initialWordIndex");
				int finalWordIndex = obj.getInt("finalWordIndex");
				byte[] knownText = obj.getString("knownText").getBytes();
				byte[] cipherText = Base64.getDecoder().decode(obj.getString("cipherText"));
				
				int attackNumber = textMessage.getIntProperty("attackNumber");
				
				startSubAttack(cipherText, knownText, initialWordIndex, finalWordIndex, attackNumber);			
			} catch(JSONException e) {
				e.printStackTrace();				
			} catch(JMSException e) {
				e.printStackTrace();
			}
		}
		
	}

}
