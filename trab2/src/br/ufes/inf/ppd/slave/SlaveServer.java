package br.ufes.inf.ppd.slave;

import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jms.JMSConsumer;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.Message;

import com.sun.messaging.ConnectionConfiguration;
import com.sun.messaging.ConnectionFactory;
import com.sun.messaging.Queue;

import br.ufes.inf.ppd.Slave;

public class SlaveServer {

	public static void main(String[] args) {
		String dictionaryPath = args[0];
		String host = (args.length < 2) ? "127.0.0.1" : args[1];
		
		System.out.print("Enter a name for the slave: ");
		Scanner scanner = new Scanner(System.in);
		String slaveName = scanner.nextLine();
		scanner.close();
		
		try {
			Logger.getLogger("").setLevel(Level.INFO);
	
			System.out.println("Obtaining connection factory...");
			ConnectionFactory connectionFactory = new ConnectionFactory();
			connectionFactory.setProperty(ConnectionConfiguration.imqAddressList, host + ":7676");
			connectionFactory.setProperty(ConnectionConfiguration.imqConsumerFlowLimitPrefetch, "false");
			System.out.println("Obtained connection factory.");
			
			System.out.println("Obtaining queue...");
			Queue subAttacksQueue = new Queue("SubAttacksQueue");
			Queue guessQueue = new Queue("GuessesQueue");
			System.out.println("Obtained queue.");
			
			JMSContext context = connectionFactory.createContext();
			JMSConsumer consumer = context.createConsumer(subAttacksQueue);

			Slave slave = new SlaveImpl(slaveName, dictionaryPath, guessQueue, context);
			
			while (true) {
				Message message = consumer.receive();
				slave.sentMessage(message);
				Thread.sleep(125);
			}
		} catch (JMSException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

}
