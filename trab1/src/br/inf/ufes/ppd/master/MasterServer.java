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
//			args[1] = path do dicionario
			MasterImpl master = new MasterImpl(args[1]);
			Master masterReference = (Master) UnicastRemoteObject.exportObject(master, 0);
			
//			args[0] = ip do registry
			Registry registry = LocateRegistry.getRegistry(args[0]);
			registry.rebind("mestre", masterReference);
			
		} catch (RemoteException e) {
//			Houve uma falha na exportacao do objeto ou na obtencao do registry
			System.err.println("Permission denied or Registry not found");
			System.exit(0);
		}
	}

}
