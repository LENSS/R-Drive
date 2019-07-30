package edu.tamu.lenss.mdfs.utils;

import android.util.Log;


public final class Logger {
	
	private static java.util.logging.Logger JLOGGER = java.util.logging.Logger.getLogger("InfoLogging");

	public static final void d(String tag, String msg) {
		Log.d(tag, msg);
		//JLOGGER.log(Level.FINEST, msg);
		System.out.println(tag + ": " + msg);
	}

	public static final void i(String tag, String msg) {
		Log.i(tag, msg);
		//JLOGGER.log(Level.INFO, msg);
		System.out.println(tag + ": " + msg);
	}

	public static final void e(String tag, String msg) {
		Log.e(tag, msg);
		//JLOGGER.log(Level.SEVERE, msg);
		System.err.println(tag + ": " + msg);
	}

	public static final void v(String tag, String msg) {
		Log.v(tag, msg);
		//JLOGGER.log(Level.FINE, msg);
		System.out.println(tag + ": " + msg);
	}

	public static final void w(String tag, String msg) {
		Log.w(tag, msg);
		//JLOGGER.log(Level.WARNING, msg);
		System.err.println(tag + ": " + msg);
	}
}
