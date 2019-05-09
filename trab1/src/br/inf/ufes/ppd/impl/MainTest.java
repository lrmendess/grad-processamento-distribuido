package br.inf.ufes.ppd.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import br.inf.ufes.ppd.Slave;

public class MainTest {

	public static void main(String[] args) {
		try {
			byte[] message = readFile(args[0]);
			
			Slave slave = new SlaveImpl();
			slave.startSubAttack(message, null, 1, 6, 0, null);
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
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

}
