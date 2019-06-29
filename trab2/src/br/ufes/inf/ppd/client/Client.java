package br.ufes.inf.ppd.client;

import java.io.File;
import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import br.ufes.inf.ppd.Guess;
import br.ufes.inf.ppd.Master;
import br.ufes.inf.ppd.utils.ByteArray;

public class Client {

	public static void main(String[] args) {
//		args[0] = ip do registry
//		args[1] = nome do arquivo criptografado
//		args[2] = palavra conhecida
//		args[3] = tamanho do vetor de bytes caso o arquivo nao exista
		
		try {
			Registry registry = LocateRegistry.getRegistry(args[0]);
			Master master = (Master) registry.lookup("mestre");

			byte[] encryptedMessage = null;

			File cipherFile = new File(args[1]);

//			Cria um arquivo com bytes aleatorios caso o arquivo especificado pelo cliente nao exista
			if (!cipherFile.exists()) {
				if (args.length == 4) {
					encryptedMessage = ByteArray.createRandomByteArray(Integer.parseInt(args[3]));
				} else {
					encryptedMessage = ByteArray.createRandomByteArray(1000, 100001);
				}
				
				ByteArray.createFile(cipherFile, encryptedMessage);
			} else {
				encryptedMessage = ByteArray.readFile(cipherFile);
			}

			Guess[] guesses = master.attack(encryptedMessage, args[2].getBytes());

//			Para cada chute retornado, criaremos um arquivo <chute>.msg com o conteudo decriptografado dentro
			for (Guess guess : guesses) {
				File file = new File(guess.getKey() + ".msg");

				if (!file.exists()) {
					ByteArray.createFile(file, guess.getMessage());
				}
			}
		} catch (IOException e) {
//			Houve uma falha na leitura do arquivo
			e.printStackTrace();
		} catch (NotBoundException e) {
//			O Mestre nao foi encontrado no Registry
			System.err.println("O servico encontra-se indisponivel no momento");
		}
	}

}
