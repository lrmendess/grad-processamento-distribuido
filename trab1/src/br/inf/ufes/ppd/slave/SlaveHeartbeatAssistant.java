package br.inf.ufes.ppd.slave;

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

	/**
	 * Procura uma referencia para o mestre e envia heartbeats a cada 30 segundos, ou seja,
	 * a cada 30 segundos o escravos se readiciona na lista de escravos do mestre.
	 */
	@Override
	public void run() {
		try {
			System.out.println("Looking for a Master reference...");
			remoteMaster = (Master) registry.lookup("mestre");
		} catch (Exception e) {
			System.exit(1);
		}
		
		while (true) {
			try {
				remoteMaster.addSlave(remoteSlave, slaveName, slaveKey);
				System.out.println("Master [ON]. Heartbeat Sent");
				Thread.sleep(30000);

			} catch (RemoteException e) {
//				Houve algo de errado com o mestre quando o escravo tentou se "re-registrar",
//				sera feita uma tentativa de busca de uma nova referencia para ele no registry
//				uma unica vez
				System.out.println("Master [OFF]. Looking for a new Master reference...");
				searchMaster();
			} catch (InterruptedException e) {
//				Exception de Thread.sleep
				e.printStackTrace();
			}
		}
	}

	/**
	 * Procura pelo mestre
	 * 
	 * @param true para buscar o mestre indeterminadamente
	 */
	private void searchMaster() {
		while(true) {
			try {
				remoteMaster = (Master) registry.lookup("mestre");
				remoteMaster.addSlave(remoteSlave, slaveName, slaveKey);
				System.out.println("Master reference found");
				System.out.println("Heartbeat Sent");
				Thread.sleep(30000);
				break;

			} catch (Exception e) {
//				O mestre nao foi encontrado, o escravo ira esperar 3 segundos para tentar procura-lo de novo
				try {
					Thread.sleep(3000);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
			}
		}
	}
	
}
