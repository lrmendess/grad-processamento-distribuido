package br.inf.ufes.ppd.server;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.rmi.AlreadyBoundException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

import br.inf.ufes.ppd.Master;
import br.inf.ufes.ppd.impl.MasterImpl;

public class MasterServer {

	public static void main(String[] args) {
		try {
			byte[] encryptedMessage = readFile("test/text.txt.cipher");
			
			Master master = new MasterImpl();
			Master masterReference = (Master) UnicastRemoteObject.exportObject(master, 0);

			Registry registry = LocateRegistry.getRegistry("localhost");
			registry.bind("mestre", masterReference);
			
			masterReference.attack(encryptedMessage, null);
			
		} catch (IOException e) {
			System.err.println("IOException::" + e.getMessage());
			e.printStackTrace();
			
		} catch (AlreadyBoundException e) {
			System.err.println("ServerException::" + e.getMessage());
			e.printStackTrace();
		}
	}
	
	private static byte[] readFile(String fileName) throws IOException {
		byte[] byteArray = Files.readAllBytes(new File(fileName).toPath());
		
		return byteArray;		
	}

}
