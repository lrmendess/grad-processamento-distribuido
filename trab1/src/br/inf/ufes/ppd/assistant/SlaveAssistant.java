package br.inf.ufes.ppd.assistant;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.util.UUID;

import br.inf.ufes.ppd.Master;
import br.inf.ufes.ppd.Slave;

public class SlaveAssistant implements Runnable {

	private Slave remoteSlave;

	private UUID slaveId;
	private String slaveName;
	
	private Master remoteMaster;
	private Registry registry;

	public SlaveAssistant(Slave remoteSlave, UUID slaveId, String slaveName, Registry registry) {
		this.remoteSlave = remoteSlave;
		this.slaveId = slaveId;
		this.slaveName = slaveName;
		this.registry = registry;
	}

	@Override
	public void run() {
		notification("Looking for a Master reference...");
		searchMaster();
		notification("Master reference found");

		while (true) {
			try {
				remoteMaster.addSlave(remoteSlave, slaveName, slaveId);
				Thread.sleep(30000);
				notification("Heartbeat Sent");

			} catch (RemoteException e) {
//				Houve algo de errado com o mestre quando o escravo tentou se "re-registrar",
//				sera feita uma tentativa de busca de uma nova referencia para ele no registry
				notification("Looking for a new Master reference...");
				searchMaster();

			} catch (InterruptedException e) {
//				Exception de Thread.sleep
				e.printStackTrace();
			}
		}
	}

	private void searchMaster() {
		while (true) {
			try {
				remoteMaster = (Master) registry.lookup("mestre");
				break;

			} catch (NotBoundException e) {
//				O mestre nao foi encontrado
				continue;

			} catch (Exception e) {
//				O Registry apresentou problemas, o que deve ser feito? (Duvida)
				e.printStackTrace();
			}
		}
	}

//	Funcao de debug (deve se removida futuramente)
	private void notification(String msg) {
		System.out.println("Slave[name=" + slaveName + "]: " + msg);
	}
	
}
