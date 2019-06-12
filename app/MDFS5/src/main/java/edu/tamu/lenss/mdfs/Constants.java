package edu.tamu.lenss.mdfs;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Handler;

import edu.tamu.lenss.mdfs.GNS.GNS;
import edu.tamu.lenss.mdfs.handler.ServiceHelper;
import edu.tamu.lenss.mdfs.models.NodeInfo;
import edu.tamu.lenss.mdfs.utils.IOUtilities;
import example.Interface;

public class Constants {
	//public static final String IP_PREFIX = "192.168.2.";
	//public static final String IP_PREFIX = "192.168.0.";
	public static final long TOPOLOGY_DISCOVERY_TIMEOUT = 3000;	// Critical parameters. Depend on link quality. 6000 when outside
	public static final long TOPOLOGY_DISCOVERY_RETRIAL_TIMEOUT = 5*60*1000;	// 5 mins The time waiting for re-try		
	public static final long FRAGMENT_CREATION_TIMEOUT_INTERVAL  = 300000;		// Timeout of sending file fragments
	public static final long FRAGMENT_RETRIEVAL_TIMEOUT_INTERVAL = 10000;		// Timeout of retrieving file fragments
	public static final long TOPOLOGY_REBROADCAST_THRESHOLD = 20*1000000000L;	// Time between each successful topology discovery request. (nano)
	public static final long FILE_REQUEST_TIMEOUT = 6000;						// Critical parameters. Depend on link quality. 6000 when outside
	public static final int FILE_RETRIEVAL_RETRIALS = 1;						//
	public static final int FILE_CREATION_RETRIALS = 2;
	public static final int BLOCK_RETRIEVAL_RETRIALS = 5;	// used only in Job retrieval now.

	public static final long DIRECTORY_LIST_REFRESH_PERIOD = 2500;
	//	public static final long MAX_BLOCK_SIZE = 128*1024;			// Max size of each data block
	public static final long MAX_BLOCK_SIZE = 3 * (1024*1024);			// Max size of each data block
	public static final long IDLE_BTW_FAILURE = 10000;					// Idle time between successive upload/download a block
	public static final long JOB_REQUEST_TIMEOUT = 6000;				// Timeout of waiting for JobReply

	public static final double KEY_STORAGE_RATIO=1;
	public static final double FILE_STORAGE_RATIO=1;
	public static final double KEY_CODING_RATIO = 1;
	public static final double FILE_CODING_RATIO = 1;

	public static final String DIR_ROOT = "MDFS";
	public static final String DIR_CACHE = DIR_ROOT + File.separator + "cache";
	public static final String DIR_DECRYPTED = DIR_ROOT + File.separator + "decrypted";
	public static final String DIR_ENCRYPTED = DIR_ROOT + File.separator + "encrypted";
	//public static final String DIR_JOB_CACHE = DIR_ROOT + File.separator +"job_cache";
	public static final String DIR_JOBS = DIR_ROOT + File.separator + "jobs";

	public static final String NAME_MDFS_DIRECTORY = "mdfs_directory";

	public static final int TCP_COMM_BUFFER_SIZE = 512;


	public static final long MAX_VIDEO_SIZE = 40 * (1024*1024);	// 40 MB
	public static final int MAX_VIDEO_DURATION = 10 * 60;  		// 30 minutes

	public static final double K_N_RATIO = 0.5;
	public static final int MAX_N_VAL = 8; // max value of n
	public static final double STORAGE_NODES_RATIO =  1;	// ratio of nodes out of the entire network that will be used as storage nodes | this should always be 1
	public static final int MAX_FACE_PER_FRAME = 6;		// number if sample frame per second
	public static final double SAMPLE_PER_SECOND = 2;
	public static final int COMPRESS_RATE = 30;			// Quality of the extracted frame to be compresses 
	public static final int COMMON_DEVICE_WIDTH = 600;
	public static final int COMMON_DEVICE_HEIGHT = 1024;
	public static final long DOWNLOADING_BLOCK_TTL = 90*1000; // 1.5 MIN



//========================================RSOCK===============================================


	//rsock api instances, initialized in PacketExchanger.java class
	public static Interface intrfc_creation;
	public static Interface intrfc_retrieval;

	//appID using which above Rsock api objects are registered to the daemon.
	public static String intrfc_creation_appid = "mdfsFileCreation";
	public static String intrfc_retrieval_appid = "mdfsFileRetrieval";

	//RSOCK variables | (value: "rsock" or "tcp").
	//when "tcp", file creation happens using tcp,
	// instant topology discovery takes place to find candidate nodes
	//who will take the file fragments, and all data packet is IP based.
	//Using tcp, no file metadata is stored in EdgeKeeper.
	//when "rsock", file creation happens using rsock,
	//no topology discovery takes place, instead topology fetching
	//takes place, and all data packet is GUID based.
	//using rsock, file metadata is stored i EdgeKeeper.
	//not a param to toggle between during runtime.
	public static final String file_creation_via_rsock_or_tcp = "rsock";

	//RSOCK variables | (value: "rsock" or "tcp").
	//when "tcp", file retrieval happens using tcp,
	//instant topology discovery takes place to find the
	//nodes who has what fragments, and all communication is IP
	//based.
	//using tcp, it doesnt fetch file metadata from EdgeKeeper.
	//when "rsock", file retrieval happens using rsock,
	//no topology discovery takes place, rather fragment holder
	//information is fetched from EDGEKEEPER, and all communication
	//is GUID based.
	//using rsock, file metadata is first fetched from EdgeKeeper
	//before fetching fragments.
	//not a param to toggle between during runtime.
	public static final String file_retrieval_via_rsock_or_tcp = "rsock";

	public static String my_wifi_ip_temp = "";
	public static final String dummy_EdgeKeeper_ip = "192.168.0.2";
	public static final int dummy_EdgeKeeper_port = 9999;
	//check if I am the dummy EdgeKeeper or nah.
	//if yes, I run a server in a thread and store all metadata in me.
	//if nah, I am a client and store/fetch metadata to the server.



//========================================RSOCK===============================================


}
