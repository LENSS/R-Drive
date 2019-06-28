package edu.tamu.lenss.mdfs.RSock;

import example.Interface;

public class RSockConstants {

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

}
