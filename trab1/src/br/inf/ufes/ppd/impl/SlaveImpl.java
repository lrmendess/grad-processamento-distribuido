package br.inf.ufes.ppd.impl;

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

import br.inf.ufes.ppd.Slave;
import br.inf.ufes.ppd.SlaveManager;
import br.inf.ufes.ppd.utils.DictionaryReader;

public class SlaveImpl implements Slave {

	private final UUID id;
	private final String name;
	private final String dictionaryPath;

	public SlaveImpl(UUID id, String name, String dictionaryPath) {
		this.id = id;
		this.name = name;
		this.dictionaryPath = dictionaryPath;
	}
	
	@Override
	public void startSubAttack(byte[] cipherText, byte[] knownText, long initialWordIndex, long finalWordIndex,
			int attackNumber, SlaveManager callbackInterface) throws RemoteException {

//		Abertura do utilitario de leitura de dicionario com fechamento automatico
		try (DictionaryReader reader = new DictionaryReader(dictionaryPath, initialWordIndex, finalWordIndex)) {

//			Enquanto houver alguma coisa para ler no dicionario...
			while (reader.ready()) {
				String key = reader.readLine();

				SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(), "Blowfish");

				try {
					Cipher cipher = Cipher.getInstance("Blowfish");
					cipher.init(Cipher.DECRYPT_MODE, secretKeySpec);

					@SuppressWarnings("unused")
					byte[] decrypted = cipher.doFinal(cipherText);

					notification("[" + (reader.getLineNumber() - 1) + "]" + key + ": " + "Gotcha!!");

				} catch (BadPaddingException e) {
//					Chave errada
					notification("[" + (reader.getLineNumber() - 1) + "]" + key + ": " + "Invalid Key");

				} catch (InvalidKeyException | IllegalBlockSizeException e) {
//					Chave mal formatada
					e.printStackTrace();

				} catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
//					Se rolarem essas excecoes, a Terra ja estara colidindo com o Sol
					e.printStackTrace();
				}
			}

		} catch (FileNotFoundException e) {
//			O dicionario nao foi encontrado localmente
			e.printStackTrace();

		} catch (IOException e) {
//			Houve alguma falha na leitura
			e.printStackTrace();
		}
	}
	
//	Funcao de debug (deve se removida futuramente)
	private void notification(String msg) {
		System.out.println("Slave[name=" + name + ", id=" + id + "]: " + msg);
	}
	
}
