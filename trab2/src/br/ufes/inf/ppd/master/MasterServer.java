package br.ufes.inf.ppd.master;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jms.JMSConsumer;
import javax.jms.JMSContext;
import javax.jms.JMSException;

import com.sun.messaging.ConnectionConfiguration;
import com.sun.messaging.ConnectionFactory;
import com.sun.messaging.Queue;

import br.ufes.inf.ppd.Master;

/*
 * Comando para executar o Registry no Windows
 * 
 * rmiregistry -J-Djava.rmi.server.hostname=localhost
 */
public class MasterServer {

	public static void main(String[] args) {
		try {
//			args[0] = path do dicionario
//			args[1] = numero de particoes do dicionario
//			args[2] = ip do registry

			String host = (args.length < 3) ? "127.0.0.1" : args[2];

			Logger.getLogger("").setLevel(Level.INFO);
			
			System.out.println("Obtaining connection factory...");
			ConnectionFactory connectionFactory = new ConnectionFactory();
			connectionFactory.setProperty(ConnectionConfiguration.imqAddressList, host + ":7676");
			connectionFactory.setProperty(ConnectionConfiguration.imqConsumerFlowLimitPrefetch, "false");
			System.out.println("Obtained connection factory.");

			System.out.println("Obtaining queue...");
			Queue guessesQueue = new Queue("GuessesQueue");
			Queue subAttacksQueue = new Queue("SubAttacksQueue");
			System.out.println("Obtained queue.");

			JMSContext context = connectionFactory.createContext();
			JMSConsumer consumer = context.createConsumer(guessesQueue);
			
			MasterImpl master = new MasterImpl(args[0], Integer.parseInt(args[1]), subAttacksQueue, context);
			
			consumer.setMessageListener(master);
			
			Master masterReference = (Master) UnicastRemoteObject.exportObject(master, 0);
			
			Registry registry = LocateRegistry.getRegistry(args[0]);
			registry.rebind("mestre", masterReference);
		} catch (RemoteException e) {
			System.err.println("Permission denied or Registry not found");
			System.exit(0);			
		} catch (JMSException e) {
			e.printStackTrace();
		}
	}

}
