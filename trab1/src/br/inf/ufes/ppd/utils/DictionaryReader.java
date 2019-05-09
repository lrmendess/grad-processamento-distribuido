package br.inf.ufes.ppd.utils;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Stream;

/**
 * DictionaryReader
 */
public class DictionaryReader implements AutoCloseable {

	private LineNumberReader reader;
	private String fileName;
	private long start;
	private long end;
	
	public DictionaryReader(String fileName, long start, long end) throws IOException, FileNotFoundException {
		FileReader fileReader = new FileReader(fileName);
		BufferedReader bufferedReader = new BufferedReader(fileReader);
		reader = new LineNumberReader(bufferedReader);
		
//		Itera ate o index inicial
		while (reader.ready()) {
			if (reader.getLineNumber() < start) {
				reader.readLine();
				
			} else {
				break;
			}
		}
		
		this.fileName = fileName;
		this.start = start;
		this.end = end;
	}
	
	public boolean ready() throws IOException {
//		Antes de resgatar o numero da linha, e necessario verificar se o reader esta disponivel
		if (reader.ready()) {
			return reader.getLineNumber() < end;
			
		} else {
			return false;
		}
	}
	
	public String readLine() throws IOException, IndexOutOfBoundsException {
//		Se o reader nao esta disponivel, quer dizer que nao ha mais nada para ler, logo uma posicao
//		indevida esta sendo acessada, seja pelo fim do arquivo ou pelo limite estabelecido
		if (!this.ready()) {
			throw new IndexOutOfBoundsException();
		}
		
		return reader.readLine();
	}
	
	public String readLine(long index) throws IOException, IndexOutOfBoundsException {
//		Verifica se o index da linha desejada encontra-se dentro do range estabelecido
		if (index < start || index >= end) {
			throw new IndexOutOfBoundsException();
		}
		
		try (Stream<String> lines = Files.lines(Paths.get(fileName), StandardCharsets.UTF_8)) {
			return lines.skip(index).findFirst().get();
		}
	}
	
	public long getLineNumber() {
		return reader.getLineNumber();
	}

	@Override
	public void close() throws IOException  {
		reader.close();
	}
	
	public long countLines() throws IOException {
//		Conta a quantidade total de linhas de um arquivo (incluindo linhas em branco)
		try (Stream<String> lines = Files.lines(Paths.get(fileName), StandardCharsets.UTF_8)) {
			return lines.count();
		}
	}
	
}
