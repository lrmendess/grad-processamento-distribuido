package br.inf.ufes.ppd.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class ByteArray {

	public static byte[] readFile(File file) throws IOException {
		return Files.readAllBytes(file.toPath());
	}
	
	public static void createFile(File file, byte[] byteArray) throws IOException {
		Files.write(file.toPath(), byteArray);
	}
	
	public static byte[] createRandomByteArray(int min, int max) {
		int randomSize = ThreadLocalRandom.current().nextInt(min, max);
		byte[] byteArray = new byte[randomSize];
		
		Random random = new Random();
		random.nextBytes(byteArray);

		return byteArray;
	}
	
	public static byte[] createRandomByteArray(int length) {
		byte[] byteArray = new byte[length];
		
		Random random = new Random();
		random.nextBytes(byteArray);

		return byteArray;
	}
	
}
