import java.net.*;
import java.io.*;
import java.util.LinkedList;

public class Receiver extends Thread {
	
	private Socket socket;
	private LinkedList<TimeStampedMessage> in_buffer;
	private ObjectInputStream in;
	private MessagePasser mp;
	
	public Receiver(Socket aSocket, LinkedList<TimeStampedMessage> aBuffer, MessagePasser mp) 
	{
		this.socket = aSocket;
		this.in_buffer = aBuffer;
		this.mp = mp;
	}
	
	public void teardown() {
		try {
			in.close();
		}
		catch (IOException e) {
			System.out.println("Closing...");
		}
	}
	
	public void run() {
	
		try {
			in = new ObjectInputStream(socket.getInputStream());
			TimeStampedMessage m;
			while(true) {

				Thread.sleep(100);

				m = (TimeStampedMessage) in.readObject();
				synchronized(in_buffer) {
					in_buffer.add(m);
				}
				mp.processInBuffer();
				//System.out.println("in receiver " + mp.getPid());
				if (mp.getPid() == 4)  // logger
					mp.receive();
			}
		}
		catch (IOException e) {
			//e.printStackTrace();
		}
		catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		catch (InterruptedException e) {
			e.printStackTrace();
		}
		finally {
			try {
				socket.close();
				in.close();
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}