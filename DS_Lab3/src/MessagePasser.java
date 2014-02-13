import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.yaml.snakeyaml.Yaml;

public class MessagePasser 
{
	/**
	 * key identifier for each message
	 * @author xincenh
	 *
	 */
	public class Key {
		String src;
		int seqNum;
		
		public Key(String s, int n) {
			this.src = s;
			this.seqNum = n;
		}
		
		public int getSeqNum() {
			return this.seqNum;
		}
		
		public String getSrc() {
			return this.getSrc();
		}
		
	}
	
	//Buffer that holds messages on client side 
	private LinkedList<TimeStampedMessage> out_buffer = new LinkedList<TimeStampedMessage>();    
	
	//Buffer that holds messages on receiver thread
	public static LinkedList<TimeStampedMessage> in_buffer = new LinkedList<TimeStampedMessage>();
	
	//All connections with other process
	private ArrayList<Connection> connList = new ArrayList<Connection>();
	private ArrayList<Rule> ruleList = new ArrayList<Rule>();
	
	//keep track of all groups, key:group name, value:all host
	private HashMap<String, ArrayList<String>> groups = new HashMap<String, ArrayList<String>>();
	
	//Hold all delayed message need to send out
	private ArrayList<TimeStampedMessage> delayedOutMsg = new ArrayList<TimeStampedMessage>();
	
	//Hold all delayed message on receiver side
	private ArrayList<TimeStampedMessage> delayedInMsg = new ArrayList<TimeStampedMessage>();
	
	//Hold all message has not been confirmed by the group other nodes
	private ArrayList<TimeStampedMessage> holdbackQueue = new ArrayList<TimeStampedMessage>();
	
	//Finally message delivered to application
	private LinkedList<TimeStampedMessage> deliveryQueue = new LinkedList<TimeStampedMessage>();
	
	//Holds multicast message
	private LinkedList<TimeStampedMessage> multicastSendQueue = new LinkedList<TimeStampedMessage>();
	private HashMap<TimeStampedMessage, TimeoutService>  timeoutServ = new HashMap<TimeStampedMessage, TimeoutService>();
	
	private HashMap<Integer, String> processes;
	
	private HashMap<String, ArrayList<Integer>> ACKMap = new HashMap<String, ArrayList<Integer>>();
	//private HashMap<String, ArrayList<Integer>> ACKMap = new HashMap<String, ArrayList<Integer>>();
	
	private HashMap<TimeStampedMessage, Boolean> blockingMsg = new HashMap<TimeStampedMessage, Boolean>();
	
	private TimeStamp systemTimeStamp, groupTimeStamp;
	private String localName;
	private String configFileName;
	public static int seqNum;
	private File configFile;
	private int nbrOfProcesses;	// number of process in the config file
	private long fileModTime;
	private int pid;
	
	public boolean enableTimeoutResend = false;
	
	public MessagePasser(String configuration_filename, String local_name)
	{
		configFileName = configuration_filename;
		localName = local_name;
		seqNum = 0;
	}
	
	public void initSystemTimeStamp(String service, int size)
	{
		systemTimeStamp = new TimeStamp(service, size);
	}
	
	private void updateSytemTimeStamp(TimeStampedMessage message, int src_pid, int dest_pid)
	{
		systemTimeStamp.updateTimeStamp(message.getTimeStamp().getTimeStamp(), src_pid, dest_pid);
	}
	
