package edu.tamu.lenss.MDFS;

import android.os.Environment;

import java.io.File;

//all final variable must remain final but all non-final variables are allowed to be changed programmatically.
public class Constants {

	//checks
	public static final boolean CHECK_MAX_FILE_SIZE_LIMIT = false;
	public static final boolean CHECK_MAX_BLOCK_COUNT = false;


	//file/block size and count parameters
	public static final long MAX_FILE_SIZE = 133994489;
	public static final int MAX_BLOCK_SIZE_IN_MB = 50;
	public static final int DEFAULT_BLOCK_SIZE_IN_MB = 1;
	public static int BLOCK_SIZE_IN_MB = DEFAULT_BLOCK_SIZE_IN_MB;


	//directory parameters
	public static final String MDFS_LOG_PATH = Environment.getExternalStorageDirectory() + "/distressnet/MDFS/mdfs_log.log";
	public static final String ANDROID_DIR_ROOT = "MDFS";
	public static final String ANDROID_DIR_CACHE = ANDROID_DIR_ROOT + File.separator + "cache";
	public static final String DEFAULT_DECRYPTION_FOLDER_NAME = "decrypted";  					//decrypted folder in sdcard
    public static String DECRYPTION_FOLDER_NAME = DEFAULT_DECRYPTION_FOLDER_NAME;

	//encoding parameters
	public static final double DEFAULT_K_N_RATIO = 0.5;
	public static final int MAX_N_VAL = 15;
	public static final int DEFAULT_K_VALUE = -1;	//-1 means default which is determined by an equation in put class.
	public static int K_VALUE = DEFAULT_K_VALUE;

	//boolean parameters
    public static boolean metadataIsGlobal = true;

    //misc
    public final static String FILE_RETRIEVE_NOTIFICATION = "FILE_RETRIEVE_NOTIFICATION";
	public static final String RSOCK_CLOSED = "RSOCK_CLOSED" ;
	public static final String EDGEKEEPER_CLOSED = "EDGEKEEPER_CLOSED";
	public static final String NON_CLI_CLIENT = "NONCLICLIENT";


}
