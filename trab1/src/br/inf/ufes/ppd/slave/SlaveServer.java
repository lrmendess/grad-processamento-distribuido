package br.inf.ufes.ppd.slave;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.UUID;

import br.inf.ufes.ppd.Master;
import br.inf.ufes.ppd.Slave;

public class SlaveServer {

	public static void main(String[] args) {
		UUID slaveKey = UUID.randomUUID();
		String slaveName = args[0];
		String dictionaryPath = args[1];
		
		Slave slave = new SlaveImpl(slaveName, slaveKey, dictionaryPath);
		
		try {
			Slave remoteSlave = (Slave) UnicastRemoteObject.exportObject(slave, 0);
			
			Registry registry = LocateRegistry.getRegistry("localhost");
			
			SlaveHeartbeatAssistant slaveAssistant = new SlaveHeartbeatAssistant(remoteSlave, slaveName, slaveKey, registry);
			
			Thread slaveMonitorThread = new Thread(slaveAssistant);
			slaveMonitorThread.start();
			
//			Ctrl + C remove o escravo do mestre
			Runtime.getRuntime().addShutdownHook(new Thread() {
				@Override
				public void run() {
					try {
						Master master = (Master) registry.lookup("mestre");
						master.removeSlave(slaveKey);
					} catch (RemoteException e) {
						e.printStackTrace();
					} catch (NotBoundException e) {
						e.printStackTrace();
					}
				}
			});
		} catch (RemoteException e) {
//			Algo deu errado na exportacao do objeto ou na obtencao do Registry
			e.printStackTrace();
		}
	}

}
