package br.inf.ufes.ppd.server;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.UUID;

import br.inf.ufes.ppd.Master;
import br.inf.ufes.ppd.Slave;
import br.inf.ufes.ppd.impl.SlaveImpl;

public class SlaveServer implements Runnable {

	private Registry registry;
	private Master masterReference;
	
	private UUID slaveId;
	private String slaveName;
	
	private Slave slaveReference;

	public SlaveServer(UUID slaveId, String slaveName, String dictionaryPath) {
		try {
			Slave slave = new SlaveImpl(slaveId, dictionaryPath);
			slaveReference = (Slave) UnicastRemoteObject.exportObject(slave, 0);
			
			registry = LocateRegistry.getRegistry();
			
		} catch (RemoteException e) {
//			Algo deu errado na exportacao do objeto ou na obtencao do Registry
			e.printStackTrace();
		}
		
		this.slaveId = slaveId;
		this.slaveName = slaveName;
	}

	public static void main(String[] args) {
		SlaveServer slaveClient = new SlaveServer(UUID.randomUUID(), args[0], args[1]);

		Thread slaveMonitor = new Thread(slaveClient);
		slaveMonitor.start();
	}

	@Override
	public void run() {
		notification("Looking for a Master reference...");
		searchMaster();
		
		while(true) {
			try {
				masterReference.addSlave(slaveReference, slaveName, slaveId);
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
				masterReference = (Master) registry.lookup("mestre");
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
	
	private void notification(String msg) {
		System.out.println("Slave[name=" + slaveName + "]: " + msg);
	}

}
