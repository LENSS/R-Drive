package edu.tamu.lenss.mdfs.utils;

import java.util.concurrent.Callable;

/**
 * This class calls back when competes. 
 * @author Jay
 * @param <T1>	A genetic type for return value
 */
public class CallableTask<T1 extends Object> implements Callable<T1> {
	private final Callable<T1> task;
	private final CallableCallback<T1> callback;
	private T1 result;
	public CallableTask(Callable<T1> task, CallableCallback<T1> callback) {
	    this.task = task;
	    this.callback = callback;
	  }

	@Override
	public T1 call() throws Exception {
		result = task.call();
		callback.complete(result);
		return result;
	}
	
	public T1 getResult(){
		return result;
	}
	
	public static interface CallableCallback<T extends Object> {
		/**
		 * This is a callback function called by the executing thread
		 * @param result
		 */
		public void complete(T result);
	}

}
