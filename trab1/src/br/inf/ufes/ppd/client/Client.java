package br.inf.ufes.ppd.client;

import java.io.File;
import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import br.inf.ufes.ppd.Guess;
import br.inf.ufes.ppd.Master;
import br.inf.ufes.ppd.utils.ByteArray;

public class Client {

	public static void main(String[] args) {
		try {
			Registry registry = LocateRegistry.getRegistry("localhost");
			Master master = (Master) registry.lookup("mestre");

			byte[] encryptedMessage = null;

			File cipherFile = new File(args[0]);

//			Cria um arquivo com bytes aleatorios caso o arquivo especificado pelo cliente nao exista
			if (!cipherFile.exists()) {
				if (args.length == 3) {
					encryptedMessage = ByteArray.createRandomByteArray(Integer.parseInt(args[2]));
				} else {
					encryptedMessage = ByteArray.createRandomByteArray(1000, 100001);
				}

				ByteArray.createFile(cipherFile, encryptedMessage);
			} else {
				encryptedMessage = ByteArray.readFile(cipherFile);
			}

//			Solicita um ataque ao mestre
			Guess[] guesses = master.attack(encryptedMessage, args[1].getBytes());

//			Para cada chute retornado, criaremos um arquivo <chute>.msg com o conteudo descriptografado dentro
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
