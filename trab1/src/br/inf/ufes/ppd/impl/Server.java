package br.inf.ufes.ppd.impl;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.UUID;

import br.inf.ufes.ppd.Master;
import br.inf.ufes.ppd.Slave;

public class Server {

	public static void main(String[] args) {
		try {
			byte[] encryptedMessage = readFile("test/text.txt.cipher");
			
			Master master = new MasterImpl();
			Slave slave = new SlaveImpl();
			
			master.addSlave(slave, "Tassis", UUID.randomUUID());
			master.attack(encryptedMessage, null);
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private static byte[] readFile(String fileName) throws IOException {
		byte[] byteArray = Files.readAllBytes(new File(fileName).toPath());
		
		return byteArray;		
	}

}
