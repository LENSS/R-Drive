package edu.tamu.lenss.mdfs.GNS;

import edu.tamu.cse.lenss.gnsService.client.GnsServiceClient;

import static java.lang.Thread.sleep;


//this class holds one and only GNS object for this app process.
//GNS is needed for app registration, service discovery and ip-to-guid conversion.
//learn more about GNS if youre interested.
public class GNS {

    public static GnsServiceClient gnsServiceClient;
    public static String ownGUID;

    public static GnsServiceClient getGNSInstance() { //todo: make sure this funtion starts after GNS service is running
        if (gnsServiceClient == null) {
            gnsServiceClient = new GnsServiceClient();
            try { sleep(1000); } catch (InterruptedException e) { e.printStackTrace(); }
            ownGUID = gnsServiceClient.getOwnGuid();
            System.out.println("own GUID: " + ownGUID);
            if(gnsServiceClient==null || ownGUID==null){
                System.out.println("GNS Error! could not init gns");
                throw new NullPointerException("GNS initialization error...Maybe GNS is not running or not connected.");
            }
        }
        return gnsServiceClient;
    }

    public static boolean stop(){
        return gnsServiceClient.removeService(GNSConstants.GNS_s);
    }
}
