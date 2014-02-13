
public class Rule
{
	private String type;
	private String action;
	private String src;
	private String dest;
	private String kind;
	private String group_name;
	private Boolean duplicate;
	private int seqNum;
	
	public Rule()
	{
		type = null;
		action = null;
		src = null;
		dest = null;
		kind = null;
		group_name = null;
		duplicate = false;
		seqNum = -1;
	}
	
	public String getType()
	{
		return type;
	}
	
	public String getAction()
	{
		return action;
	}
	
	public String getSrc()
	{
		return src;
	}
	
	public String getDest()
	{
		return dest;
	}
	
	public String getKind()
	{
		return kind;
	}
	
	public String getGroupName()
	{
		return group_name;
	}
	
	public int getSeqNum()
	{
		return seqNum;
	}
	
	public Boolean getDuplicate()
	{
		return duplicate;
	}
	
	public void setType(String t)
	{
		type = t;
	}
	
	public void setDest(String t)
	{
		dest = t;
	}
	
	public void setSrc(String t)
	{
		src = t;
	}
	
	public void setKind(String t)
	{
		kind = t;
	}
	
	public void setGroupName(String t)
	{
		group_name = t;
	}
	
	public void setseqNum(int t)
	{
		seqNum = t;
	}
	
	public void setAction(String action)
	{
		this.action = action;
	}
	
	public void setDuplicate(Boolean flag)
	{
		duplicate = flag;
	}
}