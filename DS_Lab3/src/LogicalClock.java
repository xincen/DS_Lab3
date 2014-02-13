import java.io.Serializable;
import java.util.ArrayList;


public class LogicalClock implements ClockService, Serializable
{
	private ArrayList<Integer> timeStamp;
	
	public LogicalClock()
	{
		timeStamp = new ArrayList<Integer>(1);
		timeStamp.add(0);
	}
	
	public ArrayList<Integer> getTimeStamp()
	{
		return timeStamp;
	}
	
	public void updateTimeStamp(ArrayList<Integer> timeStamps2, int src_pid, int dest_pid)
	{
		int time1 = timeStamp.get(0);
		int time2 = timeStamps2.get(0);
		
		if (time2 >= time1) 
			timeStamp.set(0, time2 +1);
	}
	
	public String toString()
	{
		String timeStamp = "(";
		
		int size = this.timeStamp.size();
		
		for(int i = 0; i < size; i++)
			if (i == size)
				timeStamp += this.timeStamp.get(i) + ")";
			else
				timeStamp += this.timeStamp.get(i) + ", ";
		
		return timeStamp;
	}
}
