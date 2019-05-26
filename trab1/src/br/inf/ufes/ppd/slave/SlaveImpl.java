package br.inf.ufes.ppd.slave;

import java.rmi.RemoteException;
import java.util.UUID;

import br.inf.ufes.ppd.Slave;
import br.inf.ufes.ppd.SlaveManager;
import br.inf.ufes.ppd.utils.DictionaryReader;

public class SlaveImpl implements Slave {

	private final String name;
	private final UUID id;

	private DictionaryReader baseDictionary;

	public SlaveImpl(String name, UUID id, String dictionaryPath) {
		this.name = name;
		this.id = id;
		this.baseDictionary = new DictionaryReader(dictionaryPath);
	}

	@Override
	public void startSubAttack(byte[] cipherText, byte[] knownText, long initialWordIndex, long finalWordIndex,
			int attackNumber, SlaveManager callbackInterface) throws RemoteException {

		DictionaryReader dictionary = new DictionaryReader(baseDictionary);

//		Abertura do utilitario de leitura de dicionario com fechamento automatico
		dictionary.setRange((int) initialWordIndex, (int) finalWordIndex);
		dictionary.rewind();

		SlaveRunnable slaveRunnable = new SlaveRunnable(name, id);
		slaveRunnable.setDictionary(dictionary);
		slaveRunnable.setSubAttackParameters(cipherText, knownText, attackNumber, callbackInterface);
		
		Thread thread = new Thread(slaveRunnable);
		thread.start();
	}

}
