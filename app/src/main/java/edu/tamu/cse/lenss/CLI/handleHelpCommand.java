package edu.tamu.cse.lenss.CLI;

public class handleHelpCommand {

    public static void handleHelpCommand(String uuid) {
        String reply =
                "mdfs -help                                                                : Shows all MDFS commands.<newline>" +
                "mdfs -ls                                                                  : Lists all directory/sub-directories in MDFS.<newline>" +
                "mdfs -list <mdfs_dir>                                                     : Lists all files in specified MDFS directory.<newline>" +
                "mdfs -mkdir <mdfs_dir>                                                    : Creates a directory in MDFS with WORLD permission.<newline>" +
                "mdfs -rm <mdfs_file>                                                      : Removes a file from MDFS if user has permission.<newline>" +
                "mdfs -rm <mdfs_dir>                                                       : Removes a directory with all files from MDFS if user has permission.<newline>" +
                "mdfs -setfacl <mdfs_file> <permission_list>                               : Sets permission of a MDFS file if user is the owner of file.<newline>" +
                "mdfs -getfacl <mdfs_file>                                                 : Shows permission of a MDFS file.<newline>" +
                "mdfs -put <local_filepath> <mdfs_filepath>                                : Creates a file in MDFS with WORLD permission.<newline>" +
                //"mdfs -put <local_filepath> <mdfs_filepath> -setfacl <permission_list>     : Creates a file in MDFS with specified permission.<newline>" +
                "mdfs -get <mdfs_filepath> <local_filepath>                                : Retrieves a file from MDFS if user has permission.<newline>" +
                "<mdfs_filepath>                                                           : Relative Path of a file in MDFS.<newline>" +
                "<local_filepath>                                                          : Absolute path of a file in local Linux file-system.<newline>" +
                "<mdfs_dir>                                                                : A directory structure in MDFS.<newline>" +
                "<mdfs_file>                                                               : A file that exists in MDFS.<newline>" +
                "<permission_list>                                                         : WORLD | OWNER | GUID(s) | GROUP.<newline>";

        clientSockets.sendAndClose(uuid, reply);
        return;
    }
}
