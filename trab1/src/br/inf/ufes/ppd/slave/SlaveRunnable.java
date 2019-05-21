package br.inf.ufes.ppd.slave;

import java.rmi.RemoteException;
import java.util.UUID;

import br.inf.ufes.ppd.Slave;
import br.inf.ufes.ppd.SlaveManager;
import br.inf.ufes.ppd.utils.Partition;

public class SlaveRunnable implements Runnable {

	private String slaveName;
	private UUID slaveKey;
	private Slave slave;

	private Partition partition;

	private byte[] cipherText;
	private byte[] knownText;
	private int attackNumber;
	private SlaveManager callbackInterface;

	public SlaveRunnable(NamedSlave namedSlave) {
		this.slaveName = namedSlave.getName();
		this.slaveKey = namedSlave.getKey();
		this.slave = namedSlave.getSlave();
	}

	public String getSlaveName() {
		return slaveName;
	}
	
	public UUID getSlaveKey() {
		return slaveKey;
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
//			Houveram problemas com o escravo, logo a particao dele deve ser realocada
			e.printStackTrace();
		}
	}

}
