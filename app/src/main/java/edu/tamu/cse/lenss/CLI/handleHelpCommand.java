package edu.tamu.cse.lenss.CLI;

public class handleHelpCommand {

    public static void handleHelpCommand(String uuid) {
        String reply =
                "mdfs help                                           : Show all MDFS commands.<newline>" +
                "mdfs directory all                                  : Show list of files available in MDFS system.<newline>" +
                "mdfs direcotry decrypted                            : Show list of locally available decrypted files.<newline>" +
                "mdfs directory encrypted                            : Show list of encrypted files.<newline>" +
                "mdfs set_perm <filename> <permission_list>          : Change permission of a file.<newline>" +
                "mdfs get_perm <filename>                            : Show permissions of file available in MDFS system.<newline>" +
                "mdfs get_perm all                                   : Show permission of all files in MDFS system.<newline>" +
                "mdfs create <filename> permission <permission_list> : Create a file in MDFS system.<newline>" +
                "mdfs retrieve <filename>                            : Retrieve a file from MDFS filesystem.<newline>" +
                "mdfs retrieve all                                   : Retrieve all files from MDFS filesystem.<newline>" +
                "mdfs detele <filename>                              : Delete a file from MDFS System owned by user.<newline>" +
                "mdfs delete all                                     : Delete all files from MDFS System owned by User.<newline>" +
                "mdfs add_group <group_name> | <group_name> | ...    : Add a group name this node belongs to.<newline>" +
                "mdfs remove_group <group_name> | <group_name> | ... : Remove this node from a group this node belongs to.<newline>" +
                "mdfs group_list                                     : Get the list of groups this node belongs to.<newline>" +
                "<permission_list>                                   : OWNER | WORLD| GUID | GROUP:<group_name>.<newline>" +
                "<group_name>                                        : Name of a group as any string without space.<endgame>";

        clientSockets.sendAndClose(uuid, reply);
        return;
    }
}
