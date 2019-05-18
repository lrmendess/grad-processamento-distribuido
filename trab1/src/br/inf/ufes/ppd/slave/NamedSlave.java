package br.inf.ufes.ppd.slave;

import java.util.UUID;

import br.inf.ufes.ppd.Slave;

public class NamedSlave {

	private Slave slave;
	private String name;
	private UUID key;

	public NamedSlave(Slave slave, String name, UUID key) {
		this.slave = slave;
		this.name = name;
		this.key = key;
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

}
