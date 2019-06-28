package br.ufes.inf.ppd;

/**
 * Master.java
 */
import java.rmi.Remote;

import javax.jms.MessageListener;

public interface Master extends Remote, Attacker, MessageListener {
	// O mestre é um SlaveManager e um Attacker
}
