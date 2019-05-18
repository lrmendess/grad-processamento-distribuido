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
//			perceba que o escravo terminou de testa-la
			callbackInterface.checkpoint(slaveId, attackNumber, currentIndex + 1);
			
		} catch (RemoteException e) {
//			TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
		while (true) {
			try {
				Thread.sleep(10000);
//				Caso o escravo diga que o terminou, a Thread para
//				Caso haja concorrencia, ele ira verificar se o ultimo indice de leitura foi o final
				if (workFinished) {
					break;
				}
				notification("Checkpoint sent");
				callbackInterface.checkpoint(slaveId, attackNumber, currentIndex);
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
