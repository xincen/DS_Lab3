import java.net.*;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.Map.Entry;

public class Client extends Thread 
{

	private MessagePasser mpasser;
	private HashMap<String, Boolean> ssetup;
	private HashMap<String, Socket> connections;
	private HashMap<String, ObjectOutputStream> outs;
	
	public Client(MessagePasser aPasser) 
	{
		mpasser = aPasser;
		ssetup = new HashMap<String, Boolean>();
		connections = new HashMap<String, Socket>();
		outs = new HashMap<String, ObjectOutputStream>();
	}
	
	public void teardown() 
	{
		try 
		{
			Set<Entry<String, Socket>> set = connections.entrySet();
			Iterator<Entry<String, Socket>> itor = set.iterator();
			while(itor.hasNext())
			{
				Entry<String, Socket> entry = itor.next();
				entry.getValue().close();
				//System.out.println("closing sockets");
			}
		}
		catch (IOException e) 
		{
			System.out.println("Closing...");
		}
	}
	
	public void run() 
	{
		
		Message message;
		while(true) 
		{
			
			// take a rest...
			try 
			{
				Thread.sleep(100);
			} 
			catch (InterruptedException e) 
			{
				e.printStackTrace();
			}
			
			// check if anything in out_buffer
			if(!mpasser.getOutBuffer().isEmpty()) 
			{
				message = mpasser.getOutBuffer().removeFirst();
			
				String dest = message.getDest();
				if((ssetup.get(dest) != null) && (ssetup.get(dest) == true)) 
				{ 
				// if connection already set up	
					//Sender sender = new Sender(message, outs.get(message.getDest()));
					//sender.start();
					System.out.println("Sending message #" + message.getSeqNum() + " (" + message.getKind() + ")"
										+ " from " + message.getSrc() + " to " + message.getDest() +": " + message.getData());
					try 
					{
						outs.get(dest).writeObject(message);
						outs.get(dest).flush();
					}
					catch (IOException e) 
					{
						System.out.println(dest + " not available!");
						ssetup.put(dest, false);
					}
				}
				else 
				{
					try 
					{
						// set up a new connection
						System.out.println(message.getHostName() + " " + message.getPort());
						Socket socket = new Socket(message.getHostName(), message.getPort());
						ObjectOutputStream oout = new ObjectOutputStream(socket.getOutputStream());
						
						// store the connection
						ssetup.put(dest, true);
						connections.put(dest, socket);
						outs.put(dest, oout);
						
						//Sender sender = new Sender(message, oout);
						//sender.start();
						System.out.println("Sending message #" + message.getSeqNum() + " (" + message.getKind() + ")"
											+ " from " + message.getSrc() + " to " + message.getDest() + "(" +
											message.getGroup() +") : " + message.getData());
										
						oout.writeObject(message);
						oout.flush();
					}
					catch (UnknownHostException e) 
					{
						System.out.println(message.getDest() + " not available!");
						e.printStackTrace();
						ssetup.put(dest, false);
					}
					catch (IOException e)
					{
						System.out.println(message.getDest() + " not available!");
						e.printStackTrace();
						ssetup.put(dest, false);
					}
				}
				
			}
		}
	}
}