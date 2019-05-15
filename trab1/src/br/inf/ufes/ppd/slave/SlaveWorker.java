package br.inf.ufes.ppd.slave;

import java.util.UUID;

import br.inf.ufes.ppd.Slave;
import br.inf.ufes.ppd.utils.Partition;

public class SlaveWorker implements Runnable {

	private Slave slave;
	private String name;
	private UUID key;
	
	private Partition partition;
	
	public SlaveWorker(Slave slave, String name, UUID key) {
		this.name = name;
		this.key = key;
		this.slave = slave;
		this.partition = null;
	}

	public UUID getKey() {
		return key;
	}

	public void setKey(UUID key) {
		this.key = key;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Slave getSlave() {
		return slave;
	}

	public void setSlave(Slave slave) {
		this.slave = slave;
	}

	public Partition getPartition() {
		return partition;
	}

	public void setPartition(Partition partition) {
		this.partition = partition;
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		
	}

}
