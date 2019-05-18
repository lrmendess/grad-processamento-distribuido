package br.inf.ufes.ppd.slave;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.rmi.RemoteException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

import br.inf.ufes.ppd.Guess;
import br.inf.ufes.ppd.Slave;
import br.inf.ufes.ppd.SlaveManager;
import br.inf.ufes.ppd.utils.DictionaryReader;

public class SlaveImpl implements Slave {

	private final String name;
	private final UUID id;
	
	private DictionaryReader dictionary;

	public SlaveImpl(String name, UUID id, String dictionaryPath) {
		this.name = name;
		this.id = id;
		
		try {
			dictionary = new DictionaryReader(dictionaryPath);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public synchronized void startSubAttack(byte[] cipherText, byte[] knownText, long initialWordIndex, long finalWordIndex,
			int attackNumber, SlaveManager callbackInterface) throws RemoteException {

//		Abertura do utilitario de leitura de dicionario com fechamento automatico
		int start = (int) initialWordIndex;
		int end = (int) finalWordIndex;
		dictionary.setRange(start, end);
		dictionary.rewind();
		
		SlaveCheckpointAssistant checkPointAssistant = new SlaveCheckpointAssistant(name, id, attackNumber,
				callbackInterface);
		
		Thread checkPointAssistantThread = new Thread(checkPointAssistant);
		checkPointAssistantThread.start();
		
//		Enquanto houver alguma coisa para ler no dicionario...
		while (dictionary.ready()) {
			String key = dictionary.readLine();

			SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(), "Blowfish");

			try {
				Cipher cipher = Cipher.getInstance("Blowfish");
				cipher.init(Cipher.DECRYPT_MODE, secretKeySpec);

				byte[] decrypted = cipher.doFinal(cipherText);

				notification("[" + dictionary.getLineNumber() + ", " + key + "]: Gotcha!!");
				
				Guess guess = new Guess();
				guess.setKey(key);
				guess.setMessage(decrypted);

				String decryptedStr = new String(decrypted);
				String knownTextStr = new String(knownText);
				
				if (decryptedStr.contains(knownTextStr)) {
					callbackInterface.foundGuess(id, attackNumber, dictionary.getLineNumber() - 1, guess);
				}
			} catch (BadPaddingException e) {
//				Chave errada
//				notification("[" + dictionary.getLineNumber() + ", " + key + "]: Invalid Key");
				continue;
			} catch (InvalidKeyException | IllegalBlockSizeException e) {
//				Chave mal formatada ou .cipher nao multiplo de 8
				e.printStackTrace();
			} catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
//				Se rolarem essas excecoes, a Terra ja estara colidindo com o Sol
				e.printStackTrace();
			}

			checkPointAssistant.setCurrentIndex(dictionary.getLineNumber() - 1);
		}
		
		checkPointAssistant.workFinished();
	}
	
//	Funcao de debug (deve se removida futuramente)
	private void notification(String msg) {
		System.out.println("Slave[name=" + name + ", id=" + id + "]: " + msg);
	}
	
}
