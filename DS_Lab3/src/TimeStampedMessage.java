import java.io.Serializable;
import java.util.ArrayList;


public class TimeStampedMessage extends Message implements Serializable
{
	private TimeStamp timeStamp, logsTimeStamp;
	
	public TimeStampedMessage(String group, String kind, Object data, int start, int length, 
			TimeStamp ts)
	{
		super(group, kind, data, start, length);
		timeStamp = new TimeStamp(ts.getClockServiceType(), ts.getTimeStamp().size());	
		logsTimeStamp = new TimeStamp(ts.getClockServiceType(), ts.getTimeStamp().size());
		setTimeStamp(ts);
	}
	
	public TimeStampedMessage(String group, String kind, Object data, int start, int length,
								String clock_service, int size, TimeStamp ts)
	{
		super(group, kind, data, start, length);
		
		timeStamp = new TimeStamp(clock_service, size);	
		logsTimeStamp = new TimeStamp(clock_service, size);
		setTimeStamp(ts);
	}
	
	public TimeStamp getTimeStamp()
	{
		return timeStamp;
	}
	
	public TimeStamp getLogsTimeStamp()
	{
		return logsTimeStamp;
	}
	
	public void setTimeStamp(TimeStamp ts)
	{
		for(int i = 0; i <timeStamp.getTimeStamp().size(); i++)
			timeStamp.getTimeStamp().set(i, ts.getTimeStamp().get(i));
	}
	
	public void setLogsTimeStamp(TimeStamp ts)
	{
		for(int i = 0; i <logsTimeStamp.getTimeStamp().size(); i++)
			logsTimeStamp.getTimeStamp().set(i, ts.getTimeStamp().get(i));
	}
	
	public TimeStampedMessage clone()  {
		TimeStampedMessage copy = new TimeStampedMessage(this.getGroup(), this.getKind(), 
				this.getData(), this.start_num, this.length, this.getTimeStamp());
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
