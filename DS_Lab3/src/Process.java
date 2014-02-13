import java.io.*;
import java.util.ArrayList;
import java.util.LinkedList;

public class Process 
{
	public static void main(String[] args) 
	{
		TimeStampedMessage msg;
		LinkedList<TimeStampedMessage> logs;
		
		if(args.length != 3) 
		{
			Usage();
			System.exit(1);
		}
		
		MessagePasser mpasser = new MessagePasser(args[0], args[1]);
		System.out.println("This is "+ args[1]);
		
		try 
		{
			mpasser.parseConfig();
		} 
		catch (FileNotFoundException e) 
		{
			System.out.println("File not found!");
		}
		mpasser.setPid();       // sets process' ID
		
		mpasser.enableTimeoutResend = true;
		
		//System.out.println("my pid is: " + mpasser.getPid());
		mpasser.initSystemTimeStamp(args[2], mpasser.getNbrOfProcesses());  // initialize reference time
		
		Server server = new Server(mpasser);
		server.start();
		Client client = new Client(mpasser);
		client.start();
		
		
		try 
		{
			Thread.sleep(100);
		}
		catch (InterruptedException e) 
		{
			e.printStackTrace();
		}
		
		BufferedReader bufreader = new BufferedReader(new InputStreamReader(System.in));
		while (true) 
		{
			try
			{
				String command = bufreader.readLine();
				String[] commandArgs = command.split(" ");
                
				if (command.equals("q")) 
				{
					client.teardown();
					server.teardown();
					System.exit(1);
				}
				else if (command.equals("r")) 
				{
					TimeStampedMessage aMessage = mpasser.receive();
					
					if (aMessage != null) 
					{
							System.out.println("/***** Message #" + aMessage.getSeqNum() + " (" + aMessage.getKind() + ")"
									+ " from " + aMessage.getSrc() + ": " + aMessage.getData() +
									"with timestamp : " + aMessage.getTimeStamp().getTimeStamp() + " *****/");
					} else {
						System.out.println("Nothing received");
					}
				}
				else if (commandArgs[0].equals("s")) 
				{
					
					if (commandArgs.length < 4) 
					{
						Usage();
						continue;
					}
					
					StringBuffer data = new StringBuffer();
					for(int i = 3; i < commandArgs.length; i++) 
					{
						data.append(commandArgs[i] + " ");
					}
					
					//msg = new Message(commandArgs[1], commandArgs[2], data.toString());
					msg = new TimeStampedMessage(commandArgs[1], commandArgs[2], data.toString(), mpasser.seqNum, 1,
									args[2], mpasser.getNbrOfProcesses(), mpasser.getSystemTimeStamp());
					msg.setTimeStamp(mpasser.getSystemTimeStamp());
					
					mpasser.send(msg); //dest, kind, data
					/*
					if(msg instanceof TimeStampedMessage)
					{
						System.out.println("message time stamp is: " + ((TimeStampedMessage)msg).
								getTimeStamp().getTimeStamp());
						System.out.println("system time is now: " + mpasser.getSystemTimeStamp().
								getTimeStamp());
					}
					else
						System.out.println("uh oh...."); 
						*/
				}
				else 
				{
					Usage();
				}
	
			} catch (IOException e) 
			{
				e.printStackTrace();
			}
		}
	}
                                
	public static void Usage() 
	{
		System.out.println("Usage: <configuration_filename> <local_name> <clock service: logical or vector>");
		System.out.println("CommandLine: q: quit, r: receive, s <dest/group> <kind> <data>: send");
	}
}