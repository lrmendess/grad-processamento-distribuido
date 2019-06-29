package br.ufes.inf.ppd;

/**
 * Slave.java
 */

import java.rmi.Remote;

import javax.jms.MessageListener;

public interface Slave extends Remote, MessageListener {

	/**
	 * Solicita a um escravo que inicie sua parte do ataque.
	 * 
	 * @param ciphertext       mensagem critografada
	 * @param knowntext        trecho conhecido da mensagem decriptografada
	 * @param initialwordindex indice inicial do trecho do dicionario a ser
	 *                         	considerado no sub-ataque.
	 * @param finalwordindex   indice final do trecho do dicionario a ser
	 *                         	considerado no sub-ataque.
	 * @param attackNumber     	chave que identifica o ataque
	 *
	 * @throws java.rmi.RemoteException
	 */
	public void startSubAttack(byte[] cipherText, byte[] knownText, long initialWordIndex, long finalWordIndex,
			int attackNumber);

}
