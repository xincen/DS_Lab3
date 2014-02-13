import java.io.Serializable;
import java.util.ArrayList;


public class Message implements Serializable, Comparable
{
	//private ArrayList<String> dest;
	private String dest;
	private String group;
	private String src;
	public int seqNum, start_num, length;
	public boolean duplicate;
	public String kind;
	public Object data;
	public int port;
	public String hostname, originalDest, loggedRule, originalSrc;
	
	/**
	 * key identifier for each message
	 * @author xincenh
	 *
	 */
	
	/*
	 * Constructor
	 */
	public Message(String dest, String kind, Object data, int start, int lenth)
	{
		if (dest.contains("Group")) {
			this.group = dest;
		} else {
			this.dest = dest;
			this.group = null;
		}
		
		this.kind = kind;
		this.data = data;
		//dest = new ArrayList<String>();
		seqNum = 0;
		start_num = start;
		this.length = lenth;
		duplicate = false;
		originalDest = null;
		originalSrc = null;
	}
	
	/*
	 * Getters
	 */
	public String getDest()
	{
		return dest;
	}
	
	public String getOriginalSrc()
	{
		return originalSrc;
	}
	
	public String getOriginalDest()
	{
		return originalDest;
	}
	
	public String getLoggedRule()
	{
		return loggedRule;
	}
	
	public String getSrc()
	{
		return src;
	}
	
	public String getKind()
	{
		return kind;
	}
	
	public int getSeqNum()
	{
		return seqNum;
		
	}
	
	public boolean getDuplicate()
	{
		return duplicate;
	}
	
	public Object getData()
	{
		return data;
	}
	
	public String getHostName()
	{
		return hostname;
	}
	
	public int getPort()
	{
		return port;
	}
	
	public String getGroup()
	{
		return group;
	}
	
	public int getMessageStart()
	{
		return start_num;
	}
	
	public int getMessageLength()
	{
		return length;
	}
	
	
	/*
	 * Setters
	 */
	public void setDest(String dest)
	{
		/*
		for(int i=0; i <dest.size(); i++)
			this.dest.add(dest.get(i));
			*/
		this.dest = dest;
	}
	
	public void setOriginalSrc(String od)
	{
		originalSrc = od;
	}
	
	public void setOriginalDest(String od)
	{
		originalDest = od;
	}
	
	public void setLoggedRule(String rule)
	{
		loggedRule = rule;
	}
	
	public void setSrc(String src)
	{
		this.src = src;
	}
	
	public void setSeqNum(int num)
	{
		seqNum = num;
	}
	
	public void setDuplicate(boolean dupl)
	{
		duplicate = dupl;
	}
	
	public void setKind(String kind)
	{
		this.kind = kind;
	}
	
	public void setData(Object data)
	{
		this.data = data;
	}
	
	public void setHostname(String name)
	{
		hostname = name;
	}
	
	public void setPort(int port)
	{
		this.port = port;
	}
	
	public void setGroup(String group)
	{
		this.group = group;
	}
	
	public void print() {
		System.out.println( "#" + this.getSeqNum() + " (" + this.getKind() +
				"), src: " + this.src + " (orig: " + this.originalSrc + 
				"), dest: " + this.getDest() + " (" + this.getGroup() +")" + 
				"data: " + this.getData());
	}
	/*
	 * Check whether a node is included in the destination list
	 */
	/*
	public boolean includeDest(String host)
	{
		for(int i=0; i<dest.size(); i++)
			if(host.equalsIgnoreCase(dest.get(i)))
				return true;
		
		return false;
	}*/
	
	/*
	 * We consider that two messages are equal if they have the same OriginalSrc and seqNum
	 * by definition, this combination uniquely identifies a message...
	 * 
	 * If two messages are equal, ret 0 else, ret -1
	 */
	public int compareTo(Object msg)
	{
		if(this.originalSrc.equalsIgnoreCase(((Message) msg).getOriginalSrc()) && seqNum == ((Message) msg).getSeqNum())
			return 0;
		else
			return -1;
	}
	
	
	public Message clone()  {
		Message copy = new Message(this.group,this.kind, this.getData(), this.start_num, this.length);
		copy.setDest(this.getDest());
		copy.setSrc(this.getSrc());
		copy.setSeqNum(this.getSeqNum());
		copy.setDuplicate(this.duplicate);
		copy.setPort(this.port);
		copy.setHostname(this.hostname);
		copy.setOriginalDest(this.originalDest);
		copy.setOriginalSrc(this.originalSrc);
		copy.setLoggedRule(this.getLoggedRule());
		return copy;
	}
}
