package br.inf.ufes.ppd.slave;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.util.UUID;

import br.inf.ufes.ppd.Master;
import br.inf.ufes.ppd.Slave;

public class SlaveHeartbeatAssistant implements Runnable {

	private Slave remoteSlave;
	private String slaveName;
	private UUID slaveKey;
	
	private Master remoteMaster;
	private Registry registry;

	public SlaveHeartbeatAssistant(Slave remoteSlave, String slaveName, UUID slaveKey, Registry registry) {
		this.remoteSlave = remoteSlave;
		this.slaveName = slaveName;
		this.slaveKey = slaveKey;
		this.registry = registry;
	}

	@Override
	public void run() {
//		Fica aguardando o mestre estar disponivel
		notification("Looking for a Master reference...");
		searchMaster(true);
		
		notification("Master reference found");

		while (true) {
			try {
				remoteMaster.addSlave(remoteSlave, slaveName, slaveKey);
				Thread.sleep(30000);
				notification("Heartbeat Sent");

			} catch (RemoteException e) {
//				Houve algo de errado com o mestre quando o escravo tentou se "re-registrar",
//				sera feita uma tentativa de busca de uma nova referencia para ele no registry
//				uma unica vez
				notification("Looking for a new Master reference...");
				searchMaster(false);

			} catch (InterruptedException e) {
//				Exception de Thread.sleep
				e.printStackTrace();
			}
		}
	}

	/**
	 * Procura pelo mestre
	 * 
	 * @param true para buscar o mestre indeterminadamente, false para buscar apenas uma vez
	 */
	private void searchMaster(boolean persist) {
		do {
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
		} while (persist);
	}

//	Funcao de debug (deve se removida futuramente)
	private void notification(String msg) {
		System.out.println("Slave[name=" + slaveName + "]: " + msg);
	}
	
}
