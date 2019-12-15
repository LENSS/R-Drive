package edu.tamu.lenss.MDFS;

import android.os.Environment;

import java.io.File;

public class Constants {


	//file/block size and count parameters
	public static final long MAX_FILE_SIZE = 133994489;
	public static final int MAX_BLOCK_SIZE = 1 * (1024*1024);
	public static final int MAX_BLOCK_COUNT = 127;
	public static final long DEFAULT_BLOCK_SIZE = 1* (1024* 1024);


	//directory parameters
	public static final String MDFS_LOG_PATH = Environment.getExternalStorageDirectory() + "/distressnet/MDFS/mdfs_log.log";
	public static final String ANDROID_DIR_ROOT = "MDFS";
	public static final String ANDROID_DIR_CACHE = ANDROID_DIR_ROOT + File.separator + "cache";
	public static final String DEFAULT_DECRYPTION_FOLDER_NAME = "decrypted";
    public static String ANDROID_DIR_DECRYPTED = "";

    //encoding parameters
	public static final double K_N_RATIO = 0.5;
	public static final int MAX_N_VAL = 8;

	//boolean parameters
    public static final boolean metadataIsGlobal = true;

    public final static String NOTIFICATION = "NOTIFICATION";
	public static final String NON_CLI_CLIENT = "NONCLICLIENT";


}
