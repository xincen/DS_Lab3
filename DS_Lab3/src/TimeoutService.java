
import java.util.Timer;
import java.util.TimerTask;

/*
 * 
 */
public class TimeoutService {
	Timer timer;
	private MessagePasser mpasser;
	private TimeStampedMessage message;
	
	public TimeoutService(int seconds, MessagePasser mp, TimeStampedMessage m) {
		timer = new Timer(); 
		mpasser = mp;
		message = m;
		timer.schedule(new TimeoutTask(), seconds*1000, seconds*1000);
	}
	
	public void cancel() {
		timer.cancel();
		System.out.println("/***** Timer is canceled for " + message.originalSrc 
				+ message.getSeqNum() + " ****/");
	}
	
	class TimeoutTask extends TimerTask {
		public void run() {
			System.out.println("/******Time is up, resend group message!******/");
			message.print();
			// Not complete yet.
			mpasser.sendMulticast(message, false);
		}
	}
}