	private void incrementSystemTime()
	{
		try
		{
			if(systemTimeStamp.getClockServiceType().equalsIgnoreCase("vector"))
				systemTimeStamp.getTimeStamp().set(pid, systemTimeStamp.getTimeStamp().get(pid)+1);
			else   //logical
				systemTimeStamp.getTimeStamp().set(0, systemTimeStamp.getTimeStamp().get(0)+1);
		}
		catch(IndexOutOfBoundsException e)
		{
			System.out.println(pid);
			e.printStackTrace();
		}
	}
	/**
	 * 
	 * @param message
	 * @param rule_type
	 * @param delay_list
	 * @param buffer
	 * @param array
	 * @return applied rule type, if no rule applies, return "normal"
	 */
	private String processRules(TimeStampedMessage message, String rule_type,
			                 ArrayList<TimeStampedMessage> delay_list, LinkedList<TimeStampedMessage> buffer, ArrayList<TimeStampedMessage> array)
	{
		Rule rule; 
		TimeStampedMessage copy;
		String dest, src, kind, group_name;
		String group;
		int seqNum;
		Boolean duplicate;
		
		String applied_rule;
		for(int i=0; i < ruleList.size(); i++)
		{
			rule = ruleList.get(i);
			if(rule.getType().equalsIgnoreCase(rule_type))
			{
				dest = rule.getDest();
				src = rule.getSrc();
				kind = rule.getKind();
				group = rule.getGroupName();
				seqNum = rule.getSeqNum();
				duplicate = rule.getDuplicate();
				
				if(rule.getAction().equalsIgnoreCase("drop"))   
					{
					
						if((group == null || group.equalsIgnoreCase(message.getGroup())) &&
								(dest == null || message.getDest().equals(dest)) && (src == null || src.equalsIgnoreCase(message.getSrc()))
								&& (kind == null || kind.equalsIgnoreCase(message.getKind())) && (seqNum == -1 || seqNum == message.getSeqNum())
								&& (duplicate ==  false || duplicate.equals(message.getDuplicate())))
						{
							System.out.println(rule_type + " Message dropped");
							applied_rule = "drop";
							return applied_rule;		
						}
					}
				else if(rule.getAction().equalsIgnoreCase("delay"))
				{
					
					if((group == null || group.equalsIgnoreCase(message.getGroup())) &&
							(dest == null || message.getDest().equals(dest)) && (src == null || src.equalsIgnoreCase(message.getSrc()))
							&& (kind == null || kind.equalsIgnoreCase(message.getKind())) && (seqNum == -1 || seqNum == message.getSeqNum())
							&& (duplicate ==  false || duplicate.equals(message.getDuplicate())))
					{
						delay_list.add(message);
						
						System.out.println(rule_type + " Message delayed");
						applied_rule = "delay";
						return applied_rule;
					}
								
				}	
				else if(rule.getAction().equalsIgnoreCase("duplicate"))
				{
					
					if((group == null || group.equalsIgnoreCase(message.getGroup())) &&
							(dest == null || message.getDest().equals(dest)) && (src == null || src.equalsIgnoreCase(message.getSrc()))
							&& (kind == null || kind.equalsIgnoreCase(message.getKind())) && (seqNum == -1 || seqNum == message.getSeqNum())
							&& (duplicate ==  false || duplicate.equals(message.getDuplicate())))
					{
						System.out.println(rule_type + " Message duplicate");
						copy = message.clone();
						copy.setSrc(message.getSrc());
						copy.setDest(message.getDest());
						copy.setKind(message.getKind());
						copy.setData(message.getData());
						copy.setSeqNum(message.getSeqNum());
						copy.setDuplicate(true);
						
						if (rule_type.equalsIgnoreCase("send"))
						{
							buffer.add(message);
							buffer.add(copy);
							
							// add potentially delayed messages to out buffer
							for(int j = 0; j < delay_list.size(); j++) {
								buffer.add(delay_list.get(j));
								System.out.print("Send delayed message : ");
								delay_list.get(j).print();
							}
							delay_list.clear();
							
							applied_rule = "duplicate";
							return applied_rule;
						}
						else  // receive
						{
							array.add(message);
							array.add(copy);
								
							// add potentially delayed messages to out buffer
							for(int j = 0; j < delay_list.size(); j++) {
								array.add(delay_list.get(j));
							}
							delay_list.clear();
							
							array.trimToSize();
							applied_rule = "duplicate";
							return applied_rule;
						}
					}
				}
					
			}
		}
		
		/*
		 * do the following when no rule applies
		 */
		if(rule_type.equalsIgnoreCase("send"))
		{
			buffer.add(message);
			for(int i = 0; i < delay_list.size(); i++) {
				buffer.add(delay_list.get(i));        // moves potential delayed msg to buffer
				System.out.print("Send delayed message : ");
				delay_list.get(i).print();
			}
			delay_list.clear();
			//return null;
		}
		else   // receive
		{
			array.add(message);
			for(int j = 0; j < delay_list.size(); j++) {
				array.add(delay_list.get(j));
			}
			delay_list.clear();
			
			array.trimToSize();
			//return array;
		}
		
		applied_rule = "normal";
		return applied_rule;
	}
	
