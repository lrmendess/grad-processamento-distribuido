package br.inf.ufes.ppd.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainTest {

	public static void main(String[] args) throws InterruptedException {
		List<String> list = new ArrayList<String>(Arrays.asList("Lucas", "Ribeiro", "Mendes", "Silva"));
		
		list.forEach(each -> {
			System.out.println(each);
		});
		
		System.out.println(list);
	}
	
}
