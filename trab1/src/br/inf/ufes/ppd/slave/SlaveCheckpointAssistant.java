package br.inf.ufes.ppd.slave;

import java.rmi.RemoteException;
import java.util.UUID;

import br.inf.ufes.ppd.SlaveManager;

public class SlaveCheckpointAssistant implements Runnable {

	private String slaveName;
	private UUID slaveId;

	private int attackNumber;
	private SlaveManager callbackInterface;

	private int currentIndex;
	private volatile boolean workFinished = false;

	public SlaveCheckpointAssistant(String slaveName, UUID slaveId, int attackNumber, SlaveManager callbackInterface) {
		this.slaveName = slaveName;
		this.slaveId = slaveId;
		this.attackNumber = attackNumber;
		this.callbackInterface = callbackInterface;
		this.currentIndex = -1; // Nenhuma palavra foi testado ainda
	}

	public void setCurrentIndex(int currentIndex) {
		this.currentIndex = currentIndex;
	}

	public void workFinished() {
		workFinished = true;

		try {
			notification("Partitions done!");
//			Acresce UM no ultimo index para que ele saia da borda da particao e o Master
//			perceba que o escravo terminou de testa-la.
			synchronized (callbackInterface) {
				callbackInterface.checkpoint(slaveId, attackNumber, currentIndex + 1);
			}
		} catch (RemoteException e) {
//			TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
		while (workFinished == false) {
			try {
				Thread.sleep(10000);
				synchronized (callbackInterface) {
//					Caso o escravo diga que o terminou, a Thread para
					if (workFinished) {
						break;
					}
//					Envia o checkpoint para o mestre via interface SlaveManager
					notification("Checkpoint sent");
					callbackInterface.checkpoint(slaveId, attackNumber, currentIndex);
				}
			} catch (InterruptedException e) {
//				Falha na thread
				e.printStackTrace();
			} catch (RemoteException e) {
				notification("Master not found");
				e.printStackTrace();
			}
		}
	}
	
//	Funcao de debug (deve se removida futuramente)
	private void notification(String msg) {
		System.out.println("Slave[name=" + slaveName + "]: " + msg);
	}

}
