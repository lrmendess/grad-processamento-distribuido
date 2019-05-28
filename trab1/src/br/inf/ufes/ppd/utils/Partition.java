package br.inf.ufes.ppd.utils;

import java.util.ArrayList;
import java.util.List;

public class Partition {
	
	private int start;
	private int end;
	
	public Partition(int start, int end) {
		this.start = start;
		this.end = end;
	}
	
	public Partition(Partition partition) { 
		this.start = partition.getStart();
		this.end = partition.getEnd();
	}

	public int getStart() {
		return start;
	}

	public void setStart(int start) {
		this.start = start;
	}

	public int getEnd() {
		return end;
	}

	public void setEnd(int end) {
		this.end = end;
	}
	
	/**
	 * Verifica se um dado index esta dentro do intervalo da particao
	 * 
	 * @param index
	 * @return
	 */
	public boolean isBetweenTheRange(int index) {
		return (index >= start) && (index < end);
	}
	
	/**
	 * Particiona uma particao em K particoes iguais
	 * 
	 * @param numberOfPartitions
	 * @return
	 */
	public List<Partition> shatter(int numberOfPartitions) {
		List<Partition> partitions = new ArrayList<>();

		int partitionLength = (end - start) / numberOfPartitions;
		int partitionLeftovers = (end - start) % numberOfPartitions;

		int currentIndex = start;
		while ((currentIndex >= start) && (currentIndex < end)) {
			int min = currentIndex;
			
			if (partitionLeftovers > 0) {
				currentIndex = seek(currentIndex, partitionLength + 1);
				partitionLeftovers--;
			} else {
				currentIndex = seek(currentIndex, partitionLength);
			}
			
			int max = currentIndex;
			
			partitions.add(new Partition(min, max));
		}

		return partitions;
	}
	
	/**
	 * Avanca o ponteiro da particao a partir de um dado buffer
	 * 
	 * @param currentIndex
	 * @param partitionLength
	 * @return
	 */
	private int seek(int currentIndex, int partitionLength) {
		int pointer = currentIndex + partitionLength;
		
		if ((pointer >= start) && (pointer < end)) {
			return pointer;
		} else {
			if (pointer < start) {
				return start;
			} else {
				return end;
			}
		}
	}
	
	@Override
	public String toString() {
		return "Partition[start=" + start + ", end=" + end + "]";
	}
	
}
