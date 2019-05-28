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

	/**
	 * A partir de uma copia do dicionario, o escravo ira varregar as palavras chaves dados indices inicial e final
	 * buscando pela chave que consiga descriptografar o cipherText
	 */
	@Override
	public void startSubAttack(byte[] cipherText, byte[] knownText, long initialWordIndex, long finalWordIndex,
			int attackNumber, SlaveManager callbackInterface) throws RemoteException {

		DictionaryReader dictionary = new DictionaryReader(baseDictionary);
		dictionary.setRange((int) initialWordIndex, (int) finalWordIndex);
		dictionary.rewind();

//		Inicializacao da thread que ira processar as informacoes e a passagem dos parametros necessarios
//		para tal processamento
		SlaveRunnable slaveRunnable = new SlaveRunnable(name, id);
		slaveRunnable.setDictionary(dictionary);
		slaveRunnable.setSubAttackParameters(cipherText, knownText, attackNumber, callbackInterface);
		
//		Tendo os dados em maos, pode iniciar a descriptografia
		Thread thread = new Thread(slaveRunnable);
		thread.start();
	}

}
