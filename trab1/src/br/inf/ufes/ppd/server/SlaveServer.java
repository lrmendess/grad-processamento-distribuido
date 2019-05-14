package br.inf.ufes.ppd.server;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.UUID;

import br.inf.ufes.ppd.Slave;
import br.inf.ufes.ppd.assistant.SlaveAssistant;
import br.inf.ufes.ppd.impl.SlaveImpl;

public class SlaveServer {

	public static void main(String[] args) {
		UUID slaveId = UUID.randomUUID();
		String slaveName = args[0];
		String dictionaryPath = args[1];
		
		Slave slave = new SlaveImpl(slaveId, slaveName, dictionaryPath);
		
		try {
			Slave remoteSlave = (Slave) UnicastRemoteObject.exportObject(slave, 0);
			
			Registry registry = LocateRegistry.getRegistry("localhost");
			
			SlaveAssistant slaveAssistant = new SlaveAssistant(remoteSlave, slaveId, slaveName, registry);
			
			Thread slaveMonitorThread = new Thread(slaveAssistant);
			slaveMonitorThread.start();
			
		} catch (RemoteException e) {
//			Algo deu errado na exportacao do objeto ou na obtencao do Registry
			e.printStackTrace();
			
		}
	}

}
