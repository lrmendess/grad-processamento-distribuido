package br.inf.ufes.ppd.client;

import java.io.File;
import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import br.inf.ufes.ppd.Master;
import br.inf.ufes.ppd.utils.ByteArray;

public class Client {

	public static void main(String[] args) {
		try {
			Registry registry = LocateRegistry.getRegistry("localhost");
			Master master = (Master) registry.lookup("mestre");
			
			byte[] encryptedMessage = null;
			
			File file = new File(args[0]);
			
			if (file.exists()) {
				encryptedMessage = ByteArray.readFile(file);
			
			} else {
				if (args.length == 3) {
					encryptedMessage = ByteArray.createRandomByteArray(Integer.parseInt(args[2]));
				} else {
					encryptedMessage = ByteArray.createRandomByteArray(1000, 100001);
				}
				
				ByteArray.createFile(file, encryptedMessage);
			}
			
			master.attack(encryptedMessage, args[1].getBytes());
			
		} catch (IOException e) {
//			Houve uma falha na leitura do arquivo
			e.printStackTrace();
			
		} catch (NotBoundException e) {
//			O Mestre nao foi encontrado no Registry
			e.printStackTrace();
		}

	}
	
}
