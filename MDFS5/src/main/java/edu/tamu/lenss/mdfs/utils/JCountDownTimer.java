package edu.tamu.lenss.mdfs.utils;

import java.util.Timer;
import java.util.TimerTask;

/**
 * This class imitates the functionalities of Android CountDownTimer, but use
 * only the naive Java API.
 * 
 * @author Jay
 * 
 */
public abstract class JCountDownTimer {
	private Timer timer;
	private TimerTask task;

	private long startT, finishT = Long.MAX_VALUE;
	private long period, duration;

	public  JCountDownTimer(long millisInFuture, long countDownInterval) {
		this.period = countDownInterval;
		this.duration = millisInFuture;
		this.timer = new Timer();
	}

	/**
	 * The timer can only be started once. Successive calls have no effects. <BR>
	 * Use cancel() and start() to reset the timer.
	 * @return
	 */
	public final synchronized JCountDownTimer start() {
		if(task != null)
			return this;
		this.startT = System.currentTimeMillis();
		this.finishT = startT + duration;
		task = getTask();
		timer.scheduleAtFixedRate(task, 0, period);
		return this;
	}

	public final synchronized void cancel() {
		if(task != null)
			task.cancel();
		task = null;
	}
	
	private TimerTask getTask(){
		return new TimerTask() {
			@Override
			public void run() {
				long curTime = System.currentTimeMillis();
				if (curTime < finishT) {
					onTick(finishT - curTime);
				} else {
					onFinish();
					cancel();
				}
			}
		};
	}

	public abstract void onFinish();
	public abstract void onTick(long millisUntilFinished);

}
