package edu.tamu.lenss.MDFS.Utils;

import java.io.File;
import android.os.Environment;

public class AndroidIOUtils {


	//Create a File handler to the specified path on SD Card
	public static File getExternalFile(String path) { return new File(Environment.getExternalStorageDirectory(), path); }  //Isagor0!



}
