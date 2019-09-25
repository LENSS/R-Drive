package edu.tamu.lenss.mdfs;

import java.io.File;

public class Constants {

	public static final long FRAGMENT_RETRIEVAL_TIMEOUT_INTERVAL = 10000 * 6;		//Timeout of retrieving file fragments, after this time a thread stops trying
	public static final int FILE_RETRIEVAL_RETRIALS = 1;						//how many times

	public static final int MAX_BLOCK_SIZE = 1024* 1024; //2048*2048;   //3 * (1024*1024);					// Max size of each data block || must not be set larger than (Integer.MAX - Integer.BYTES - 1)
	public static final long IDLE_BTW_FAILURE = 10000;							// Idle time between successive upload/download a block

	public static final int FILE_SYNC_PERIOD = 1*60000;	// Each file has to be synchronized at least every 1 min
	public static final int FILE_DEL_PERIOD = 60*60000; //  The file delete record is kept for an hour

	public static final String ANDROID_DIR_ROOT = "MDFS";
	public static final String ANDROID_DIR_CACHE = ANDROID_DIR_ROOT + File.separator + "cache";
	public static String ANDROID_DIR_DECRYPTED = "";

	public static final String NAME_MDFS_DIRECTORY = "mdfs_directory";

	public static final double K_N_RATIO = 0.5;
	public static final int MAX_N_VAL = 8; 										// max value of n

    public static final boolean isGlobal = true;
    public static boolean testRsock = false;
}
