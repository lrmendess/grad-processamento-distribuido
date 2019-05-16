package br.inf.ufes.ppd.slave;

import java.rmi.RemoteException;
import java.util.UUID;

import br.inf.ufes.ppd.Slave;
import br.inf.ufes.ppd.SlaveManager;
import br.inf.ufes.ppd.utils.Partition;

public class SlaveWorker implements Runnable {

	private Slave slave;
	private String name;
	private UUID key;

	private Partition partition;

	private byte[] cipherText;
	private byte[] knownText;
	private int attackNumber;
	private SlaveManager callbackInterface;

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

	public void setSubAttackParameters(byte[] cipherText, byte[] knownText, int attackNumber,
			SlaveManager callbackInterface) {
		this.cipherText = cipherText;
		this.knownText = knownText;
		this.attackNumber = attackNumber;
		this.callbackInterface = callbackInterface;
	}

	@Override
	public void run() {
		try {
			slave.startSubAttack(cipherText, knownText, partition.getMin(), partition.getMax(), attackNumber,
					callbackInterface);

		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

}
