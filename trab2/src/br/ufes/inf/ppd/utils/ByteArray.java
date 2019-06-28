package br.ufes.inf.ppd.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class ByteArray {

	/**
	 * Le os bytes de um arquivo
	 * 
	 * @param file
	 * @return
	 * @throws IOException
	 */
	public static byte[] readFile(File file) throws IOException {
		return Files.readAllBytes(file.toPath());
	}
	
	/**
	 * Cria um arquivo a partir de um vetor de bytes
	 * 
	 * @param file
	 * @param byteArray
	 * @throws IOException
	 */
	public static void createFile(File file, byte[] byteArray) throws IOException {
		Files.write(file.toPath(), byteArray);
	}
	
	/**
	 * Cria um vetor de bytes aleatorio dentro de um dado intervalo
	 * 
	 * @param min
	 * @param max
	 * @return
	 */
	public static byte[] createRandomByteArray(int min, int max) {
		int randomSize = ThreadLocalRandom.current().nextInt(min, max);
		byte[] byteArray = new byte[randomSize];
		
		Random random = new Random();
		random.nextBytes(byteArray);

		return byteArray;
	}
	
	/**
	 * Cria um vetor de bytes aleatorio dado um tamanho maximo
	 * 
	 * @param length
	 * @return
	 */
	public static byte[] createRandomByteArray(int length) {
		byte[] byteArray = new byte[length];
		
		Random random = new Random();
		random.nextBytes(byteArray);

		return byteArray;
	}
	
}