	private void checkConfigUpdates()
	{
		if(configFile.lastModified() > fileModTime)
		 {
			System.out.println("ConfigFile is modified");
			 try
			 {
				 parseConfig();
			 }
			 catch(FileNotFoundException e)
			 {
				 e.printStackTrace();
			 }
		 }
	}
	
	public void send(TimeStampedMessage message)
	{
		boolean flag = false;
		message.setSrc(localName);
		message.setOriginalSrc(localName);
		message.setSeqNum(++seqNum);
		
		System.out.println("------Send Message : " + 
				"dest: " + message.getDest() + "(" + message.getGroup() +
				"), SeqNum: " + message.getSeqNum() 
				+ ", timestamp " + message.getTimeStamp().getTimeStamp() + "-----");
		
		if(message.getGroup() != null || !(message.getGroup().equals("")))
		{
			//message.getDest().add(message.getSrc());    // add source to broadcast group
			//message.setOriginalSrc(message.getSrc());
			multicastSendQueue.add(message);
			
			//Start timer
			if (this.enableTimeoutResend) {
				TimeoutService ts = new TimeoutService(20, this, message);
				timeoutServ.put(message, ts);
			}
			
			if(message instanceof TimeStampedMessage)
			{
				incrementSystemTime();
			}
			
			//Also send itself a copy
			if (!this.holdbackQueue.contains(message)) {
				this.holdbackQueue.add(message);
			}
			processACK(message);
			
			sendMulticast(message, false);
		}
		else
		{
			for(Connection conn: connList) 
			{
				if(message.getDest().equals(conn.getName()))
				{
					message.setHostname(conn.getIP());
					message.setPort(conn.getPort());
					message.setOriginalSrc(message.getSrc());
					
					flag = true;
					break;
				}
			}
			
			if (!flag) 
			{
				System.out.println("No matching destination!");
				return;
			}
			
			if(message instanceof TimeStampedMessage)
			{
				incrementSystemTime();
			}
			
			checkConfigUpdates();
			
			processRules(message, "send", delayedOutMsg, out_buffer, null);
		}
	}
	
	public void sendMulticast(TimeStampedMessage message, boolean flag) 
	{
		//This is a message that not send to an entire group
		if (flag) {
			System.out.println("This is a message send to a single node");
			for(Connection conn: connList) 
			{
				if(message.getDest().equalsIgnoreCase(conn.getName()))
				{
					message.setHostname(conn.getIP());
					message.setPort(conn.getPort());
					message.setSrc(this.localName);
				
					checkConfigUpdates();
					processRules(message, "send", delayedOutMsg, out_buffer, null);
					System.out.println("Reply with the request message");
					message.print();
					return;
				}
			}
		}
		
		boolean send_success = false;
		System.out.println(localName + " send multicast " + message.getKind().toUpperCase() +  " message");
		String group = message.getGroup();
		for(String dest_name : this.groups.get(group))
		{	
			TimeStampedMessage copy = message.clone();
			
			// create entry in ACKMap for message
			if(!dest_name.equalsIgnoreCase(localName))     
			{
				flag = false;
				for(Connection conn: connList) 
				{
					if(dest_name.equalsIgnoreCase(conn.getName()))
					{
						copy.setHostname(conn.getIP());
						copy.setPort(conn.getPort());
						copy.setSrc(this.localName);
						copy.setDest(dest_name);
						flag = true;
						break;
					}
				}
				
				if (!flag) 
				{
					System.out.println("No matching destination for : " + dest_name);
				}
				else
				{

					send_success = true;
					checkConfigUpdates();
					
					processRules(copy, "send", delayedOutMsg, out_buffer, null);
				}
			}
		}
		
	}
	
