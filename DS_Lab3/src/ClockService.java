import java.util.ArrayList;


public interface ClockService
{
	public ArrayList<Integer> getTimeStamp();
	public void updateTimeStamp(ArrayList<Integer> list, int src_pid, int dest_pid);
	public String toString();
}
