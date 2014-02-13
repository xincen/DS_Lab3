
public class ClockServiceFactory 
{
	public static ClockService getClockService(String service, int size)
	{
		if(service.equalsIgnoreCase("logical"))
			return new LogicalClock();
		else if(service.equalsIgnoreCase("vector"))
			return new VectorClock(size);
		
		return null;
	}
}
