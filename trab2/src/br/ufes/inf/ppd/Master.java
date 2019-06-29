package br.ufes.inf.ppd;

/**
 * Master.java
 */
import java.rmi.Remote;

public interface Master extends Remote, Attacker {
	// O mestre eh um Attacker
}
