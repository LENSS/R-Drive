package edu.tamu.lenss.mdfs.Commands.help;

import org.apache.log4j.Level;

import edu.tamu.lenss.mdfs.Constants;
import edu.tamu.lenss.mdfs.Handler.StartAll;

public class help {

    //logger
    public static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(help.class);

    public static String help(String clientID) {

        //log
        logger.log(Level.ALL, "Starting to handle -help command.");

        String reply_1 =
                "mdfs -help                                  : Shows all MDFS commands.<newline>" +
                "mdfs -ls <mdfs_dir>                         : Lists all directory/sub-directories in MDFS.<newline>" +
                "mdfs -mkdir <mdfs_dir>                      : Creates a directory in MDFS.<newline>" +
                "mdfs -rm <mdfs_file>                        : Removes a file from MDFS.<newline>" +
                "mdfs -rm <mdfs_dir>                         : Removes a directory with all files from MDFS.<newline>" +
                //"mdfs -setfacl <mdfs_file> <permission_list> : Sets permission of a MDFS file if user is the owner of file.<newline>" +
                //"mdfs -getfacl <mdfs_file>                   : Shows permission of a MDFS file.<newline>" +
                "mdfs -put <local_filepath> <mdfs_filepath>  : Creates a file in MDFS.<newline>" +
                //"mdfs -put <local_filepath> <mdfs_filepath> -setfacl <permission_list>     : Creates a file in MDFS with specified permission.<newline>" +
                "mdfs -get <mdfs_filepath> <local_filepath>  : Retrieves a file from MDFS.<newline>"
                //"<mdfs_filepath>                                                           : Relative Path of a file in MDFS.<newline>" +
                //"<local_filepath>                                                          : Absolute path of a file in local Linux file-system.<newline>" +
                //"<mdfs_dir>                                                                : A directory structure in MDFS.<newline>" +
                //"<mdfs_file>                                                               : A file that exists in MDFS.<newline>" +
                //"<permission_list>                                                         : WORLD | OWNER | GUID(s) | GROUP.<newline>"
                ;


        String reply_2 =
                        "mdfs -help \n" +
                        "mdfs -ls <mdfs_dir> \n" +
                        "mdfs -mkdir <mdfs_dir> \n" +
                        "mdfs -rm <mdfs_file> \n" +
                        "mdfs -rm <mdfs_dir> \n" +
                        "mdfs -put <local_filepath> <mdfs_filepath> \n" +
                        "mdfs -get <mdfs_filepath> <local_filepath> \n";



        //log
        logger.log(Level.ALL, "reply sent for -help command.");

        //return
        if(clientID.equals(Constants.NON_CLI_CLIENT)){
            return reply_2;
        }else {
            return reply_1;
        }
    }
}