	/*
	 * The receive() function should always return a single message;
	 */
	public TimeStampedMessage receive() {
		TimeStampedMessage msg  = null;
		synchronized(this.deliveryQueue) {
            if(this.deliveryQueue.isEmpty()) {
            	/*
                try {
                    this.deliveryQueue.wait();
                } catch (InterruptedException ex) {
                	ex.printStackTrace();
                    Logger.getLogger(MessagePasser.class.getName()).log(Level.SEVERE, null, ex);
                }*/
            	return null;
            }
            msg = this.deliveryQueue.removeFirst();            
            return msg;
        
        }
	}
	
	public void processInBuffer() {
		TimeStampedMessage message;
		ArrayList<TimeStampedMessage> array = new ArrayList<TimeStampedMessage>();
		
		if (in_buffer.isEmpty()) 
		{
			System.out.println("No more messages!");
		}
		
		while(!in_buffer.isEmpty()) {
			synchronized(in_buffer) {
				message = in_buffer.removeFirst();
			}
					
			if(message.getGroup() != null || !(message.getGroup().equals(""))) {
				
				receiveMulticast(message);
			}
			else
			{
				checkConfigUpdates();
			 	processRules(message, "receive", delayedInMsg, null, array);
			 	
			 	for (int i = 0; i < array.size(); i++) {
					this.addToDeliveryQueue(array.get(i));
				}
			}
			
		}
		
	}
	/*
	 *  This method processes received multicast messages.
	 *  When a message comes in, the passer checks the hold back queue
	 *  to see whether that message was previously received. If, yes
	 *  and this is the last packet, then the passer proceeds to forwarding the 
	 *  message sequence to a sorting queue, and eventually to the delivery queue.
	 *  Else, the message is re-broadcast to the group and added to the hold back queue 
	 */
	public void receiveMulticast(TimeStampedMessage message)
	{
		System.out.print("Receive multicast message");
		message.print();
		
		TimeStampedMessage ack, ts_msg;
		ArrayList<TimeStampedMessage> array = new ArrayList<TimeStampedMessage>();	//message & duplicate & previously delayed ones
		
		//First apply rules for received message
		checkConfigUpdates();
		String rule = processRules(message, "receive", delayedInMsg, null, array);
		//System.out.println("process multicast meesage");
		if (rule.equalsIgnoreCase("drop") || rule.equalsIgnoreCase("delay")) {
			System.out.print(rule + " message ");
			message.print();
			return;
		}
		
		// Need to process all message, including duplicate & previously delayed ones
		for (int i = 0; i < array.size(); i++) {
			TimeStampedMessage msg = array.get(i);
			System.out.print("-------- Receive message: ");
			msg.print();
			
			if(msg.getKind().equals("ACK")) {
				/* Process ACK message, if all ack are received, 
				/* will put corresponding message into deliveryQueue 
				 */
				processACK(msg);
			} else if (msg.getKind().equals("request")) {
				String origSrc = msg.getOriginalSrc();
				int seqNum = msg.getSeqNum();
				System.out.println(msg.getSrc() + " request for original message");
				for (int j = 0; j < holdbackQueue.size(); j++) {
					TimeStampedMessage m = holdbackQueue.get(j);
					if ((m.getSeqNum() == seqNum) &&
							(m.getOriginalSrc().equalsIgnoreCase(origSrc))) {
						
						TimeStampedMessage copy = m.clone();
						copy.setGroup(msg.getGroup());
						copy.setDest(msg.getSrc());
						copy.setSrc(localName);
						
						this.sendMulticast(copy, true);
						break;
					}
				}
			}
			 else
			 {
				 if(!msg.getOriginalSrc().equals(localName))       //  if local is not message original sender
				 {
					 //if(!isInHoldbackQueue(msg))
					 //{
						 
						 // send ack
						 ts_msg = msg;
						 
						 Key msg_key = new Key(msg.getOriginalSrc(), msg.getSeqNum());
						 // note that original msg seqNum is stored in ACK's data field
						 ack = new TimeStampedMessage(ts_msg.getGroup(), "ACK", msg.getSeqNum(), 
								 ts_msg.getMessageStart(), ts_msg.getMessageLength(), ts_msg.getTimeStamp().getClockServiceType(), 
								 nbrOfProcesses, systemTimeStamp);
						 
						 //Why increase by 1?
						 //ack.setSeqNum(ts_msg.getSeqNum() + 1);
						 ack.setSeqNum(msg.getSeqNum());
						 ack.setSrc(localName);
						 ack.setOriginalSrc(msg.getOriginalSrc());
						 ack.setGroup(msg.getGroup());
						 //ack.setDest(ts_msg.getDest());
						 
						 //update ACKMap
						 //updateACKMap(ack);
						 
						 sendMulticast(ack, false);
						 if(!isInHoldbackQueue(msg))
						 {
							 synchronized(holdbackQueue) {
								 holdbackQueue.add(msg);
							 }
							 processACK(msg);
						 }
					 
				 }
			 }
		}
		 //return holdbackQueue;
	}
	
