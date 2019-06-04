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
		String slaveName = args[1];
		String dictionaryPath = args[2];
		
		Slave slave = new SlaveImpl(slaveName, slaveKey, dictionaryPath);
		
		try {
			Slave remoteSlave = (Slave) UnicastRemoteObject.exportObject(slave, 0);
			
//			args[0] = ip do registry
			Registry registry = LocateRegistry.getRegistry(args[0]);

			SlaveHeartbeatAssistant slaveAssistant = new SlaveHeartbeatAssistant(remoteSlave, slaveName, slaveKey,
					registry);

			Thread slaveMonitorThread = new Thread(slaveAssistant);
			slaveMonitorThread.start();
			
//			Ctrl + C remove o escravo do mestre
			Runtime.getRuntime().addShutdownHook(new Thread() {
				@Override
				public void run() {
					try {
						Master master = (Master) registry.lookup("mestre");
						master.removeSlave(slaveKey);
						System.out.println("Bye!");
					} catch (RemoteException | NotBoundException e) {
						System.out.println("The master fell, bye!");
					}
				}
			});
		} catch (RemoteException e) {
//			Algo deu errado na exportacao do objeto ou na obtencao do Registry
			e.printStackTrace();
		}
	}

}
