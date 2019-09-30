package edu.tamu.lenss.mdfs.Utils;

public class MyTextUtils {
	public static boolean isEmpty(String str){
		if(str != null && str.trim().length() > 0)
			return false;
		else
			return true;
		
	}
	
	public static boolean isNumeric(String str) {
		try {
			Integer.parseInt(str);
		} catch (NumberFormatException nfe) {
			return false;
		}
		return true;
	}
}
