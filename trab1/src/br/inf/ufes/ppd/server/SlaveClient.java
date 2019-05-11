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

public class SlaveClient {
	
	public static void main(String[] args) {
		try {
			Registry registry = LocateRegistry.getRegistry();
			Master master = (Master) registry.lookup("mestre");

			Slave slave = new SlaveImpl("Tassis");
			Slave slaveReference = (Slave) UnicastRemoteObject.exportObject(slave, 0);
			master.addSlave(slaveReference, "Tassis", UUID.randomUUID());

		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();

		} catch (NotBoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
}
