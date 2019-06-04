package br.inf.ufes.ppd.slave;

import java.rmi.RemoteException;
import java.util.TimerTask;
import java.util.UUID;

import br.inf.ufes.ppd.SlaveManager;

public class SlaveCheckpointAssistant extends TimerTask {

	@SuppressWarnings("unused")
	private String slaveName;
	private UUID slaveId;

	private int attackNumber;
	private SlaveManager callbackInterface;

	private int currentIndex;

	public SlaveCheckpointAssistant(String slaveName, UUID slaveId, int attackNumber, SlaveManager callbackInterface) {
		this.slaveName = slaveName;
		this.slaveId = slaveId;
		this.attackNumber = attackNumber;
		this.callbackInterface = callbackInterface;
		this.currentIndex = 0;
	}

	/**
	 * Atualiza o index que o escravo esta lendo
	 * 
	 * @param currentIndex
	 */
	public void setCurrentIndex(int currentIndex) {
		this.currentIndex = currentIndex;
	}

	/**
	 * A cada 10 segundos, o escravo envia um checkpoint para o mestre
	 */
	@Override
	public void run() {
		try {
//			Envia o checkpoint para o mestre via interface SlaveManager
			System.out.println("Checkpoint sent <" + currentIndex + ">");
			callbackInterface.checkpoint(slaveId, attackNumber, currentIndex);
		} catch (RemoteException e) {
//			Houve algum problema com o mestre durante o ataque, logo iremos finalizar o processo.
//			Nas especificacoes nao ha exigencias quanto ao tratamento desse problema
			System.err.println("The master fell during the attack (SlaveCheckpointAssistant)");
		}
	}
	
}