	/*
	 * Checks whether a message is already in the hold back queue
	 */
	private boolean isInHoldbackQueue(TimeStampedMessage msg)
	{
		for(TimeStampedMessage m : holdbackQueue)
			if(m.compareTo(msg) == 0)
				return true;
		
		return false;
	}
	
	
	/*
	 *  Add a list of messages to the delivery queue in a sorted sequence
	 */
	private void addToDeliveryQueue(TimeStampedMessage msg)
	{
		// Only update time clock when add message to deliverQueue
		updateSytemTimeStamp(msg, getSrcPID(msg), pid);
		
		System.out.print("Insert to deliverQueue at: " + localName + getSystemTimeStamp().
		getTimeStamp());
		System.out.println(", Message: (" + msg.getOriginalSrc() +
				", " + msg.getSeqNum() + ", " + msg.getData() + ")");
		incrementSystemTime();
		 
		TimeStampedMessage ts_msg; 
		TimeStampedMessage m;
		boolean message_sorted;
		
		ts_msg = (TimeStampedMessage) msg;	
		synchronized(deliveryQueue) {
			if(deliveryQueue.size() == 0) {
					deliveryQueue.add(ts_msg);
			}
			else
			{
				if((ts_msg.getTimeStamp().getClockServiceType().
						equalsIgnoreCase("vector")))   // change to outer check
				{
					message_sorted = false;
					for(int j=0; j < deliveryQueue.size(); j++)
					{
						m = deliveryQueue.get(j);
						int compare = (((VectorClock) (((TimeStampedMessage) m).getTimeStamp().getClockService())).
						         compareTo((VectorClock)(ts_msg.getTimeStamp().getClockService())));
						System.out.println("compare number is: " + compare);
						if(compare == 0)
						{
							deliveryQueue.add(ts_msg);
							message_sorted = true;
							break;
						}
						else if(compare  == 1 || compare  == 2)
						{
							deliveryQueue.add(j, ts_msg);
							message_sorted = true;
							break;
						}
					}
					if(!message_sorted)  // if not equal or less then append end of queue
						deliveryQueue.add(ts_msg);					
				}
			}
		}
	}
	/*
	 *  Update the map if it already has an entry for the message,
	 *  else create one before updating it 
	 */
    private void updateACKMap(TimeStampedMessage message)
    {
    	String msg_key = new String(message.getOriginalSrc()+ message.getSeqNum());
    	ArrayList<Integer> ACKList = null;
    	for (String key: ACKMap.keySet()) {
    		if (key.equals(msg_key)) {
    			ACKList = ACKMap.get(key);
    			break;
    		}
    	}
    	
    	int srcPid = getProcessPID(message.getSrc());
    	int origPid = getProcessPID(message.getOriginalSrc());
    	
    	if (ACKList != null) {
    		ACKList.set(srcPid, 1);
    		if (!message.kind.equalsIgnoreCase("ACK")) {
    			ACKList.set(origPid, 1);
    		}
    		//System.out.println("set ACKMap as: " + msg_key + " " + ACKList);
    	}
    	else
    	{
    		ACKList = new ArrayList<Integer>();
    		for (int i = 0; i < nbrOfProcesses; i++) {
    			ACKList.add(0);
    		}
    		ACKList.set(srcPid, 1);	// This could not set value at index_pid, out_of_size error!
    		if (!message.kind.equalsIgnoreCase("ACK")) {
    			ACKList.set(origPid, 1);
    		}
    		ACKMap.put(msg_key, ACKList);
    		//System.out.println("set ACKMap as: " + msg_key + " " + ACKList);
    		
    	}
    }
    
