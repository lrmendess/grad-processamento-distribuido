package br.inf.ufes.ppd.client;

import java.io.File;
import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import br.inf.ufes.ppd.Guess;
import br.inf.ufes.ppd.Master;
import br.inf.ufes.ppd.utils.ByteArray;

public class ClientTester {
	
	public static void main(String[] args) {
		
//		Diretorio onde os arquivos de testes (Alice) estao, o padrao eh ../test/Alice
		String inputFolder = args[1];
		
//		Diretorio para armazenar os resultados
		String outputFolder = args[2];
		new File(outputFolder).mkdirs();
		
		int[] kbytesPerFile = new int[] {
			1, 2, 5, 10, 20, 30, 40, 50, 60, 70
		};
		
		String[] fileNames = new String[]{
			"alice1", "alice2", "alice5", "alice10",
			"alice20", "alice30", "alice40", "alice50",
			"alice60", "alice70"
		};
		
		String knownText = "Alice";
		StringBuilder timeCsv = new StringBuilder();
		
		try {
//			args[0] = ip do registry
			Registry registry = LocateRegistry.getRegistry(args[0]);
			Master master = (Master) registry.lookup("mestre");

			for (int i = 0; i < fileNames.length; i++) {
				File cipherFile = new File(inputFolder + "/" + fileNames[i] + ".txt.cipher");
				
				byte[] encryptedMessage = ByteArray.readFile(cipherFile);

				long start = System.nanoTime();
				Guess[] guesses = master.attack(encryptedMessage, knownText.getBytes());
				long end = System.nanoTime();
				
				double difference = ((double) (end - start)) / 1_000_000_000;
				
				String outputAliceFolder = outputFolder + "/" + fileNames[i];
				new File(outputAliceFolder).mkdirs();
				
//				Para cada chute retornado, criaremos um arquivo <chute>.msg com o conteudo descriptografado dentro
				for (Guess guess : guesses) {
					File guessFile = new File(outputAliceFolder + "/" + guess.getKey() + ".msg");
					ByteArray.createFile(guessFile, guess.getMessage());
				}
				
				String timeFileContent = kbytesPerFile[i] + "," + difference;
				timeCsv.append(timeFileContent + "\n");
			}
			
			File timeFile = new File(outputFolder + "/time.csv");
			ByteArray.createFile(timeFile, timeCsv.toString().getBytes());
		} catch (IOException e) {
//			Houve uma falha na leitura do arquivo
			e.printStackTrace();
		} catch (NotBoundException e) {
//			O Mestre nao foi encontrado no Registry
			System.err.println("O servico encontra-se indisponivel no momento");
		}
	}
	
}
