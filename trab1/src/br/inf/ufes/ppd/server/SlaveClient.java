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

public class SlaveClient implements Runnable {

	private Registry registry;
	private Master masterReference;

	private UUID slaveId;
	private String slaveName;
	
	private Slave slaveReference;

	public SlaveClient(UUID slaveId, String slaveName) {
		try {
			Slave slave = new SlaveImpl(slaveId, slaveName);
			slaveReference = (Slave) UnicastRemoteObject.exportObject(slave, 0);
			
			registry = LocateRegistry.getRegistry();
			masterReference = (Master) registry.lookup("mestre");

		} catch (RemoteException e) {
//			Algo deu errado (o que? nao faco ideia)
			e.printStackTrace();

		} catch (NotBoundException e) {
//			O mestre nao foi encontrado
			e.printStackTrace();
		}
		
		this.slaveId = slaveId;
		this.slaveName = slaveName;

	}

	public static void main(String[] args) {
		SlaveClient slaveClient = new SlaveClient(UUID.randomUUID(), args[0]);

		Thread slaveMonitor = new Thread(slaveClient);
		slaveMonitor.start();
	}

	@Override
	public void run() {
		while(true) {
			try {
				masterReference.addSlave(slaveReference, slaveName, slaveId);
				Thread.sleep(30000);
	
			} catch (RemoteException e) {
//				Houve algo de errado com o mestre quando o escravo tentou se "re-registrar",
//				sera feita uma tentativa de busca de uma nova referencia para ele no registry
				while (true) {
					try {
						masterReference = (Master) registry.lookup("mestre");
						break;

					} catch (Exception e1) {
//						O mestre ainda nao foi encontrado...
						System.out.println("Slayer::" + slaveName + " esta procurando uma nova referencia para o mestre");
					}

				}
	
			} catch (InterruptedException e) {
//				Exception de Thread.sleep
				e.printStackTrace();
			}
		}
	}

}