    /* 
     * Process ACK message, if all ack are received, 
     * will put corresponding message into deliveryQueue 
	 */
	
	private void processACK(TimeStampedMessage ack_msg)
	{
		System.out.println("Proccess ACK");
		String msg_key = new String(ack_msg.getOriginalSrc()+ ack_msg.getSeqNum());
		
		boolean checkACKs = false;
		
		updateACKMap(ack_msg);
		
		ArrayList<Integer> ACKList = null;
		String actual_key = null;
    	for (String key: ACKMap.keySet()) {
    		//System.out.println(key + ACKMap.get(key));
    		if (key.equals(msg_key)) {
    			actual_key = key;
    			ACKList = ACKMap.get(key);
    			break;
    		}
    	}
    	
    	System.out.println("Actual key: " + actual_key + ACKList);
		String groupname = ack_msg.getGroup();
		ArrayList<String> grp_members = this.groups.get(groupname);
		int dest_pid;
		
		if (ACKList == null) {
			return;
		}
		//If doesn't receive message from original source, request message 
		int src_pid = getProcessPID(ack_msg.getOriginalSrc());
		if (ACKList.get(src_pid) != 1) {
			TimeStampedMessage request = new TimeStampedMessage(groupname, "request",
				ack_msg.getOriginalSrc(), ack_msg.getMessageStart(), ack_msg.getMessageLength(), this.systemTimeStamp);
			request.setSeqNum(ack_msg.getSeqNum());
			request.setOriginalSrc(ack_msg.getOriginalSrc());
			request.setSrc(localName);
			System.out.println("Request for original message");
			sendMulticast(request, false);
		}
		
		// check whether all ACKs are in 
		for(int i=0; i<grp_members.size(); i++)
		{	
			dest_pid = getProcessPID((String)grp_members.get(i));
		
			if (dest_pid != pid) {
				//System.out.println("dest_pid: " + dest_pid + "; msg_length: "+ ack_msg.getMessageLength());
				if(ACKList.get(dest_pid) == ack_msg.getMessageLength())
					checkACKs = true;
				else
				{
					checkACKs = false;
					break;
				}
			}
		}
		System.out.println("check ACK for " + actual_key +" is " + checkACKs);
		if(checkACKs)
		{
			//System.out.println("All ACK received.");
			if(ack_msg.getOriginalSrc().equalsIgnoreCase(localName))      // if source
			{
				// remove message from multicast send Q
				for(TimeStampedMessage message : multicastSendQueue)
					if(message.getSrc().equalsIgnoreCase(ack_msg.getOriginalSrc()) && message.getSeqNum() == ack_msg.getSeqNum())	
					{
						//if(message.getSrc().equalsIgnoreCase(msg.getOriginalSrc()) && message.getSeqNum() == (Integer)msg.getData())	
						System.out.println("Sender remove message from sendQueue");
						multicastSendQueue.remove(message);
						if (this.enableTimeoutResend) {
							timeoutServ.get(message).cancel();
							timeoutServ.remove(message);
						}
						ACKMap.remove(actual_key);
						break;
						
					}
			}
			
			boolean deliverFlag = false;
			System.out.println("holdbackQueue size is: " + this.holdbackQueue.size());
			for (int i = 0; i < holdbackQueue.size(); i++) {
				TimeStampedMessage m = holdbackQueue.get(i);
				m.print();
				if ((m.getSeqNum() == ack_msg.getSeqNum()) &&
						(m.getOriginalSrc().equalsIgnoreCase(ack_msg.getOriginalSrc()))) {
					System.out.print(i + "th message is correct:  ");
					m.print();
					
					boolean haveWait = false;
					
					for (int j = 0; j< holdbackQueue.size(); j++ ){
						TimeStampedMessage other = holdbackQueue.get(j);
						if (j != i && other.getGroup().equals(m.getGroup())) {
							int compare = (((VectorClock) (m.getTimeStamp().getClockService())).
							         compareTo((VectorClock)other.getTimeStamp().getClockService()));
							if (compare == 1 || compare == 2) {
								
								System.out.print("Blocked by message ");
								other.print();
								
								haveWait = true;
								break;
							}
						}
					}
					if (!haveWait) {
						addToDeliveryQueue(m);
						holdbackQueue.remove(i);
						deliverFlag = true;
						ACKMap.remove(actual_key);
						System.out.println("having " + this.blockingMsg.size() +" message blocking");
						for (TimeStampedMessage other : this.blockingMsg.keySet()) {

							if (other.getGroup().equals(m.getGroup()) && this.blockingMsg.get(other)) {
									System.out.print("releasing blocking message: ");
									other.print();
									
									addToDeliveryQueue(other);
									this.blockingMsg.put(other, false);
								}
							}
						break;
					}else {
						holdbackQueue.remove(i);
						this.blockingMsg.put(m, true);
						ACKMap.remove(actual_key);
					}
				}
			}
			
			if (!deliverFlag) {
				System.out.println("nothing to deliver");
			}
			
		}	
	}
	
