import java.io.Serializable;
import java.util.ArrayList;

public class TimeStamp implements Serializable
{
	private ClockService cs;
	private String service;
	
	public TimeStamp(String service, int size)
	{
		this.service = service;
		cs = ClockServiceFactory.getClockService(service, size);
	}

	public ArrayList<Integer> getTimeStamp()
	{
		return cs.getTimeStamp();
	}
	
	public void updateTimeStamp(ArrayList<Integer> list, int src_pid, int dest_pid)
	{
		cs.updateTimeStamp(list, src_pid, dest_pid);
	}
	
	public String getClockServiceType()
	{
		return service;
	}
	
	public ClockService getClockService()
	{
		return cs;
	}
}
