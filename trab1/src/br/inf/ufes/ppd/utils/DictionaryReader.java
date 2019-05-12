package br.inf.ufes.ppd.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Iterator;
import java.util.List;

public class DictionaryReader implements Iterable<String> {

	private List<String> lines;
	
	private int start;
	private int end;
	private int currentLineNumber;
	
	/**
	 * Utilitario de leitura do dicionario
	 * 
	 * @param caminho do arquivo
	 * @throws IOException
	 */
	public DictionaryReader(String filePath) throws IOException, FileNotFoundException {
		File file = new File(filePath);
		
		if (!file.exists()) {
			throw new FileNotFoundException();
		}
		
		/* ArrayList<String> */
		lines = Files.readAllLines(file.toPath());
		
		start = 0;
		end = lines.size();
		
		currentLineNumber = 0;
	}
	
	/**
	 * Estabelece um range de leitura entre as linhas do arquivo
	 * 
	 * @param index de inicio
	 * @param index de fim
	 * @throws IndexOutOfBoundsException
	 */
	public void setRange(int start, int end) throws IndexOutOfBoundsException {
		if ((start < 0) || (end > lines.size()))
			throw new IndexOutOfBoundsException();
		
		this.start = start;
		this.end = end;
	}
	
	/**
	 * Realiza a leitura da proxima linha no buffer
	 * 
	 * @return proxima linha do buffer
	 */
	public String readLine() {
		String line = lines.get(currentLineNumber);
		currentLineNumber++;
		
		return line;
	}
	
	/**
	 * Realiza a leitura de uma linha especifica
	 * 
	 * @param index da linha desejada
	 * @return linha desejada
	 * @throws IndexOutOfBoundsException
	 */
	public String readLine(int index) throws IndexOutOfBoundsException {
		if ((index < start) || (index >= end))
			throw new IndexOutOfBoundsException();
		
		return lines.get(index);
	}
	
	/**
	 * Verifica se ainda existe linhas para serem lidas
	 * 
	 * @return pode ler a proxima linha ou nao
	 */
	public boolean ready() {
		return (currentLineNumber >= start) && (currentLineNumber < end);
	}
	
	/**
	 * Contabiliza todas as linhas dentro do range estabelecido, de start ate end
	 * 
	 * @return numero total de linhas range estabelicido
	 */
	public int countLines() {
		return end - start;
	}
	
	/**
	 * Contabiliza todas as linhas do arquivo original, do primeiro ao ultimo byte
	 * 
	 * @return numero total de linhas no arquivo original
	 */
	public int countAllLines() {
		return lines.size();
	}
	
	/**
	 * Retorna o ponteiro para a primeira linha do arquivo
	 */
	public void rewind() {
		currentLineNumber = 0;
	}
	
	/**
	 * Retorna a linha a qual o ponteiro se encontra
	 * 
	 * @return linha do ponteiro
	 */
	public int getLineNumber() {
		return currentLineNumber;
	}

	@Override
	public Iterator<String> iterator() {
		return new Iterator<String>() {

			@Override
			public boolean hasNext() {
				return ready();
			}

			@Override
			public String next() {
				return readLine();
			}
			
		};
	}

}
