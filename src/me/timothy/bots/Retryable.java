package me.timothy.bots;

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
	
	/** The logger. */
	private Logger logger;
	
	/** The name. */
	private String name;
	
	/**
	 * Instantiates a new retryable.
	 *
	 * @param name the name
	 */
	public Retryable(String name) {
		this.name = name;
		
		logger = LogManager.getLogger();
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
			}
			if(result != null)
				return result;
			onFailure();
			times++;
			long sleepTime = (long) (duration * Math.pow(2, times));
			
			logger.debug(name + " failed (#" + times + "); retrying in " + sleepTime);
			try {
				Thread.sleep(sleepTime);
			}catch(InterruptedException ex) {
				throw new RuntimeException(ex);
			}
		}while(true);
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
