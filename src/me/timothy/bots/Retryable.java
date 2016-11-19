package me.timothy.bots;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

// TODO: Auto-generated Javadoc
/**
 * Meant to be used in anonymous classes, this
 * retries {@code runImpl} by means of exponential back-off.
 *
 * @author Timothy
 * @param <T> what is returned
 */
public abstract class Retryable<T> {
	private static final Random RANDOM = new Random();
	
	/**
	 * Marks that this should fail on exceptions by
	 * propagating said exception. 
	 */
	public static final short FAIL_ON_EXCEPTION = 0;

	/** The logger. */
	private Logger logger;
	
	/** The name. */
	private String name;
	
	private short[] params;
	
	/**
	 * The list of runnables that are called in the event of failure. 
	 * These should help protect this retryable against state that 
	 * is time-sensitive, such as an oauth cookie falling out of date.
	 * 
	 * Called in addition to onFailure()
	 * 
	 * May be null.
	 */
	private List<Runnable> failCallbacks;
	
	/**
	 * Instantiates a new retryable.
	 *
	 * @param name the name of the action being attempted, for debugging
	 * @param failCallbacks callbacks in the event of failure. may be null
	 * @param params optional constants that affect the behavior of the retryable
	 */
	public Retryable(String name, List<Runnable> failCallbacks, short... params) {
		if(failCallbacks != null && failCallbacks.isEmpty())
			failCallbacks = null;
		
		this.name = name;
		this.failCallbacks = failCallbacks;
		this.params = params;
		
		logger = LogManager.getLogger();
	}
	
	/**
	 * Convenience constructor for retryables that have exactly one fail
	 * callback.
	 * @param name the name of the action being attempted, for debugging
	 * @param failCallback the callback in the event of failure. May be null
	 * @param params optional constants that affect the behavior of the retryable
	 */
	public Retryable(String name, Runnable failCallback, short... params)
	{
		List<Runnable> failCallbacks = null;
		if(failCallback != null) {
			failCallbacks = new ArrayList<>(1);
			failCallbacks.add(failCallback);
		}
		
		this.name = name;
		this.failCallbacks = failCallbacks;
		this.params = params;
		
		logger = LogManager.getLogger();
	}
	
	/**
	 * Convenience constructor for retryables with no runnable failure 
	 * callbacks.
	 * 
	 * @param name the name of the action being attempted, for debugging
	 * @param params optional constants that affect the behavior of the retryable
	 */
	public Retryable(String name, short... params)
	{
		this(name, (List<Runnable>)null, params);
	}
	
	/**
	 * Runs runImpl at most 10 times or until the result is non-null.
	 * 
	 * @return runImpl's non-null result upon success, null on failure
	 */
	public T run() {
		int duration = 10000, times = 0;
		T result = null;
		do {
			try {
				result = runImpl();
			} catch (Exception e) {
				e.printStackTrace();
				for(short s : params) {
					if(s == FAIL_ON_EXCEPTION)
						return null;
				}
			}
			if(result != null)
				return result;
			onFailure();
			callFailureCallbacks();
			times++;
			long sleepTime = (long) (duration * Math.pow(2, RANDOM.nextInt(times)));
			if(sleepTime > 1000 * 60 * 30) {
				sleepTime = 1000 * 60 * 30; // 30 minutes
			}
			
			logger.debug(name + " failed (#" + times + "); retrying in " + sleepTime);
			try {
				Thread.sleep(sleepTime);
			}catch(InterruptedException ex) {
				throw new RuntimeException(ex);
			}
		}while(true);
	}
	
	private void callFailureCallbacks()
	{
		if(failCallbacks != null) {
			for(Runnable r : failCallbacks) {
				r.run();
			}
		}
	}
	
	/**
	 * Run impl.
	 *
	 * @return the t
	 */
	protected abstract T runImpl() throws Exception;
	
	/**
	 * Called after an exception is thrown or null is returned
	 * from runImpl
	 */
	protected void onFailure() {};
}
