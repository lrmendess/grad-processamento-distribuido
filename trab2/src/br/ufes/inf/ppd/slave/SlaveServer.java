package br.ufes.inf.ppd.slave;

import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jms.JMSConsumer;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.MessageListener;

import com.sun.messaging.ConnectionConfiguration;
import com.sun.messaging.Queue;

public class SlaveServer {

	public static void main(String[] args) {
		String dictionaryPath = args[0];
		String host = (args.length < 2) ? "127.0.0.1" : args[1];
		
		System.out.print("Digite um nome para o escravo: ");
		Scanner scanner = new Scanner(System.in);
		String slaveName = scanner.nextLine();
		scanner.close();
		
		try {
			Logger.getLogger("").setLevel(Level.INFO);
	
			System.out.println("obtaining connection factory...");
			com.sun.messaging.ConnectionFactory connectionFactory = new com.sun.messaging.ConnectionFactory();
			connectionFactory.setProperty(ConnectionConfiguration.imqAddressList, host + ":7676");
			System.out.println("obtained connection factory.");
			
			System.out.println("Obtaining queue...");
			Queue subAttacksQueue = new Queue("SubAttacksQueue");
			Queue guessQueue = new Queue("GuessesQueue");
			System.out.println("Obtained queue.");
			
			JMSContext context = connectionFactory.createContext();
			JMSConsumer consumer = context.createConsumer(subAttacksQueue);

			MessageListener listener = new SlaveImpl(slaveName, dictionaryPath, guessQueue);
			consumer.setMessageListener(listener);
			
		} catch (JMSException e) {
			e.printStackTrace();
		}
	}

}
