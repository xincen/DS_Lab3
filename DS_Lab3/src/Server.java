import java.net.*;
import java.io.*;
import java.util.ArrayList;

public class Server extends Thread {
	
	private MessagePasser mpasser;
	private ServerSocket serverSocket;
	private ArrayList<Socket> csockets;
	private ArrayList<Receiver> receivers;
	
	public Server(MessagePasser aPasser) {
		mpasser = aPasser;
		csockets = new ArrayList<Socket>();
		receivers = new ArrayList<Receiver>();
	}
	
	public void teardown() {
		try {
			serverSocket.close();
			if(csockets != null)
				for(Socket s: csockets) {
					s.close();
					//System.out.println("deleting csockets");
				}
			if(receivers != null)
				for(Receiver r: receivers) {
					r.teardown();
				}
		}
		catch (IOException e) {
			System.out.println("Closing...");
		}
	}
	
	public void run() 
	{
		try 
		{
			for(Connection conn: mpasser.getConnList()) 
			{
			// check config
				if(mpasser.getLocalName().equals(conn.getName())) {
					System.out.println("Listening on port: " + conn.getPort());
					serverSocket = new ServerSocket(conn.getPort());
					while (true) {
						
						Thread.sleep(100);
						
						Socket clientSocket = serverSocket.accept();
						csockets.add(clientSocket);
						//System.out.println("new connection from " + clientSocket.getRemoteSocketAddress().toString());
						Receiver receiver = new Receiver(clientSocket, mpasser.getInBuffer(), mpasser);
						receivers.add(receiver);
						receiver.start();
						
					}
				}
			}
			System.out.println("No matching local_name");
			System.exit(1);
		}
		catch (IOException e) {
			System.out.println("Closing...");
		}
		catch (InterruptedException e) {
			System.out.println("Closing...");
		}
	}
}