	/*
	 * Getters
	 */
	public int getPid()
	{
		return pid;
	}
	
	public LinkedList<TimeStampedMessage> getOutBuffer()
	{
		return out_buffer;
	}
	
	public LinkedList<TimeStampedMessage> getInBuffer()
	{
		return in_buffer;
	}
	
	public ArrayList<Connection> getConnList()
	{
		return connList;
	}
	
	public HashMap<Integer, String> getProcesses()
	{
		return processes;
	}
	
	public String getLocalName()
	{
		return localName;
	}
	
	public int getNbrOfProcesses()
	{
		return nbrOfProcesses;
	}
	
	public TimeStamp getSystemTimeStamp()
	{
		return systemTimeStamp;
	}
	
	public void setPid()
	{
		/*
		 * Assign pid
		 */
		for(int i = 0; i < nbrOfProcesses; i++)
			if(processes.get(i).equalsIgnoreCase(localName))
			{
				pid = i;
				break;
			}
	}
	
	/*
	 * Return the PID of the process that sent the message
	 * 
	 * @return an integer
	 */
	public int getSrcPID(TimeStampedMessage msg)
	{ 
		int src_pid = -1;
		
		for(int i = 0; i < nbrOfProcesses; i++)
			if(processes.get(i).equalsIgnoreCase(msg.getSrc()))
			{
				src_pid = i;
				break;
			}
		
		return src_pid;
	}
	
