package edu.tamu.cse.lenss.MDFS5.Utils;

import android.os.Environment;

import java.io.File;

public class AndroidIOUtils {


	//Create a File handler to the specified path on SD Card
	public static File getExternalFile(String path) {

		File f = new File(Environment.getExternalStorageDirectory(), path); //Isagor0!
		return f;
	}




}
