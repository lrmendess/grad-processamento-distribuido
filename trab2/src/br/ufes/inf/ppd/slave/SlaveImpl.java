package br.ufes.inf.ppd.slave;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.jms.Message;
import javax.jms.MessageListener;

import com.sun.messaging.Queue;

import br.ufes.inf.ppd.Guess;
import br.ufes.inf.ppd.Slave;
import br.ufes.inf.ppd.utils.DictionaryReader;

public class SlaveImpl implements Slave, MessageListener {

	@SuppressWarnings("unused")
	private String name;
	private DictionaryReader dictionary;
	private Queue guessQueue;

	public SlaveImpl(String name, String dictionaryPath, Queue guessQueue) {
		this.name = name;
		this.dictionary = new DictionaryReader(dictionaryPath);
		this.guessQueue = guessQueue;
	}

	/**
	 * A partir de uma copia do dicionario, o escravo ira varregar as palavras
	 * chaves dados indices inicial e final buscando pela chave que consiga
	 * descriptografar o cipherText
	 */
	@Override
	public void startSubAttack(byte[] cipherText, byte[] knownText, long initialWordIndex, long finalWordIndex,
			int attackNumber) {

//		Limita o range do dicionario que o escravo ira trabalhar
		dictionary.setRange((int) initialWordIndex, (int) finalWordIndex);
		dictionary.rewind();

		System.out.println("Received Partition: <" + dictionary.getStart() + ", " + dictionary.getEnd() + ">");

//		Enquanto houver alguma coisa para ler no dicionario...
		while (dictionary.ready()) {
			String key = dictionary.readLine();

			SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(), "Blowfish");

			try {
				Cipher cipher = Cipher.getInstance("Blowfish");
				cipher.init(Cipher.DECRYPT_MODE, secretKeySpec);

//				Caso nao caia no catch BadPaddingException, essa chave eh uma chave candidata
				byte[] decrypted = cipher.doFinal(cipherText);

				@SuppressWarnings("unused")
				Guess guess = new Guess(key, decrypted);

				String decryptedStr = new String(decrypted);
				String knownTextStr = new String(knownText);

//				Caso a mensagem descriptografada contenha a palavra conhecida, pode-se dizer que esta chave
//				eh uma chave candidata, portanto sera enviada para o mestre como um chute.
				if (decryptedStr.contains(knownTextStr)) {
					System.out.println(key);
//					TODO salvar o chute numa lista para ser retornada assim que a particao for concluida
				}
			} catch (BadPaddingException e) {
//				Chave errada, ignorar
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		System.out.println("Partition has been terminated <" + dictionary.getStart() + ", "
				+ dictionary.getEnd() + ">");
		
//		TODO enviar os chutes e o numero do ataque para a lista de chutes
//		TODO limpar a lista local de chutes
		
	}

	@Override
	public void onMessage(Message message) {
		// TODO Auto-generated method stub
		
	}

}
