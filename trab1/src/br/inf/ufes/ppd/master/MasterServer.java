package br.inf.ufes.ppd.master;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

import br.inf.ufes.ppd.Master;

/*
 * Comando para executar o Registry no Windows
 * 
 * rmiregistry -J-Djava.rmi.server.hostname=localhost
 */
public class MasterServer {

	public static void main(String[] args) {
		try {
			MasterImpl master = new MasterImpl(args[0]);
			Master masterReference = (Master) UnicastRemoteObject.exportObject(master, 0);
			
			Registry registry = LocateRegistry.getRegistry("localhost");
			registry.rebind("mestre", masterReference);
			
		} catch (RemoteException e) {
//			Houve uma falha na exportacao do objeto ou na obtencao do registry
			e.printStackTrace();
			
		}
	}

}
