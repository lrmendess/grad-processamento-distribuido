package br.inf.ufes.ppd.server;

import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

import br.inf.ufes.ppd.Master;
import br.inf.ufes.ppd.impl.MasterImpl;

/*
 * Comando para executar o Registry no Windows
 * 
 * rmiregistry -J-Djava.rmi.server.hostname=localhost
 */
public class MasterServer {

	public static void main(String[] args) {
		try {
			MasterImpl master = new MasterImpl();
			Master masterReference = (Master) UnicastRemoteObject.exportObject(master, 0);
			
			Registry registry = LocateRegistry.getRegistry("localhost");
			registry.bind("mestre", masterReference);
			
		} catch (RemoteException e) {
//			Houve uma falha na exportacao do objeto ou na obtencao do registry
			e.printStackTrace();
			
		} catch (AlreadyBoundException e) {
//			Ja existe um objeto remote de nome "mestre"
			e.printStackTrace();
		}

	}

}
