package br.inf.ufes.ppd.impl;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.rmi.RemoteException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import br.inf.ufes.ppd.Slave;
import br.inf.ufes.ppd.SlaveManager;
import br.inf.ufes.ppd.utils.DictionaryReader;

public class SlaveImpl implements Slave {

	private String name;
	
	public SlaveImpl(String name) {
		this.name = name;
	}
	
	@Override
	public void startSubAttack(byte[] cipherText, byte[] knownText, long initialWordIndex, long finalWordIndex,
			int attackNumber, SlaveManager callbackInterface) throws RemoteException {

		String testDictionaryPath = "test/dictionary.txt";
		
		try (DictionaryReader reader = new DictionaryReader(testDictionaryPath, initialWordIndex, finalWordIndex)) {

			while (reader.ready()) {
				String key = reader.readLine();
				
				SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(), "Blowfish");
		
				try {
					Cipher cipher = Cipher.getInstance("Blowfish");
					cipher.init(Cipher.DECRYPT_MODE, secretKeySpec);
		
					@SuppressWarnings("unused")
					byte[] decrypted = cipher.doFinal(cipherText);
					
					System.out.println("[" + (reader.getLineNumber() - 1) + "]" + key + ": Gotcha!!");
					
				} catch (BadPaddingException e) {
					// TODO Auto-generated catch block
					System.out.println("[" + (reader.getLineNumber() - 1) + "]" + key + ": Invalid Key!");
					
				} catch (Exception e) {
					e.printStackTrace();
				}
			
			}
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

}