	/*
	 * Return a member PID
	 * 
	 * @return an integer
	 */
	public int getProcessPID(String process_name)
	{ 
		int pid = -1;
		
		for(int i = 0; i < nbrOfProcesses; i++)
			if(processes.get(i).equalsIgnoreCase(process_name))
			{
				pid = i;
				break;
			}
		
		return pid;
	}
	
	
	@SuppressWarnings("unchecked")
	public void parseConfig() throws FileNotFoundException 
	{
		//configFile = new File ("C:/Users/YAYA/workspace_1/DS_LAB2/src/configuration.yaml"); 
		configFile = new File (configFileName); 
		fileModTime = configFile.lastModified();
	    InputStream input = new FileInputStream(configFile);
	    Yaml yaml = new Yaml();
	    
	    connList = new ArrayList<Connection> ();
	    ruleList = new ArrayList<Rule>();
	    groups = new HashMap<String, ArrayList<String>>();
		Map<String, Map> map = yaml.loadAs(input, Map.class);
		Iterator iterator  = map.keySet().iterator();
		String temp;	
		ArrayList list;
		Connection conn;
		Rule rule;
		
		while(iterator.hasNext())
		{
			//System.out.println(iterator.next().getClass());
			temp = (String) iterator.next();
			if(temp.equalsIgnoreCase("groups"))
			{
				list = (ArrayList) map.get(temp);
				for(int i=0; i<list.size(); i++)
					groups.put(((LinkedHashMap<String, String>)list.get(i)).get("name"), 
							((LinkedHashMap<String, ArrayList<String>>)list.get(i)).get("members"));
				//System.out.println(groups.get("Group1"));
			}
			else if(temp.equalsIgnoreCase("configuration"))
			{
				list = (ArrayList) map.get(temp);
				nbrOfProcesses = list.size();
				processes = new HashMap<Integer, String>(nbrOfProcesses);
				
				//System.out.println(((LinkedHashMap<String, String>) list.get(0)).get("name"));
				for(int i = 0; i < nbrOfProcesses; i++)
				{
					conn = new Connection("", "", 0);
					conn.setIP(((LinkedHashMap<String, String>)list.get(i)).get("ip"));
					conn.setPort((int)((LinkedHashMap) list.get(i)).get("port")); 
					conn.setName(((LinkedHashMap<String, String>) list.get(i)).get("name"));
					connList.add(conn);
					processes.put(i, conn.getName());
				}
				connList.trimToSize();
				//System.out.println(msgList.size());
			}
			else 
			{
				//System.out.println(map.get(temp).getClass());
				
				list = (ArrayList) map.get(temp);
				for(int i = 0; i < list.size(); i++)
				{
					rule = new Rule();
					
					if(temp.equalsIgnoreCase("sendRules"))
						rule.setType("send");
					else
						rule.setType("receive");
					
					rule.setAction(((LinkedHashMap<String, String>)list.get(i)).get("action"));
					
					if(((LinkedHashMap<String, String>)list.get(i)).containsKey("src"))
						rule.setSrc(((LinkedHashMap<String, String>)list.get(i)).get("src"));
					
					if(((LinkedHashMap<String, String>)list.get(i)).containsKey("dest"))
						rule.setDest(((LinkedHashMap<String, String>)list.get(i)).get("dest"));
					
					if(((LinkedHashMap<String, String>)list.get(i)).containsKey("kind"))
						rule.setKind(((LinkedHashMap<String, String>)list.get(i)).get("kind"));
					
					if(((LinkedHashMap)list.get(i)).containsKey("seqNum"))
						rule.setseqNum((int)((LinkedHashMap)list.get(i)).get("seqNum"));
					
					if(((LinkedHashMap<String, Boolean>)list.get(i)).containsKey("duplicate"))
						rule.setDuplicate(((LinkedHashMap<String, Boolean>)list.get(i)).get("duplicate"));
					
					if(((LinkedHashMap<String, String>)list.get(i)).containsKey("groupName"))
						rule.setGroupName(((LinkedHashMap<String, String>)list.get(i)).get("groupName"));
				
					ruleList.add(rule);
				}
				ruleList.trimToSize();
			}			
		}
	}	
}

class Connection
{
    private String name;
    private String ip;
    private int port;

    public Connection(String name, String ip, int port)
    {
        this.name = name;
        this.ip = ip;
        this.port = port;
    }

    public String getName() 
    {
        return name;
    }

    public void setName(String name) 
    {
        this.name = name;
    }

    public String getIP() 
    {
        return ip;
    }

    public void setIP(String ip) 
    {
        this.ip = ip;
    }

    public int getPort()
    {
    	return port;
    }
    
    public void setPort(int prt)
    {
    	port = prt;
    }
} 
