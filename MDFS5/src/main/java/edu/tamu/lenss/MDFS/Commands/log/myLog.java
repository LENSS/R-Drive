package edu.tamu.lenss.MDFS.Commands.log;

import org.apache.log4j.Logger;

public class myLog {

    //logger
    public static Logger logger = Logger.getLogger(myLog.class);

    public static long put_take_file_from_disk = 0;
    public static long put_erasure_coding = 0;
    public static long put_encryption = 0;
    public static long put_sending_over_rsock = 0;
    public static long put_metadata_update = 0;
    public static long put_writing_fragments_in_file = 0;


    public static long get_take_fragments_from_disk = 0;
    public static long get_erasure_coding = 0;
    public static long get_encryption = 0;
    public static long get_receiving_fragments_over_rsock = 0;
    public static long get_metadata_fetch = 0;
    public static long get_writing_file_in_disk = 0;


}
