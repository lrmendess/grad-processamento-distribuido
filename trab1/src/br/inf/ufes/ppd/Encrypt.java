package br.inf.ufes.ppd;

import javax.crypto.*;
import javax.crypto.spec.*;
import java.io.*;
import java.security.*;

@SuppressWarnings("unused")
public class Encrypt {

	private static byte[] readFile(String filename) throws IOException {
		File file = new File(filename);
		InputStream is = new FileInputStream(file);
		long length = file.length();
		
//		Creates array (assumes file length<Integer.MAX_VALUE)
		byte[] data = new byte[(int) length];
		
		int offset = 0;
		int count = 0;
		
		while ((offset < data.length) && (count = is.read(data, offset, data.length - offset)) >= 0) {
			offset += count;
		}
		
		is.close();
		
		return data;
	}

	private static void saveFile(String filename, byte[] data) throws IOException {
		FileOutputStream out = new FileOutputStream(filename);
		
		out.write(data);
		out.close();
	}

	public static void main(String[] args) {
//		args[0]: Chave a ser usada
//		args[1]: Nome do arquivo de entrada

		try {
			
			byte[] key = args[0].getBytes();
			SecretKeySpec keySpec = new SecretKeySpec(key, "Blowfish");

			Cipher cipher = Cipher.getInstance("Blowfish");
			cipher.init(Cipher.ENCRYPT_MODE, keySpec);

			byte[] message = readFile(args[1]);
			System.out.println("message size (bytes) = " + message.length);

			byte[] encrypted = cipher.doFinal(message);

			saveFile(args[1] + ".cipher", encrypted);

		} catch (Exception e) {
//			Don't try this at home
			e.printStackTrace();
		}
	}
}
